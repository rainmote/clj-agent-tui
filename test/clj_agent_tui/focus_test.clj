(ns clj-agent-tui.focus-test
  (:require
   [clojure.test :refer [deftest is]]
   [clj-agent-tui.focus :as focus]))

(deftest focus-stack-push-pop-and-replace
  (let [state (-> {}
                  (focus/push-focus :suggestions)
                  (focus/push-focus :dialog))]
    (is (= :dialog (focus/active state)))
    (is (= [:composer :suggestions :dialog] (focus/stack state)))
    (is (= :suggestions (focus/active (focus/pop-focus state))))
    (is (= [:composer] (:focus-stack (focus/focus state :composer))))))
