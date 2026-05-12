(ns clj-agent-tui.agent
  "Pure agent event/effect boundary for clj-agent-tui shells."
  (:require
   [clojure.string :as str]
   [clj-agent-tui.commands :as commands]))

(defn- next-id
  [state prefix]
  (let [n (long (or (:next-id state) 1))]
    [(assoc state :next-id (inc n)) (str prefix "-" n)]))

(defn- add-message
  [state message]
  (let [[state id] (if (:id message)
                     [state (:id message)]
                     (next-id state "msg"))]
    [(update state :messages (fnil conj []) (assoc message :id id)) id]))

(defn- message-index
  [messages id]
  (first (keep-indexed (fn [idx message]
                         (when (= id (:id message)) idx))
                       messages)))

(defn- update-message
  [state id f & args]
  (if-let [idx (message-index (:messages state) id)]
    (apply update-in state [:messages idx] f args)
    state))

(defn- active-assistant-id
  [state event]
  (or (:id event) (:active-response-id state)))

(defn command-input?
  [text]
  (boolean (commands/parse-command text)))

(defn shell-input?
  [text]
  (str/starts-with? (str/trim (or text "")) "!"))

(defn shell-command
  [text]
  (str/trim (subs (str/trim (or text "")) 1)))

(defn at-paths
  "Extract simple @path references from text. Escaped/spaced paths are out of scope
  for this first runtime seam; the returned values exclude the leading @."
  [text]
  (->> (re-seq #"(?:^|\s)@([^\s]+)" (or text ""))
       (map second)
       vec))

(defn submit-user-input
  "Submit a user draft.

  Slash commands are handled locally. Normal user messages are appended to
  history and return an `:agent/send-message` effect for an external runtime."
  ([state text] (submit-user-input state text {}))
  ([state text {:keys [commands] :or {commands commands/default-commands}}]
   (let [text (str/trim (or text ""))]
     (cond
       (str/blank? text)
       [state []]

       (commands/parse-command text)
       (commands/execute-command state commands (commands/parse-command text))

       (shell-input? text)
       (let [cmd (shell-command text)
             [state _id] (add-message state {:role :shell :text cmd :status :pending})]
         [(assoc-in state [:stream :state] :shell)
          [{:type :shell/run :command cmd}]])

       :else
       (let [paths (at-paths text)
             [state _id] (add-message state (cond-> {:role :user :text text}
                                              (seq paths) (assoc :attachments paths)))]
         [(-> state
              (assoc-in [:stream :state] :thinking)
              (update-in [:stream :queue] (fnil inc 0)))
          (cond-> [(cond-> {:type :agent/send-message :text text}
                     (seq paths) (assoc :attachments paths))]
            (seq paths) (into (map (fn [path] {:type :context/attach :path path}) paths)))])))))

(defn apply-event
  "Apply an agent/runtime event to UI state.

  Returns `[state effects]`; effects are data descriptions for the outer
  runtime, never executed here."
  [state {:keys [type] :as event}]
  (case type
    :user/submit
    (submit-user-input state (:text event) event)

    :command/run
    (commands/execute-command state (:commands event commands/default-commands) event)

    :assistant/start
    (let [[state id] (add-message state {:role :assistant
                                         :text (or (:text event) "")
                                         :thought (:thought event)
                                         :status :streaming})]
      [(-> state
           (assoc :active-response-id id)
           (assoc-in [:stream :state] :streaming))
       []])

    :assistant/delta
    (let [id (active-assistant-id state event)
          delta (or (:text event) "")]
      [(-> state
           (update-message id update :text str delta)
           (update-in [:stream :tokens] (fnil + 0) (long (or (:tokens event) 0)))
           (assoc-in [:stream :state] :streaming))
       []])

    :assistant/done
    (let [id (active-assistant-id state event)]
      [(-> state
           (update-message id assoc :status :done)
           (dissoc :active-response-id)
           (assoc-in [:stream :state] :idle)
           (update-in [:stream :queue] (fnil #(max 0 (dec %)) 0)))
       []])

    :tool/request
    (let [[state id] (next-id state "tool")
          tool (-> event
                   (dissoc :type)
                   (assoc :id (or (:id event) id)
                          :status (or (:status event) :pending)))
          tool-id (:id tool)
          state (-> state
                    (assoc-in [:tools tool-id] tool)
                    (update-in [:stream :tools] (fnil inc 0))
                    (update :messages (fnil conj []) {:role :tool
                                                      :id (str "tool-message-" tool-id)
                                                      :tool (:name tool)
                                                      :status (:status tool)
                                                      :text (or (:summary tool) (:description tool) "Tool requested")}))]
      (if (:requires-approval? event)
        [(assoc state :dialog {:kind :tool-approval
                               :tool-id tool-id
                               :title "Approve tool call"
                               :body (or (:description event) (:summary event) (:name event))
                               :actions "enter accept · n reject · esc cancel"})
         [{:type :tool/approval-needed :tool-id tool-id}]]
        [state [{:type :tool/execute :tool-id tool-id :tool tool}]]))

    :tool/update
    (let [tool-id (:id event)
          status (:status event)]
      [(-> state
           (update-in [:tools tool-id] merge (dissoc event :type))
           (update-message (str "tool-message-" tool-id)
                           merge
                           (cond-> {}
                             status (assoc :status status)
                             (:text event) (assoc :text (:text event))
                             (:summary event) (assoc :text (:summary event)))))
       []])

    :tool/decision
    (let [tool-id (:tool-id event)
          accepted? (= :accept (:decision event))]
      [(-> state
           (assoc :dialog nil)
           (assoc-in [:tools tool-id :status] (if accepted? :approved :rejected)))
       [(if accepted?
          {:type :tool/execute :tool-id tool-id :tool (get-in state [:tools tool-id])}
          {:type :tool/rejected :tool-id tool-id})]])

    :shell/result
    [(-> state
         (assoc-in [:stream :state] :idle)
         (update :messages (fnil conj []) {:role :tool
                                           :tool "Shell"
                                           :status (or (:status event) :done)
                                           :text (or (:output event) "")}))
     []]

    :settings/update
    [(update state :settings merge (:settings event)) []]

    :session/saved
    [(assoc-in state [:session :saved-at] (:at event)) []]

    :agent/error
    [(-> state
         (assoc-in [:stream :state] :error)
         (update :messages (fnil conj []) {:role :system
                                           :status :error
                                           :text (or (:message event) "Agent error")}))
     []]

    [state [{:type :agent/unknown-event :event event}]]))
