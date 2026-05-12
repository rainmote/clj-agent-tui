(ns clj-agent-tui.react-demo
  "Runnable deterministic ReAct demo for clj-agent-tui."
  (:require
   [clojure.string :as str]
   [clj-agent-tui.app :as app]
   [clj-agent-tui.mock :as mock]
   [clj-agent-tui.react :as react]))

(defn demo-state
  ([] (demo-state "请用 ReAct echo 工具观察这句话"))
  ([question]
   (first (react/run-loop (mock/state {:messages []
                                       :stream {:state :idle :elapsed "00:00" :tokens 0 :queue 0 :tools 0}})
                          question))))

(defn -main [& args]
  (let [question (if (seq args) (str/join " " args) "请用 ReAct echo 工具观察这句话")]
    (println (app/view (demo-state question)))))
