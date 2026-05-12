(ns clj-agent-tui.core
  "Stable first-pass facade for clj-agent-tui consumers. Prefer this namespace
  from applications; lower-level namespaces remain available for focused use."
  (:require
   [clj-agent-tui.agent :as agent]
   [clj-agent-tui.app :as app]
   [clj-agent-tui.commands :as commands]
   [clj-agent-tui.components :as components]
   [clj-agent-tui.focus :as focus]
   [clj-agent-tui.integrations :as integrations]
   [clj-agent-tui.fuzzy :as fuzzy]
   [clj-agent-tui.provider :as provider]
   [clj-agent-tui.react :as react]
   [clj-agent-tui.runtime :as runtime]
   [clj-agent-tui.selection :as selection]
   [clj-agent-tui.session :as session]
   [clj-agent-tui.tools :as tools]))

(def view
  "Render a complete agent-style shell from a data state map."
  app/view)

(def update-fn
  "Default demo reducer. Dialog focus wins, then suggestions, then composer."
  app/update-fn)

(def run
  "Run a clj-agent-tui state with charm.clj's program runtime."
  app/run)

(def make-input
  "Create the default charm.clj text input used by the composer."
  app/make-input)

(def header
  "Render the top session header."
  components/header)

(def status-line
  "Render the stream/status line below the header."
  components/status-line)

(def history
  "Render bounded conversation history from the bottom."
  components/history)

(def composer
  "Render the bottom input composer."
  components/composer)

(def footer
  "Render the keyboard shortcut footer."
  components/footer)

(def modal
  "Render a blocking dialog/modal."
  components/modal)

(def rail
  "Render the right-side task/tool/agent rail."
  components/rail)

(def suggestions
  "Render a fuzzy suggestion popup."
  components/suggestions)

(def selection
  "Create a fuzzy selection state from option maps."
  selection/selection)

(def set-query
  "Set the query on a selection state and clamp the active row."
  selection/set-query)

(def update-selection
  "Apply a keyboard message to a selection state. Returns [state action]."
  selection/update-selection)

(def render-selection
  "Render a selection state into a bounded string surface."
  selection/render-selection)

(def filter-options
  "Filter and score option maps with fuzzy matching."
  fuzzy/filter-options)

(def submit-user-input
  "Submit a user draft through the pure agent/event boundary. Returns [state effects]."
  agent/submit-user-input)

(def apply-event
  "Apply a pure agent/runtime event to state. Returns [state effects]."
  agent/apply-event)

(def parse-command
  "Parse slash-command text into a command map, or nil."
  commands/parse-command)

(def execute-command
  "Execute a parsed slash command as a pure state/effect transition."
  commands/execute-command)

(def command-options
  "Return command registry entries as selection option maps."
  commands/command-options)

(def default-commands
  "Default slash-command registry used by demos."
  commands/default-commands)

(def active-focus
  "Return the currently active focus key from state."
  focus/active)

(def push-focus
  "Push a focus key onto the focus stack."
  focus/push-focus)

(def pop-focus
  "Pop the active focus key from the focus stack."
  focus/pop-focus)

(def focus
  "Replace the focus stack with one active focus key."
  focus/focus)

(def claude-config
  "Read Claude-shaped configuration from env/system properties without network calls."
  react/claude-config)

(def react-step
  "Execute one deterministic/data-driven ReAct step."
  react/step)

(def react-run-loop
  "Run a bounded ReAct loop over clj-agent-tui state."
  react/run-loop)

(def scripted-llm
  "Create a deterministic test/demo LLM function."
  react/scripted-llm)

(def default-tools
  "Return the default ReAct demo tool map."
  react/default-tools)

(def deterministic-provider
  "Create a local provider that echoes submitted text as stream events."
  provider/deterministic-provider)

(def claude-provider
  "Create an offline Claude-shaped provider wrapper for tests/adapters."
  provider/claude-provider)

(def stream-events
  "Return bounded agent events for a provider request."
  provider/stream-events)

(def default-tool-registry
  "Return the default local tool registry used by demos."
  tools/default-registry)

(def tool-options
  "Return registered tools as selection option maps."
  tools/tool-options)

(def run-tool
  "Run a named local tool from a registry."
  tools/run-tool)

(def drain-effects
  "Handle local effects until the queue is empty. Returns [state handled-effects]."
  runtime/drain-effects)

(def submit-and-run
  "Submit text, then drain all local runtime effects."
  runtime/submit-and-run)

(def save-session!
  "Persist a serializable state map to EDN."
  session/save-session!)

(def load-session
  "Load a persisted EDN session map."
  session/load-session)

(def mcp-server
  "Describe an MCP server and its tools as data."
  integrations/mcp-server)

(def mcp-tool-effect
  "Create a tool-execution effect for an MCP tool call."
  integrations/mcp-tool-effect)

(def lsp-request-effect
  "Create an LSP request effect map."
  integrations/lsp-request-effect)

(def subagent-effect
  "Create a subagent tool-execution effect map."
  integrations/subagent-effect)

(def describe-integrations
  "Summarize configured integration counts from state."
  integrations/describe-integrations)
