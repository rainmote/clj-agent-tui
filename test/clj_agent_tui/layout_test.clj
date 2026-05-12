(ns clj-agent-tui.layout-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clj-agent-tui.layout :as layout]))

(deftest block-clips-from-top-and-bottom
  (testing "top clipping"
    (is (= "a         \nb         " (layout/block "a\nb\nc" 10 2 :from :top))))
  (testing "bottom clipping keeps newest lines"
    (is (= "b         \nc         " (layout/block "a\nb\nc" 10 2 :from :bottom)))))

(deftest columns-preserve-height
  (is (= "a  c \nb  d "
         (layout/columns [{:content "a\nb" :width 3}
                          {:content "c\nd" :width 2}]
                         2))))
