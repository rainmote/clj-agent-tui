(ns clj-agent-tui.integrations
  "Minimal MCP/LSP/subagent integration seams for clj-agent-tui.

  These adapters are intentionally local and declarative: they provide stable
  data shapes that a production runtime can replace with real clients.")

(defn mcp-server
  [name tools]
  {:type :mcp/server
   :name name
   :tools (vec tools)
   :status :configured})

(defn mcp-tool-effect
  [server tool args]
  {:type :tool/execute
   :tool-id (str "mcp-" server "-" tool)
   :tool {:name (str server "/" tool)
          :args args
          :source :mcp}})

(defn lsp-request-effect
  [method params]
  {:type :lsp/request
   :method method
   :params params})

(defn subagent-effect
  [agent-name task]
  {:type :tool/execute
   :tool-id (str "subagent-" (Math/abs (hash [agent-name task])))
   :tool {:name "agent"
          :args {:agent agent-name :task task}
          :source :subagent}})

(defn describe-integrations
  [state]
  (let [mcp-count (count (get-in state [:integrations :mcp] []))
        lsp? (boolean (get-in state [:integrations :lsp]))
        subagents (count (get-in state [:integrations :subagents] []))]
    (str "integrations: mcp=" mcp-count
         " lsp=" (if lsp? "configured" "none")
         " subagents=" subagents)))
