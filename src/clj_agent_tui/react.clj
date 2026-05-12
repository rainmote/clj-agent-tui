(ns clj-agent-tui.react
  "Minimal ReAct loop demo runtime for clj-agent-tui states.

  The loop is intentionally dependency-light: callers provide an `:llm-fn` that
  returns either final text or a tool request. This keeps clj-agent-tui usable without
  network credentials while leaving a clear seam for Claude/Anthropic adapters."
  (:require
   [clj-agent-tui.agent :as agent]))

(defn claude-config
  "Return Claude-like configuration from env/system props without making calls."
  ([] (claude-config (System/getenv)))
  ([env]
   {:provider :claude
    :api-key (or (get env "ANTHROPIC_API_KEY") (System/getProperty "anthropic.api.key"))
    :base-url (or (get env "ANTHROPIC_BASE_URL") "https://api.anthropic.com")
    :model (or (get env "ANTHROPIC_MODEL") "claude-sonnet-4-5")
    :max-tokens (parse-long (or (get env "ANTHROPIC_MAX_TOKENS") "1024"))}))

(defn default-tools
  []
  {"echo" (fn [{:keys [input]}] (str input))
   "count" (fn [{:keys [input]}] (str (count (str input))))})

(defn scripted-llm
  "Return a deterministic LLM function for tests and local demos.

  First turn asks to call echo; second turn returns a final answer using the
  observation."
  ([] (scripted-llm {}))
  ([_opts]
   (fn [{:keys [question observations]}]
     (if (seq observations)
       {:type :final
        :thought "I have an observation, so I can answer."
        :answer (str "Final: " (last observations))}
       {:type :tool
        :thought (str "Need a tool to inspect: " question)
        :tool "echo"
        :input question}))))

(defn- append-assistant!
  [state text thought]
  (let [[state _] (agent/apply-event state {:type :assistant/start :thought thought})
        [state _] (agent/apply-event state {:type :assistant/delta :text text :tokens (count text)})
        [state _] (agent/apply-event state {:type :assistant/done})]
    state))

(defn step
  "Execute one ReAct step. Returns `[state ctx]`.

  `ctx` keys: `:question`, `:observations`, `:llm-fn`, `:tools`, `:done?`."
  [state {:keys [llm-fn tools question observations] :as ctx}]
  (let [llm-fn (or llm-fn (scripted-llm))
        tools (or tools (default-tools))
        result (llm-fn (assoc ctx :observations observations))]
    (case (:type result)
      :tool
      (let [tool-name (:tool result)
            tool-id (str "react-" (inc (count observations)))
            [state _] (agent/apply-event state {:type :tool/request
                                                :id tool-id
                                                :name tool-name
                                                :summary (str "ReAct action: " tool-name)
                                                :status :running})
            tool-fn (get tools tool-name)
            observation (if tool-fn
                          (tool-fn {:input (:input result) :question question})
                          (str "Unknown tool: " tool-name))
            [state _] (agent/apply-event state {:type :tool/update
                                                :id tool-id
                                                :status (if tool-fn :done :error)
                                                :summary (str "Observation: " observation)})
            state (append-assistant! state
                                     (str "Thought: " (:thought result) "\n"
                                          "Action: " tool-name "\n"
                                          "Observation: " observation)
                                     (:thought result))]
        [state (-> ctx
                   (assoc :tools tools :llm-fn llm-fn)
                   (update :observations (fnil conj []) observation))])

      :final
      [(append-assistant! state (:answer result) (:thought result))
       (assoc ctx :done? true :answer (:answer result))]

      [(append-assistant! state (str result) "Unexpected ReAct result")
       (assoc ctx :done? true :answer (str result))])))

(defn run-loop
  "Run a bounded ReAct loop over clj-agent-tui state. Returns `[state ctx]`."
  ([state question] (run-loop state question {}))
  ([state question {:keys [max-steps] :as opts}]
   (let [[state effects] (agent/submit-user-input state question)
         ctx (merge {:question question
                     :observations []
                     :max-steps (or max-steps 4)}
                    opts
                    {:initial-effects effects})]
     (loop [state state
            ctx ctx
            n 0]
       (if (or (:done? ctx) (>= n (:max-steps ctx)))
         [state (cond-> ctx
                  (and (not (:done? ctx)) (>= n (:max-steps ctx)))
                  (assoc :stopped? true))]
         (let [[state ctx] (step state ctx)]
           (recur state ctx (inc n))))))))
