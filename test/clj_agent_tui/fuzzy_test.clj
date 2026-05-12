(ns clj-agent-tui.fuzzy-test
  (:require
   [clojure.test :refer [deftest is]]
   [clj-agent-tui.fuzzy :as fuzzy]))

(deftest fuzzy-matches-sequentially
  (is (fuzzy/matches? "mdl" "/model"))
  (is (fuzzy/matches? "RM" "README.md"))
  (is (not (fuzzy/matches? "zz" "README.md"))))

(deftest filters-and-sorts-options
  (let [result (fuzzy/filter-options "mo" [{:label "/help"}
                                           {:label "/model" :description "Switch model"}])]
    (is (= ["/model"] (mapv :label result)))))

(deftest command-labels-outrank-and-filter-description-hits
  (let [options [{:label "/help" :value "help" :description "Show keyboard shortcuts"}
                 {:label "/model" :value "model" :description "Switch model"}
                 {:label "@README.md" :value "readme" :description "Attach README"}]]
    (is (= ["/help"] (mapv :label (fuzzy/filter-options "h" options))))
    (is (= ["/help"] (mapv :label (fuzzy/filter-options "help" options))))
    (is (= ["/model"] (mapv :label (fuzzy/filter-options "switch" options))))))
