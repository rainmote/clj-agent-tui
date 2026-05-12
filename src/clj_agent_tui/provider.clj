(ns clj-agent-tui.provider
  "Provider abstraction for clj-agent-tui runtimes.")

(defprotocol Provider
  (stream-events [provider request]
    "Return a bounded seq of clj-agent-tui.agent events for a request."))

(defn deterministic-provider
  "Create a local provider used for demos/tests. It echoes the submitted text."
  ([] (deterministic-provider {}))
  ([{:keys [name response-fn] :or {name :deterministic}}]
   (reify Provider
     (stream-events [_ {:keys [text]}]
       (let [answer (if response-fn
                      (response-fn text)
                      (str "Echo: " text))]
         [{:type :assistant/start :thought (str "provider:" (clojure.core/name name))}
          {:type :assistant/delta :text answer :tokens (count answer)}
          {:type :assistant/done}])))))

(defn claude-provider
  "Return a Claude-shaped provider wrapper.

  This is intentionally adapter-shaped but offline by default: pass `:response-fn`
  for tests or bind this protocol to a real Anthropic client later."
  [config]
  (deterministic-provider {:name :claude
                           :response-fn (or (:response-fn config)
                                            (fn [text]
                                              (str "Claude(" (:model config) ") would answer: " text)))}))
