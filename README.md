# clj-agent-tui

[![CI](https://github.com/rainmote/clj-agent-tui/actions/workflows/ci.yml/badge.svg)](https://github.com/rainmote/clj-agent-tui/actions/workflows/ci.yml)
[![Clojars Project](https://img.shields.io/clojars/v/com.github.rainmote/clj-agent-tui.svg)](https://clojars.org/com.github.rainmote/clj-agent-tui)
[![cljdoc badge](https://cljdoc.org/badge/com.github.rainmote/clj-agent-tui)](https://cljdoc.org/d/com.github.rainmote/clj-agent-tui)

`clj-agent-tui` is a small Clojure library for composing agent-oriented terminal
UIs on top of [`charm.clj`](https://github.com/TimoKramer/charm.clj). It provides
rendering components, a pure event/effect boundary, and deterministic local
runtime adapters that are useful for demos, tests, and thin agent shells.

## Status

Alpha. The public facade in `clj-agent-tui.core` is the supported consumption
surface. Runtime/provider/ReAct/integration helpers are included as data-first
extension seams, but may change before `1.0`.

The library intentionally does not make network calls by default. Provider and
LLM behavior is bound by callers through data/functions.

## Installation

Add the library to `deps.edn` after it is published to Clojars:

```clojure
{:deps {com.github.rainmote/clj-agent-tui {:mvn/version "0.1.0-alpha1"}}}
```

The default dependency on `charm.clj` is a Maven/Clojars dependency so consumers
do not need a sibling checkout:

```clojure
de.timokramer/charm.clj {:mvn/version "0.2.71"}
```

For local development against a sibling `../charm.clj` checkout, use the `:dev`
alias provided by this repository.

## Quick start

```clojure
(require '[clj-agent-tui.core :as tui]
         '[clj-agent-tui.mock :as mock])

(println (tui/view (mock/state)))
```

Run the mock full-screen TUI:

```bash
clojure -M -m clj-agent-tui.mock
```

Run the deterministic ReAct demo:

```bash
clojure -M -m clj-agent-tui.react-demo "inspect this message"
```

## Public API

Prefer `clj-agent-tui.core` from applications. The facade exposes these groups:

| Group | Functions | Contract |
| --- | --- | --- |
| Rendering | `view`, `header`, `status-line`, `history`, `composer`, `footer`, `modal`, `rail`, `suggestions` | Pure string rendering from data maps. |
| App runtime | `run`, `update-fn`, `make-input` | Charm program integration for demos/shells. |
| Agent boundary | `submit-user-input`, `apply-event` | Pure state transition functions returning `[state effects]`. |
| Commands | `parse-command`, `execute-command`, `command-options`, `default-commands` | Data-first slash-command registry. |
| Selection | `selection`, `set-query`, `update-selection`, `render-selection`, `filter-options` | Fuzzy suggestion/filter state and rendering. |
| Focus | `active-focus`, `push-focus`, `pop-focus`, `focus` | Small focus-stack helpers over state maps. |
| Runtime adapters | `deterministic-provider`, `claude-provider`, `stream-events`, `default-tool-registry`, `tool-options`, `run-tool`, `drain-effects`, `submit-and-run`, `save-session!`, `load-session` | Local/demo adapters; production runtimes may replace them. |
| ReAct and integrations | `claude-config`, `react-step`, `react-run-loop`, `scripted-llm`, `default-tools`, `mcp-server`, `mcp-tool-effect`, `lsp-request-effect`, `subagent-effect`, `describe-integrations` | Experimental extension seams. |

### State contract

`view` expects a shell state map with these common keys:

```clojure
{:session {:title "Agent" :subtitle "clj-agent-tui" :model "model" :branch "main" :mode "chat"}
 :layout {:width 100 :height 32}
 :stream {:state :idle :queue 0 :tokens 0 :tools 0}
 :messages [{:role :user :text "hello"}]
 :rail {:tasks [] :tools [] :agents []}
 :input ...}
```

Optional `:dialog` and `:suggestions` keys take focus priority over composer
input.

### Event/effect boundary

Normal user input is appended to history and returns an effect for an external
runtime:

```clojure
(tui/submit-user-input {:messages [] :stream {:queue 0}} "hello")
;; => [state [{:type :agent/send-message :text "hello"}]]
```

`apply-event` accepts runtime events such as `:assistant/start`,
`:assistant/delta`, `:assistant/done`, `:tool/request`, `:tool/update`,
`:shell/result`, `:settings/update`, and `:agent/error`.

## Development

```bash
clojure -M:test
clojure -M:lint
clojure -M -e "(require '[clj-agent-tui.core :as tui]) (println :ok)"
clojure -T:build jar
```

Use the local `charm.clj` checkout during development:

```bash
clojure -M:dev:test
```

## Release

Release tasks are implemented in `build.clj` and wrapped by Babashka for the
one-command release flow.

Dry-run the full release plan for a Git tag:

```bash
bb release v0.1.0-alpha1 --dry-run
```

Run the real release. The task derives `VERSION=0.1.0-alpha1` from the Git tag,
verifies the project, builds the jar, creates and pushes the annotated tag, and
publishes to Clojars:

```bash
export CLOJARS_USERNAME=rainmote
export CLOJARS_PASSWORD='your Clojars deploy token'
bb release v0.1.0-alpha1
```

Useful alternatives:

```bash
bb release v0.1.0-alpha1 --no-deploy   # build, tag, and push only
bb release v0.1.0-alpha1 --no-push     # build, tag locally, and deploy only
bb release v0.1.0-alpha1 --dry-run     # print commands without running them
```

See [`doc/release.md`](doc/release.md) for the full release checklist.

## License

MIT. See [`LICENSE`](LICENSE).
