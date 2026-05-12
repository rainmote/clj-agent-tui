(ns clj-agent-tui.mock
  "Mock data and runnable demo for clj-agent-tui."
  (:require
   [clojure.string :as str]
   [clj-agent-tui.app :as app]
   [clj-agent-tui.commands :as commands]
   [clj-agent-tui.selection :as selection]))

(def suggestions
  (vec
   (concat (commands/command-options)
           [{:label "@README.md" :value "readme" :description "Attach README"}
            {:label "@src/clj_agent_tui/app.clj" :value "app" :description "Attach app source"}
            {:label "Plan mode" :value "plan" :description "Open planning workflow"}])))

(defn state
  ([] (state {}))
  ([overrides]
   (merge
    {:session {:title "Agent Code"
               :subtitle "clj-agent-tui"
               :model "agent-model-plus"
               :branch "feature/clj-agent-tui"
               :mode :interactive
               :status "mock"}
     :layout {:width 110 :height 34}
     :stream {:state :streaming :elapsed "00:18" :tokens 18472 :queue 2 :tools 4}
     :messages [{:role :user :text "请用 Clojure 构建一个 agent-code 风格 TUI。"}
                {:role :assistant :thought "Mapping agent-code layout into charm.clj primitives."
                 :text "我会优先复刻布局：顶部状态、历史区、底部 composer、modal 与选择列表。"}
                {:role :tool :tool "Read" :status :done :text "agent-code/packages/cli/src/ui/layouts/DefaultAppLayout.tsx"}
                {:role :assistant :text "已创建 mock-only 组件库边界，真实 LLM/API 集成保持在第一版之外。"}]
     :rail {:tasks [{:title "Scaffold standalone lib" :done true}
                    {:title "Render agent-like shell" :active true}
                    {:title "Verify mock layout"}]
            :tools [{:name "Read" :status :done :summary "agent-code UI refs"}
                    {:name "Test" :status :running :summary "render smoke"}]
            :agents [{:name "executor" :state :running}
                     {:name "reviewer" :state :queued}]}
     :input (app/make-input)
     :command-palette (selection/selection suggestions :title "Command Suggestions")
     :path-palette (selection/selection (filter #(str/starts-with? (:label %) "@") suggestions) :title "Path Suggestions")
     :composer {:mode :chat}
     :focus-stack [:composer]}
    overrides)))

(defn modal-state []
  (state {:dialog {:title "Approve tool call"
                   :body "Allow mock Read to inspect agent-code UI files?"
                   :actions "enter accept · n reject · esc cancel"}}))

(defn suggestions-state []
  (state {:suggestions (selection/set-query (selection/selection suggestions :title "Command Suggestions") "mo")}))

(defn -main [& _args]
  (app/run (state)))
