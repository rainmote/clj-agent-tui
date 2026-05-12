(ns clj-agent-tui.commands-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clj-agent-tui.commands :as commands]))

(deftest parse-command-recognizes-slash-forms
  (is (= {:name "model" :args ["agent-model"] :raw "/model agent-model"}
         (commands/parse-command " /model agent-model ")))
  (is (nil? (commands/parse-command "hello")))
  (is (nil? (commands/parse-command "/"))))

(deftest execute-command-handles-built-ins-purely
  (testing "help opens a dialog"
    (let [[state effects] (commands/execute-command {} {:name "help"})]
      (is (= "Keyboard Shortcuts" (get-in state [:dialog :title])))
      (is (= [{:type :command/handled :name "help"}] effects))))
  (testing "clear removes visible messages"
    (let [[state effects] (commands/execute-command {:messages [{:role :user :text "hi"}]}
                                                    {:name "clear"})]
      (is (= [] (:messages state)))
      (is (= [{:type :history/clear :name "clear"}] effects))))
  (testing "model with arg updates session model"
    (let [[state effects] (commands/execute-command {:session {:model "old"}}
                                                    {:name "model" :args ["new"]})]
      (is (= "new" (get-in state [:session :model])))
      (is (= [{:type :model/select :model "new" :name "model"}] effects)))))

(deftest unknown-command-emits-effect-without-state-change
  (let [state {:messages []}
        [next-state effects] (commands/execute-command state {:name "missing" :raw "/missing"})]
    (is (= state next-state))
    (is (= [{:type :command/unknown :name "missing" :raw "/missing"}] effects))))
