(ns clj-agent-tui.react-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [clj-agent-tui.app :as app]
   [clj-agent-tui.mock :as mock]
   [clj-agent-tui.react :as react]
   [clj-agent-tui.react-demo :as demo]))

(def ansi-re #"\u001B\[[;?0-9]*[ -/]*[@-~]")

(defn plain [s]
  (str/replace s ansi-re ""))

(deftest claude-config-reads-claude-like-env
  (let [cfg (react/claude-config {"ANTHROPIC_API_KEY" "k"
                                  "ANTHROPIC_BASE_URL" "http://localhost"
                                  "ANTHROPIC_MODEL" "claude-test"
                                  "ANTHROPIC_MAX_TOKENS" "7"})]
    (is (= :claude (:provider cfg)))
    (is (= "k" (:api-key cfg)))
    (is (= "http://localhost" (:base-url cfg)))
    (is (= "claude-test" (:model cfg)))
    (is (= 7 (:max-tokens cfg)))))

(deftest react-loop-renders-tool-observation-and-final-answer
  (let [[state ctx] (react/run-loop (mock/state {:messages []
                                                 :stream {:state :idle :elapsed "00:00" :tokens 0 :queue 0 :tools 0}})
                                    "ping")
        rendered (plain (app/view state))]
    (is (:done? ctx))
    (is (= "Final: ping" (:answer ctx)))
    (is (str/includes? rendered "Action: echo"))
    (is (str/includes? rendered "Observation: ping"))
    (is (str/includes? rendered "Final: ping"))))

(deftest react-demo-state-is-renderable
  (let [rendered (plain (app/view (demo/demo-state "demo")))]
    (is (str/includes? rendered "Agent Code"))
    (is (str/includes? rendered "Final: demo"))))
