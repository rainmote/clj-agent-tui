(ns clj-agent-tui.agent-test
  (:require
   [clojure.test :refer [deftest is]]
   [clj-agent-tui.agent :as agent]))

(deftest submit-user-input-appends-message-and-returns-effect
  (let [[state effects] (agent/submit-user-input {:messages [] :stream {:queue 0}} " hello ")]
    (is (= [{:role :user :text "hello" :id "msg-1"}] (:messages state)))
    (is (= :thinking (get-in state [:stream :state])))
    (is (= 1 (get-in state [:stream :queue])))
    (is (= [{:type :agent/send-message :text "hello"}] effects))))

(deftest submit-user-input-runs-slash-command-locally
  (let [[state effects] (agent/submit-user-input {:messages [{:role :user :text "old"}]}
                                                 "/clear")]
    (is (= [] (:messages state)))
    (is (= [{:type :history/clear :name "clear"}] effects))))

(deftest assistant-stream-events-build-one-message
  (let [[state _] (agent/apply-event {:messages [] :stream {:queue 1 :tokens 0}}
                                     {:type :assistant/start :thought "thinking"})
        response-id (:active-response-id state)
        [state _] (agent/apply-event state {:type :assistant/delta :text "hel" :tokens 1})
        [state _] (agent/apply-event state {:type :assistant/delta :text "lo" :tokens 2})
        [state _] (agent/apply-event state {:type :assistant/done})]
    (is (= "msg-1" response-id))
    (is (= [{:role :assistant :text "hello" :thought "thinking" :status :done :id "msg-1"}]
           (:messages state)))
    (is (= 3 (get-in state [:stream :tokens])))
    (is (= :idle (get-in state [:stream :state])))
    (is (nil? (:active-response-id state)))))

(deftest tool-request-can-require-approval-and-decision
  (let [[state effects] (agent/apply-event {:messages [] :stream {:tools 0}}
                                           {:type :tool/request
                                            :name "Write"
                                            :summary "edit file"
                                            :requires-approval? true})
        tool-id (get-in state [:dialog :tool-id])]
    (is (= "tool-1" tool-id))
    (is (= :tool-approval (get-in state [:dialog :kind])))
    (is (= [{:type :tool/approval-needed :tool-id "tool-1"}] effects))
    (is (= :pending (get-in state [:tools tool-id :status])))
    (let [[state effects] (agent/apply-event state {:type :tool/decision
                                                    :tool-id tool-id
                                                    :decision :accept})]
      (is (nil? (:dialog state)))
      (is (= :approved (get-in state [:tools tool-id :status])))
      (is (= :tool/execute (:type (first effects)))))))

(deftest tool-update-refreshes-message-status
  (let [[state _] (agent/apply-event {:messages [] :stream {:tools 0}}
                                     {:type :tool/request :id "read-1" :name "Read" :summary "read file"})
        [state _] (agent/apply-event state {:type :tool/update
                                            :id "read-1"
                                            :status :done
                                            :summary "read README"})]
    (is (= :done (get-in state [:tools "read-1" :status])))
    (is (= {:role :tool
            :id "tool-message-read-1"
            :tool "Read"
            :status :done
            :text "read README"}
           (first (:messages state))))))

(deftest user-input-with-at-paths-emits-attachment-effects
  (let [[state effects] (agent/submit-user-input {:messages [] :stream {:queue 0}}
                                                 "read @README.md and @src/clj_agent_tui/app.clj")]
    (is (= ["README.md" "src/clj_agent_tui/app.clj"] (-> state :messages first :attachments)))
    (is (= [{:type :agent/send-message
             :text "read @README.md and @src/clj_agent_tui/app.clj"
             :attachments ["README.md" "src/clj_agent_tui/app.clj"]}
            {:type :context/attach :path "README.md"}
            {:type :context/attach :path "src/clj_agent_tui/app.clj"}]
           effects))))

(deftest shell-input-emits-shell-effect
  (let [[state effects] (agent/submit-user-input {:messages [] :stream {}} "!ls src")]
    (is (= {:role :shell :text "ls src" :status :pending :id "msg-1"}
           (first (:messages state))))
    (is (= :shell (get-in state [:stream :state])))
    (is (= [{:type :shell/run :command "ls src"}] effects))))
