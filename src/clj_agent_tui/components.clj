(ns clj-agent-tui.components
  "Reusable agent-code-style Agent TUI render components."
  (:require
   [charm.style.core :as style]
   [clj-agent-tui.layout :as layout]
   [clj-agent-tui.selection :as selection]
   [clj-agent-tui.theme :as theme]
   [clojure.string :as str]))

(defn header
  [{:keys [title subtitle model branch mode width status]}]
  (let [width (or width 100)
        left (str (style/render theme/logo (or title "Agent Code"))
                  (style/render theme/muted "  •  ")
                  (style/render theme/title (or subtitle "Clojure Agent TUI")))
        right (str (style/render theme/muted (or branch "main"))
                   (style/render theme/muted "  ")
                   (style/render theme/accent (or model "agent-model"))
                   (style/render theme/muted "  ")
                   (style/render theme/muted (name (or mode :interactive)))
                   (when status (str (style/render theme/muted "  ") (style/render theme/success status))))
        gap (apply str (repeat (max 1 (- width (style/string-width left) (style/string-width right))) " "))]
    (layout/line (str left gap right) width)))

(defn status-line
  [{:keys [state elapsed tokens queue tools width]}]
  (layout/line
   (style/render theme/accent
                 (format "● %s   %s   %,d tokens   queue:%d   tools:%d"
                         (name (or state :idle))
                         (or elapsed "00:00")
                         (long (or tokens 0))
                         (long (or queue 0))
                         (long (or tools 0))))
   (or width 100)))

(defn- render-content-line
  [line width]
  (let [plain (or line "")]
    (cond
      (str/starts-with? plain "#")
      (layout/line (style/render theme/title plain) width)

      (str/starts-with? plain "+")
      (layout/line (style/render theme/success plain) width)

      (str/starts-with? plain "-")
      (layout/line (style/render theme/danger plain) width)

      (str/starts-with? plain "```")
      (layout/line (style/render theme/subtle plain) width)

      :else
      (layout/line plain width))))

(defn message-block
  [{:keys [role text thought tool status title attachments]} width]
  (let [label (case role
                :user (style/render theme/accent "You")
                :assistant (style/render theme/logo "Agent")
                :system (style/render theme/muted "System")
                :tool (style/render theme/warning (or tool "Tool"))
                :shell (style/render theme/warning "Shell")
                :summary (style/render theme/success (or title "Summary"))
                (style/render theme/muted (name role)))
        status (when status (str " " (style/render theme/muted (name status))))
        body-width (max 12 (- width 4))
        attachment-line (when (seq attachments)
                          (str "  " (style/render theme/muted
                                                (str "attachments: " (str/join ", " attachments)))))
        body (->> (layout/split-lines text)
                  (map #(str "  " (render-content-line % body-width)))
                  (str/join "\n"))]
    (layout/stack
     (str label status)
     (when thought (str "  " (style/render theme/muted (str "thought: " thought))))
     attachment-line
     body)))

(defn history
  [{:keys [messages width height]}]
  (let [width (or width 80)
        height (or height 12)
        rendered (->> messages
                      (map #(message-block % width))
                      (str/join "\n\n"))]
    (layout/block rendered width height :from :bottom)))

(defn composer
  [{:keys [draft placeholder queue streaming? elapsed tokens tools width mode]}]
  (let [width (or width 100)
        mode (or mode :chat)
        state-label (if streaming? "Streaming" "Ready")
        status-text (format "● %s · %s · %,d tokens · queue:%d · tools:%d · mode:%s"
                            state-label
                            (or elapsed "00:00")
                            (long (or tokens 0))
                            (long (or queue 0))
                            (long (or tools 0))
                            (name mode))
        value (if (str/blank? draft)
                (style/render theme/muted (or placeholder (case mode
                                       :shell "!shell command"
                                       :at-path "@path/to/file"
                                       :slash "/command"
                                       "Type your message or @path/to/file")))
                draft)
        top-rule (layout/rule width)
        bottom-rule (layout/rule width)]
    (layout/stack
     (layout/line (style/render theme/muted status-text) width)
     (layout/line (style/render theme/subtle top-rule) width)
     (layout/line (str (style/render theme/accent (case mode :shell "! " :at-path "@ " :slash "/ " "❯ ")) value) width)
     (layout/line (style/render theme/subtle bottom-rule) width))))

(defn footer
  [{:keys [width]}]
  (layout/line
   (str (style/render theme/muted "Enter") " send  "
        (style/render theme/muted "Esc") " cancel  "
        (style/render theme/muted "?") " shortcuts  "
        (style/render theme/muted "↑/↓") " navigate  "
        (style/render theme/muted "q") " quit  "
        (style/render theme/muted "!") " shell  "
        (style/render theme/muted "@") " attach")
   (or width 100)))

(defn modal
  [{:keys [title body actions]}]
  (style/render theme/active-panel
                (layout/stack
                 (style/render theme/title (or title "Confirm"))
                 (or body "")
                 ""
                 (style/render theme/muted (or actions "enter accept · esc cancel")))))

(defn rail
  [{:keys [tasks tools agents width]}]
  (let [width (or width 32)
        task-rows (map (fn [{:keys [title done active]}]
                         (str (cond done "✓" active "●" :else "○") " " title))
                       tasks)
        tool-rows (map (fn [{:keys [name status summary]}]
                         (str (case status :done "✓" :running "●" :error "✗" "•") " " name " — " summary))
                       tools)
        agent-rows (map (fn [{:keys [name state]}] (str "◇ " name " " (clojure.core/name state))) agents)]
    (style/render theme/panel
                  (layout/block
                   (layout/stack
                    (style/render theme/accent "Plan")
                    (str/join "\n" task-rows)
                    ""
                    (style/render theme/accent "Tools")
                    (str/join "\n" tool-rows)
                    ""
                    (style/render theme/accent "Agents")
                    (str/join "\n" agent-rows))
                   (- width 4) 18))))

(defn suggestions
  [state opts]
  (selection/render-selection state opts))
