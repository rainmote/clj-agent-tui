(ns clj-agent-tui.runtime
  "Minimal local runtime that binds clj-agent-tui effects to provider/tool/session adapters."
  (:require
   [clj-agent-tui.agent :as agent]
   [clj-agent-tui.provider :as provider]
   [clj-agent-tui.session :as session]
   [clj-agent-tui.tools :as tools]))

(defn apply-events
  [state events]
  (reduce (fn [[state effects] event]
            (let [[state more] (agent/apply-event state event)]
              [state (into effects more)]))
          [state []]
          events))

(defn handle-effect
  [state {:keys [provider registry session-path]} {:keys [type] :as effect}]
  (case type
    :agent/send-message
    (apply-events state (provider/stream-events (or provider (provider/deterministic-provider)) effect))

    (:tool/execute :shell/run :context/attach)
    (apply-events state (tools/effect->events (or registry (tools/default-registry)) effect))

    :history/clear
    [state []]

    :model/select
    [state []]

    :react/run
    [state [effect]]

    :session/save
    (let [result (session/save-session! (or session-path ".omx/state/clj-agent-tui-session.edn") state)]
      (agent/apply-event state {:type :session/saved :at (:path result)}))

    [state []]))

(defn drain-effects
  "Handle effects until the local queue is empty. Returns `[state handled-effects]`."
  ([state effects] (drain-effects state effects {}))
  ([state effects runtime]
   (loop [state state
          pending (vec effects)
          handled []]
     (if-let [effect (first pending)]
       (let [[state more] (handle-effect state runtime effect)]
         (recur state (into (subvec pending 1) more) (conj handled effect)))
       [state handled]))))

(defn submit-and-run
  "Submit text, then handle all local effects with the supplied runtime adapters."
  ([state text] (submit-and-run state text {}))
  ([state text runtime]
   (let [[state effects] (agent/submit-user-input state text)
         [state handled] (drain-effects state effects runtime)]
     [state {:initial-effects effects :handled-effects handled}])))
