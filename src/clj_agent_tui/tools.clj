(ns clj-agent-tui.tools
  "Small data-first tool registry and scheduler for clj-agent-tui runtimes."
  (:require
   [clojure.java.io :as io]))

(defn default-registry
  []
  {"echo" {:name "echo"
           :description "Return input"
           :run (fn [{:keys [input]}] (str input))}
   "count" {:name "count"
            :description "Count input chars"
            :run (fn [{:keys [input]}] (str (count (str input))))}
   "shell" {:name "shell"
            :description "Mock shell runner"
            :run (fn [{:keys [command]}] (str "mock-shell$ " command))}
   "read" {:name "read"
           :description "Read a local file"
           :run (fn [{:keys [path]}]
                  (try
                    (slurp (io/file path))
                    (catch Exception e
                      (str "read failed: " (.getMessage e)))))}})

(defn run-tool
  ([registry name args]
   (if-let [tool (get registry name)]
     {:status :done :output ((:run tool) args)}
     {:status :error :output (str "Unknown tool: " name)})))

(defn effect->events
  "Turn a runtime effect into agent events. This is a local demo scheduler, not a
  production sandbox."
  ([effect] (effect->events (default-registry) effect))
  ([registry {:keys [type] :as effect}]
   (case type
     :tool/execute
     (let [tool (:tool effect)
           name (:name tool)
           result (run-tool registry name (:args tool))]
       [{:type :tool/update
         :id (:tool-id effect)
         :status (:status result)
         :summary (:output result)}])

     :shell/run
     (let [result (run-tool registry "shell" {:command (:command effect)})]
       [{:type :shell/result :status (:status result) :output (:output result)}])

     :context/attach
     (let [result (run-tool registry "read" {:path (:path effect)})]
       [{:type :tool/request :id (str "attach-" (Math/abs (hash (:path effect))))
         :name "read" :summary (str "Attach " (:path effect)) :status :done}
        {:type :tool/update :id (str "attach-" (Math/abs (hash (:path effect))))
         :status (:status result)
         :summary (str "Attached " (:path effect) " (" (count (:output result)) " chars)")}])

     :react/run
     []

     [])))

(defn tool-options
  ([] (tool-options (default-registry)))
  ([registry]
   (->> registry vals (map #(select-keys % [:name :description])) (sort-by :name) vec)))
