# Release checklist

Use this checklist before publishing `com.github.rainmote/clj-agent-tui` to Clojars.

## One-time setup

- Confirm the Clojars group/artifact coordinate: `com.github.rainmote/clj-agent-tui`.
- Confirm the repository URL used by `build.clj` or set `SCM_URL` during release.
- Confirm the copyright holder in `LICENSE`.
- Configure Clojars deploy credentials (`CLOJARS_USERNAME` and `CLOJARS_PASSWORD`).

## Per release

1. Update `CHANGELOG.md` and remove `Unreleased` from the target version.
2. Commit the release-preparation changes.
3. Dry-run the tag-driven release task:

   ```bash
   bb release v0.1.0-alpha1 --dry-run
   ```

4. Run the one-command release:

   ```bash
   export CLOJARS_USERNAME=rainmote
   export CLOJARS_PASSWORD='your Clojars deploy token'
   bb release v0.1.0-alpha1
   ```

   The task derives `VERSION=0.1.0-alpha1` from the Git tag, then runs tests,
   lint, public API smoke, cljdoc config validation, jar build, annotated tag
   creation, branch/tag push to `origin`, and `clojure -T:build deploy`.

5. Verify Clojars and cljdoc render the published version.

## Babashka release flags

- `--dry-run` prints commands without running them.
- `--no-push` skips pushing the current branch and tag to `origin`.
- `--no-deploy` skips Clojars deploy.
- `--skip-tests` skips `clojure -M:test`.
- `--skip-lint` skips `clojure -M:lint`.
- `--allow-dirty` allows releasing from a dirty working tree.

## Manual fallback

```bash
clojure -M:test
clojure -M:lint
clojure -M -e "(require '[clj-agent-tui.core :as tui]) (println :ok)"
curl -fsSL https://raw.githubusercontent.com/cljdoc/cljdoc/master/script/verify-cljdoc-edn | bash -s doc/cljdoc.edn
VERSION=0.1.0-alpha1 clojure -T:build jar
git tag -a v0.1.0-alpha1 -m "Release v0.1.0-alpha1"
git push origin main
git push origin v0.1.0-alpha1
VERSION=0.1.0-alpha1 clojure -T:build deploy
```

## Development override

Default dependency resolution uses the published `de.timokramer/charm.clj` Maven
artifact so this library can be consumed from Clojars. Local development against
a sibling checkout is still available with:

```bash
clojure -M:dev:test
```
