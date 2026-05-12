(ns clj-agent-tui.app
  "Composable agent-code-style Agent TUI shell."
  (:require
   [charm.components.text-input :as text-input]
   [charm.message :as msg]
   [charm.program :as program]
   [clj-agent-tui.agent :as agent]
   [clj-agent-tui.components :as c]
   [clj-agent-tui.focus :as focus]
   [clj-agent-tui.layout :as layout]
   [clj-agent-tui.selection :as selection]
   [clojure.string :as str]))

(def default-width 100)
(def default-height 32)

(defn make-input
  ([] (make-input ""))
  ([value]
   (text-input/text-input :prompt ""
                          :value value
                          :placeholder "Type your message or @path/to/file"
                          :focused true)))

(defn- reset-composer
  [state]
  (-> state
      (assoc :input (make-input))
      (assoc :composer {:mode :chat})
      (focus/focus :composer)))

(defn submit-draft [state]
  (let [draft (str/trim (text-input/value (:input state)))
        [state effects] (agent/submit-user-input state draft)]
    (-> state
        reset-composer
        (assoc :last-effects effects))))

(defn- suggestion-trigger
  [suggestions]
  (or (:trigger suggestions) "/"))

(defn- suggestion-draft
  [suggestions]
  (str (suggestion-trigger suggestions) (:query suggestions "")))

(defn- selected-insert-text
  [option]
  (let [value (or (:insert option) (:label option) (:value option) "")]
    (if (string? value) value (str value))))

(defn- sync-suggestion-input
  [state suggestions]
  (-> state
      (assoc :suggestions suggestions
             :input (make-input (suggestion-draft suggestions))
             :composer {:mode (case (suggestion-trigger suggestions)
                                "/" :slash
                                "@" :at-path
                                :chat)})
      (focus/push-focus :suggestions)))

(defn- delete-empty-suggestion-trigger?
  [state m]
  (and (:suggestions state)
       (str/blank? (get-in state [:suggestions :query] ""))
       (or (msg/key-match? m "backspace") (msg/key-match? m :backspace))))

(defn- tool-approval-dialog?
  [state]
  (= :tool-approval (get-in state [:dialog :kind])))

(defn- apply-tool-dialog-decision
  [state decision]
  (let [[state effects] (agent/apply-event state {:type :tool/decision
                                                  :tool-id (get-in state [:dialog :tool-id])
                                                  :decision decision})]
    (-> state
        (focus/pop-focus)
        (assoc :last-dialog-action decision
               :last-effects effects))))

(defn update-dialog
  [state m]
  (cond
    (or (msg/key-match? m "esc") (msg/key-match? m :escape))
    [(if (tool-approval-dialog? state)
       (apply-tool-dialog-decision state :reject)
       (-> state (assoc :dialog nil) (focus/pop-focus))) :dialog/closed]

    (or (msg/key-match? m "enter") (msg/key-match? m "y"))
    [(if (tool-approval-dialog? state)
       (apply-tool-dialog-decision state :accept)
       (-> state
           (assoc :dialog nil)
           (focus/pop-focus)
           (assoc :last-dialog-action :accept))) :dialog/accepted]

    (msg/key-match? m "n")
    [(if (tool-approval-dialog? state)
       (apply-tool-dialog-decision state :reject)
       (-> state
           (assoc :dialog nil)
           (focus/pop-focus)
           (assoc :last-dialog-action :reject))) :dialog/rejected]

    :else [state :dialog/focused]))

(defn update-fn
  "Default reducer for clj-agent-tui mock/demo shells. Dialog focus wins first."
  [state m]
  (cond
    (:dialog state)
    (let [[new-state _action] (update-dialog state m)]
      [new-state nil])

    (delete-empty-suggestion-trigger? state m)
    [(-> state reset-composer (assoc :suggestions nil)) nil]

    (:suggestions state)
    (let [[suggestions action] (selection/update-selection (:suggestions state) m)
          state (sync-suggestion-input state suggestions)]
      (case action
        :selected [(-> state
                    reset-composer
                    (assoc :last-selection (:selected suggestions)
                           :suggestions nil
                           :input (make-input (selected-insert-text (:selected suggestions))))) nil]
        :closed [(-> state reset-composer (assoc :suggestions nil)) nil]
        [state nil]))

    (or (msg/key-match? m "ctrl+c") (msg/key-match? m "q"))
    [state program/quit-cmd]

    (msg/window-size? m)
    [(assoc state :layout {:width (:width m) :height (:height m)}) nil]

    (msg/key-match? m "enter")
    [(submit-draft state) nil]

    (msg/key-match? m "?")
    [(-> state
         (assoc :dialog {:title "Keyboard Shortcuts"
                         :body "enter send · esc cancel · / suggestions · ! shell · @ attach · q quit"
                         :actions "enter close · esc close"})
         (focus/push-focus :dialog)) nil]

    (msg/key-match? m "/")
    [(sync-suggestion-input state (assoc (selection/set-query (:command-palette state) "") :trigger "/")) nil]

    (msg/key-match? m "@")
    [(sync-suggestion-input state (assoc (selection/set-query (or (:path-palette state) (:command-palette state)) "") :trigger "@")) nil]

    (msg/key-match? m "!")
    [(-> state
         (assoc :composer {:mode :shell}
                :input (make-input "!"))
         (focus/focus :composer)) nil]

    :else
    (let [[new-input cmd] (text-input/text-input-update (:input state) m)]
      [(assoc state :input new-input) cmd])))

(defn body
  [state width height]
  (let [rail-width (if (>= width 96) 34 0)
        gutter (if (pos? rail-width) 2 0)
        main-width (- width rail-width gutter)
        main (cond
               (:dialog state) (layout/block (c/modal (:dialog state)) main-width height)
               :else (c/history {:messages (:messages state) :width main-width :height height}))
        main (layout/block main main-width height :from :top)]
    (if (pos? rail-width)
      (layout/columns [{:content main :width main-width :from :top}
                       {:content (apply str (repeat gutter " ")) :width gutter}
                       {:content (c/rail (assoc (:rail state) :width rail-width)) :width rail-width}]
                      height)
      main)))

(defn controls
  [state width]
  (when-not (:dialog state)
    (let [draft (if-let [suggestions (:suggestions state)]
                  (suggestion-draft suggestions)
                  (text-input/value (:input state)))]
      (layout/stack
       (c/composer {:width width
                    :draft draft
                    :placeholder "Type your message or @path/to/file"
                    :queue (get-in state [:stream :queue])
                    :elapsed (get-in state [:stream :elapsed])
                    :tokens (get-in state [:stream :tokens])
                    :tools (get-in state [:stream :tools])
                    :streaming? (= :streaming (get-in state [:stream :state]))
                    :mode (get-in state [:composer :mode] :chat)})
       (when-let [suggestions (:suggestions state)]
         (c/suggestions suggestions {:width width}))
       (c/footer {:width width})))))

(defn view
  "Render a full agent-code-style shell from data state."
  [state]
  (let [{:keys [width height]} (:layout state)
        width (max 60 (or width default-width))
        height (max 1 (or height default-height))
        header (layout/stack
                (c/header (merge {:width width} (:session state)))
                (c/status-line (merge {:width width} (:stream state)))
                (layout/rule width))
        controls-text (controls state width)
        chrome-height (+ (count (layout/split-lines header))
                         (if controls-text (count (layout/split-lines controls-text)) 0))
        body-height (max 0 (- height chrome-height))]
    (layout/block
     (layout/stack header (body state width body-height) controls-text)
     width height :from :top)))

(defn run
  [initial-state]
  (program/run {:init (fn [] [initial-state nil])
                :update update-fn
                :view view
                :alt-screen true
                :mouse :cell
                :focus-reporting true}))
