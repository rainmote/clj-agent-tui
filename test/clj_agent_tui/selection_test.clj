(ns clj-agent-tui.selection-test
  (:require
   [charm.message :as msg]
   [clojure.test :refer [deftest is]]
   [clojure.string :as str]
   [clj-agent-tui.selection :as selection]))

(def opts [{:label "/help" :value "help"}
           {:label "/model" :value "model"}
           {:label "/memory" :value "memory"}])

(deftest selection-navigation-and-select
  (let [s (selection/selection opts)
        [s _] (selection/update-selection s (msg/key-press :down))
        [s action] (selection/update-selection s (msg/key-press :enter))]
    (is (= :selected action))
    (is (= "model" (get-in s [:selected :value])))))

(deftest selection-query-filters
  (let [s (selection/set-query (selection/selection opts) "mry")]
    (is (= ["memory"] (mapv :value (selection/visible-options s))))))

(deftest selection-closes-on-escape
  (let [[s action] (selection/update-selection (selection/selection opts) (msg/key-press :escape))]
    (is (= :closed action))
    (is (:closed? s))))


(deftest render-scrolls-active-row-into-view
  (let [options (mapv (fn [n] {:label (str "Option " n) :value (str n)}) (range 10))
        rendered (selection/render-selection (selection/selection options :height 3 :active 5))]
    (is (str/includes? rendered "Option 5"))
    (is (str/includes? rendered "❯"))
    (is (str/includes? rendered "▲"))
    (is (str/includes? rendered "▼"))
    (is (str/includes? rendered "(6/10)"))))


(deftest typing-while-focused-updates-query
  (let [[s _] (selection/update-selection (selection/selection opts) (msg/key-press "m"))
        [s _] (selection/update-selection s (msg/key-press "o"))]
    (is (= "mo" (:query s)))
    (is (= ["model" "memory"] (mapv :value (selection/visible-options s))))))

(deftest backspace-edits-focused-query
  (let [s (selection/set-query (selection/selection opts) "mod")
        [s _] (selection/update-selection s (msg/key-press :backspace))]
    (is (= "mo" (:query s)))))
