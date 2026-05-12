(ns clj-agent-tui.commands
  "Data-first slash command registry for clj-agent-tui agent shells."
  (:require
   [clojure.string :as str]))

(def default-commands
  [{:name "help"
    :label "/help"
    :value "help"
    :description "Show keyboard shortcuts and available commands"}
   {:name "clear"
    :label "/clear"
    :value "clear"
    :description "Clear the visible conversation history"}
   {:name "status"
    :label "/status"
    :value "status"
    :description "Print current session and stream status"}
   {:name "model"
    :label "/model"
    :value "model"
    :description "Show or switch the active model"}
   {:name "react"
    :label "/react"
    :value "react"
    :description "Run a local deterministic ReAct demo loop"}
   {:name "tools"
    :label "/tools"
    :value "tools"
    :description "List registered tool placeholders"}
   {:name "permissions"
    :label "/permissions"
    :value "permissions"
    :description "Show current approval policy placeholder"}
   {:name "mcp"
    :label "/mcp"
    :value "mcp"
    :description "Show configured MCP integration placeholders"}
   {:name "lsp"
    :label "/lsp"
    :value "lsp"
    :description "Show LSP integration placeholder"}
   {:name "agents"
    :label "/agents"
    :value "agents"
    :description "Show subagent integration placeholders"}])

(defn command-options
  "Return command registry entries as selection option maps."
  ([] default-commands)
  ([commands] (vec commands)))

(defn parse-command
  "Parse a slash command string into {:name :args :raw}; returns nil otherwise."
  [text]
  (let [text (str/trim (or text ""))]
    (when (str/starts-with? text "/")
      (let [body (subs text 1)
            [name & args] (str/split body #"\s+")]
        (when (seq name)
          {:name (str/lower-case name)
           :args (vec (remove str/blank? args))
           :raw text})))))

(defn command-map
  [commands]
  (into {} (map (juxt :name identity) commands)))

(defn known-command?
  ([name] (known-command? default-commands name))
  ([commands name]
   (contains? (command-map commands) (str/lower-case (or name "")))))

(defn- status-message
  [state]
  (format "session:%s model:%s stream:%s queue:%d tools:%d"
          (or (get-in state [:session :title]) "clj-agent-tui")
          (or (get-in state [:session :model]) "unknown")
          (name (or (get-in state [:stream :state]) :idle))
          (long (or (get-in state [:stream :queue]) 0))
          (long (or (get-in state [:stream :tools]) 0))))

(defn execute-command
  "Execute a parsed slash command as a pure state transition.

  Returns `[state effects]`. Unknown commands leave state unchanged and emit a
  `:command/unknown` effect so a runtime can decide how to recover."
  ([state command] (execute-command state default-commands command))
  ([state commands {:keys [name args raw] :as command}]
   (let [name (str/lower-case (or name ""))]
     (if-not (known-command? commands name)
       [state [{:type :command/unknown :name name :raw raw}]]
       (case name
         "help"
         [(assoc state :dialog {:title "Keyboard Shortcuts"
                                :body "enter send · esc cancel · / suggestions · q quit"
                                :actions "enter close · esc close"})
          [{:type :command/handled :name name}]]

         "clear"
         [(assoc state :messages [])
          [{:type :history/clear :name name}]]

         "status"
         [(update state :messages conj {:role :system :text (status-message state)})
          [{:type :command/handled :name name}]]

         "model"
         (if-let [model (first args)]
           [(assoc-in state [:session :model] model)
            [{:type :model/select :model model :name name}]]
           [(update state :messages conj {:role :system
                                          :text (str "current model: "
                                                     (or (get-in state [:session :model]) "unknown"))})
            [{:type :command/handled :name name}]])

         "react"
         [state [{:type :react/run :question (str/join " " args) :name name}]]

         "tools"
         [(update state :messages conj {:role :system
                                        :text "tools: echo, count, shell, read, write (runtime adapters pending)"})
          [{:type :command/handled :name name}]]

         "permissions"
         [(update state :messages conj {:role :system
                                        :text (str "approval: " (name (or (get-in state [:settings :approval]) :default)))})
          [{:type :command/handled :name name}]]

         "mcp"
         [(update state :messages conj {:role :system
                                        :text (str "mcp servers: " (count (get-in state [:integrations :mcp] [])))})
          [{:type :command/handled :name name}]]

         "lsp"
         [(update state :messages conj {:role :system
                                        :text (str "lsp: " (if (get-in state [:integrations :lsp]) "configured" "none"))})
          [{:type :command/handled :name name}]]

         "agents"
         [(update state :messages conj {:role :system
                                        :text (str "subagents: " (count (get-in state [:integrations :subagents] [])))})
          [{:type :command/handled :name name}]]

         [state [{:type :command/handled :name (:name command)}]])))))
