(ns clj-agent-tui.runtime-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [clj-agent-tui.mock :as mock]
   [clj-agent-tui.provider :as provider]
   [clj-agent-tui.runtime :as runtime]
   [clj-agent-tui.session :as session]
   [clj-agent-tui.tools :as tools]))

(deftest deterministic-provider-streams-assistant-events
  (let [events (provider/stream-events (provider/deterministic-provider)
                                       {:text "hello"})]
    (is (= [:assistant/start :assistant/delta :assistant/done]
           (mapv :type events)))
    (is (= "Echo: hello" (:text (second events))))))

(deftest local-runtime-submits-and-drains-provider-effects
  (let [[state report] (runtime/submit-and-run (mock/state {:messages []
                                                            :stream {:state :idle :elapsed "00:00" :tokens 0 :queue 0 :tools 0}})
                                               "hello")]
    (is (= [{:type :agent/send-message :text "hello"}] (:initial-effects report)))
    (is (some #(str/includes? (:text %) "Echo: hello") (:messages state)))
    (is (= :idle (get-in state [:stream :state])))))

(deftest local-runtime-drains-shell-effect
  (let [[state report] (runtime/submit-and-run (mock/state {:messages [] :stream {}}) "!pwd")]
    (is (= [{:type :shell/run :command "pwd"}] (:initial-effects report)))
    (is (some #(= "Shell" (:tool %)) (:messages state)))))

(deftest session-save-and-load-roundtrip
  (let [file (java.io.File/createTempFile "clj-agent-tui-session" ".edn")
        path (.getPath file)
        state (mock/state {:messages [{:role :user :text "persist"}]})]
    (.delete file)
    (session/save-session! path state)
    (is (= [{:role :user :text "persist"}] (:messages (session/load-session path))))
    (io/delete-file path true)))

(deftest tool-registry-runs-echo-and-unknown-tools
  (is (= {:status :done :output "abc"}
         (tools/run-tool (tools/default-registry) "echo" {:input "abc"})))
  (is (= :error (:status (tools/run-tool (tools/default-registry) "missing" {})))))
