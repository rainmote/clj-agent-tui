(ns clj-agent-tui.app-test
  (:require
   [charm.message :as msg]
   [charm.components.text-input :as text-input]
   [clojure.test :refer [deftest is testing]]
   [clj-agent-tui.app :as app]
   [clj-agent-tui.mock :as mock]
   [clojure.string :as str]))

(def ansi-re #"\u001B\[[;?0-9]*[ -/]*[@-~]")

(defn plain [s]
  (str/replace s ansi-re ""))

(defn line-index-containing [rendered needle]
  (first
   (keep-indexed
    (fn [idx line]
      (when (str/includes? line needle) idx))
    (str/split-lines rendered))))

(deftest mock-render-has-required-wide-layout-content
  (let [rendered (plain (app/view (mock/state)))]
    (is (not (str/includes? rendered "TUI:")))
    (is (str/includes? rendered "Agent Code"))
    (is (str/includes? rendered "Plan"))
    (is (str/includes? rendered "Tools"))
    (is (str/includes? rendered "Agents"))
    (is (str/includes? rendered "Type your message or @path/to/file"))
    (is (str/includes? rendered "queue:2"))
    (is (str/includes? rendered "tools:4"))
    (is (str/includes? rendered "Streaming"))))

(deftest modal-render-and-focus-priority
  (testing "modal renders and composer is suppressed"
    (let [rendered (plain (app/view (mock/modal-state)))]
      (is (str/includes? rendered "Approve tool call"))
      (is (not (str/includes? rendered "Type your message or @path/to/file")))))
  (testing "dialog consumes enter instead of submitting composer"
    (let [state (mock/modal-state)
          original-count (count (:messages state))
          [next-state _] (app/update-fn state (msg/key-press :enter))]
      (is (= original-count (count (:messages next-state))))
      (is (= :accept (:last-dialog-action next-state))))))

(deftest suggestions-render-and-focus-priority
  (let [state (mock/suggestions-state)
        rendered (plain (app/view state))]
    (is (not (str/includes? rendered "TUI:")))
    (is (str/includes? rendered "Command Suggestions"))
    (is (str/includes? rendered "/model"))
    (is (str/includes? rendered "❯ /mo"))
    (is (< (line-index-containing rendered "❯ /mo")
           (line-index-containing rendered "Command Suggestions"))))
  (let [state (mock/suggestions-state)
        [next-state _] (app/update-fn state (msg/key-press :enter))]
    (is (:last-selection next-state))
    (is (nil? (:suggestions next-state)))
    (is (= "/model" (text-input/value (:input next-state))))))

(deftest narrow-layout-collapses-rail
  (let [rendered (plain (app/view (mock/state {:layout {:width 80 :height 28}})))]
    (is (not (str/includes? rendered "TUI:")))
    (is (str/includes? rendered "Agent Code"))
    (is (str/includes? rendered "Type your message or @path/to/file"))
    (is (not (str/includes? rendered "Agents")))))

(defn line-count [s]
  (count (str/split-lines s)))

(deftest view-respects-requested-terminal-height
  (doseq [[label state] [["wide" (mock/state {:layout {:width 110 :height 34}})]
                         ["narrow" (mock/state {:layout {:width 80 :height 28}})]
                         ["modal" (mock/modal-state)]
                         ["suggestions" (mock/suggestions-state)]
                         ["tiny" (mock/state {:layout {:width 60 :height 10}})]]]
    (let [rendered (plain (app/view state))
          expected (get-in state [:layout :height])]
      (is (= expected (line-count rendered)) label))))


(deftest slash-opens-suggestions-and-typing-filters-query
  (let [[state _] (app/update-fn (mock/state) (msg/key-press "/"))
        [state _] (app/update-fn state (msg/key-press "m"))
        [state _] (app/update-fn state (msg/key-press "o"))
        rendered (plain (app/view state))]
    (is (= "mo" (get-in state [:suggestions :query])))
    (is (= "/mo" (text-input/value (:input state))))
    (is (str/includes? rendered "query:mo"))
    (is (str/includes? rendered "❯ /mo"))
    (is (str/includes? rendered "/model"))))

(deftest slash-help-filters-to-help-command
  (let [[state _] (app/update-fn (mock/state) (msg/key-press "/"))
        [state _] (app/update-fn state (msg/key-press "h"))
        h-rendered (plain (app/view state))
        [state _] (app/update-fn state (msg/key-press "e"))
        [state _] (app/update-fn state (msg/key-press "l"))
        [state _] (app/update-fn state (msg/key-press "p"))
        rendered (plain (app/view state))]
    (is (str/includes? h-rendered "/help"))
    (is (not (str/includes? h-rendered "/model")))
    (is (= "help" (get-in state [:suggestions :query])))
    (is (str/includes? rendered "/help"))
    (is (not (str/includes? rendered "/model")))))

(deftest slash-fuzzy-selection-autocompletes-input
  (let [[state _] (app/update-fn (mock/state) (msg/key-press "/"))
        [state _] (app/update-fn state (msg/key-press "m"))
        [state _] (app/update-fn state (msg/key-press "o"))
        [state _] (app/update-fn state (msg/key-press :enter))]
    (is (nil? (:suggestions state)))
    (is (= "/model" (text-input/value (:input state))))))

(deftest slash-trigger-can-be-deleted-when-query-is-empty
  (let [[state _] (app/update-fn (mock/state) (msg/key-press "/"))
        [state _] (app/update-fn state (msg/key-press :backspace))
        rendered (plain (app/view state))]
    (is (nil? (:suggestions state)))
    (is (= "" (text-input/value (:input state))))
    (is (str/includes? rendered "Type your message or @path/to/file"))
    (is (not (str/includes? rendered "Command Suggestions")))))

(deftest shell-mode-submits-shell-effect
  (let [[state _] (app/update-fn (mock/state {:stream {:state :idle :elapsed "00:00" :tokens 0 :queue 0 :tools 0}})
                                 (msg/key-press "!"))
        [state _] (app/update-fn state (msg/key-press "e"))
        [state _] (app/update-fn state (msg/key-press "c"))
        [state _] (app/update-fn state (msg/key-press "h"))
        [state _] (app/update-fn state (msg/key-press "o"))
        [state _] (app/update-fn state (msg/key-press :enter))]
    (is (= [{:type :shell/run :command "echo"}] (:last-effects state)))
    (is (= :chat (get-in state [:composer :mode])))
    (is (= :composer (last (:focus-stack state))))))

(deftest at-trigger-opens-path-suggestions
  (let [[state _] (app/update-fn (mock/state) (msg/key-press "@"))
        rendered (plain (app/view state))]
    (is (= "@" (get-in state [:suggestions :trigger])))
    (is (= :at-path (get-in state [:composer :mode])))
    (is (str/includes? rendered "Path Suggestions"))
    (is (str/includes? rendered "@README.md"))))
