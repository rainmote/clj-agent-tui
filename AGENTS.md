# Repository Guidelines

## Project Structure & Module Organization

This repository is a small Clojure library for rendering an agent-oriented TUI on top of the published `de.timokramer/charm.clj` dependency. Source files live in `src/clj_agent_tui/` and use `clj-agent-tui.*` namespaces. Tests mirror the same namespace layout in `test/clj_agent_tui/`.

Key entry points:
- `src/clj_agent_tui/core.clj`: public facade for consumers.
- `src/clj_agent_tui/app.clj`: full app rendering and update flow.
- `src/clj_agent_tui/components.clj`: reusable TUI rendering pieces.
- `src/clj_agent_tui/mock.clj`: mock state and demo runner.

## Build, Test, and Development Commands

- `clojure -M:test` — runs the full `clojure.test` suite through Cognitect test-runner.
- `clojure -M:lint` — runs clj-kondo on `src` and `test`, failing on errors.
- `clojure -M -m clj-agent-tui.mock` — starts the mock TUI demo.
- `clojure -M -e "(require '[clj-agent-tui.core :as tui])"` — quick load check for the public API.
- `bb tasks` — lists Babashka convenience tasks.
- `bb release v0.1.0-alpha1 --dry-run` — previews the tag-driven release flow.
- `bb release v0.1.0-alpha1` — verifies, builds, tags, pushes, and deploys to Clojars.

`clojure -T:build jar` builds the publishable jar. Keep changes library-first and test with the commands above.

## Coding Style & Naming Conventions

Use idiomatic Clojure formatting with two-space indentation. Keep namespace filenames in snake_case (`clj_agent_tui/app.clj`) and namespace names hyphenated (`clj-agent-tui.app`). Prefer small pure functions that consume and return data maps. Public functions should have short docstrings when exposed through `clj-agent-tui.core`.

Avoid importing `charm.agent.*`; use stable `charm.clj` primitives instead. If duplicated helpers grow, prefer upstream extraction into stable `charm.clj` primitives rather than adding local abstraction layers.

## Testing Guidelines

Tests use `clojure.test` and should be placed in matching `*_test.clj` files under `test/clj_agent_tui/`. Name tests by behavior, for example `view-respects-requested-terminal-height`. For rendering tests, strip ANSI escape codes before assertions and verify layout dimensions, focus priority, and visible text.

Run `clojure -M:test` before submitting. Add focused regression tests for any change to input handling, selection, layout, or rendering.

## Commit & Pull Request Guidelines

This checkout has no local git history to infer additional project-specific conventions. Use concise, intent-first commit messages and include Lore-style trailers when useful, especially `Tested:` and `Not-tested:`. Example: `Tested: clojure -M:test; clojure -M:lint`.

Pull requests should include a short description, affected namespaces, test results, and screenshots or terminal output for visible TUI changes.
