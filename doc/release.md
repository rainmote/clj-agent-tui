# Release checklist

Use this checklist before publishing `rainmote/clj-agent-tui` to Clojars.

## One-time setup

- Confirm the Clojars group/artifact coordinate: `rainmote/clj-agent-tui`.
- Confirm the repository URL used by `build.clj` or set `SCM_URL` during release.
- Confirm the copyright holder in `LICENSE`.
- Configure Clojars deploy credentials (`CLOJARS_USERNAME` and `CLOJARS_PASSWORD`).

## Per release

1. Update `CHANGELOG.md` and remove `Unreleased` from the target version.
2. Set the release version explicitly, for example:

   ```bash
   VERSION=0.1.0-alpha1 clojure -T:build jar
   ```

3. Verify locally:

   ```bash
   clojure -M:test
   clojure -M:lint
   clojure -M -e "(require '[clj-agent-tui.core :as tui]) (println :ok)"
   clojure -T:build jar
   ```

4. Deploy:

   ```bash
   VERSION=0.1.0-alpha1 clojure -T:build deploy
   ```

5. Create and push the matching Git tag, for example `v0.1.0-alpha1`.
6. Verify Clojars and cljdoc render the published version.

## Development override

Default dependency resolution uses the published `de.timokramer/charm.clj` Maven
artifact so this library can be consumed from Clojars. Local development against
a sibling checkout is still available with:

```bash
clojure -M:dev:test
```
