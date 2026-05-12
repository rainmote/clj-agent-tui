(ns release
  (:require
   [babashka.process :refer [shell]]
   [clojure.string :as str]))

(def usage
  "Usage: bb release <git-tag> [--dry-run] [--no-push] [--no-deploy] [--skip-tests] [--skip-lint] [--allow-dirty]

Examples:
  bb release v0.1.0-alpha1
  bb release 0.1.0-alpha1 --dry-run

The release task derives VERSION from the tag by stripping a leading v, then:
  1. verifies the repo is clean
  2. runs tests, lint, public require smoke, and cljdoc config validation
  3. builds the jar with VERSION=<derived-version>
  4. creates an annotated git tag
  5. pushes the current branch and tag to origin
  6. deploys to Clojars with clojure -T:build deploy

Clojars deploy requires CLOJARS_USERNAME and CLOJARS_PASSWORD in the environment.")

(def valid-options
  #{"--dry-run" "--no-push" "--no-deploy" "--skip-tests" "--skip-lint" "--allow-dirty"})

(defn die!
  ([message] (die! message 1))
  ([message code]
   (binding [*out* *err*]
     (println message)
     (println)
     (println usage))
   (System/exit code)))

(defn parse-args [args]
  (let [options (set (filter #(str/starts-with? % "--") args))
        unknown (seq (remove valid-options options))
        positionals (remove #(str/starts-with? % "--") args)]
    (when unknown
      (die! (str "Unknown option(s): " (str/join ", " unknown))))
    (when-not (= 1 (count positionals))
      (die! "Exactly one git tag argument is required."))
    {:tag-arg (first positionals)
     :dry-run? (contains? options "--dry-run")
     :push? (not (contains? options "--no-push"))
     :deploy? (not (contains? options "--no-deploy"))
     :tests? (not (contains? options "--skip-tests"))
     :lint? (not (contains? options "--skip-lint"))
     :allow-dirty? (contains? options "--allow-dirty")}))

(defn normalize-tag [tag]
  (if (str/starts-with? tag "v") tag (str "v" tag)))

(defn tag->version [tag]
  (str/replace-first tag #"^v" ""))

(defn valid-tag? [tag]
  (boolean (re-matches #"v?\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?" tag)))

(defn command-line [env command]
  (str (when (seq env)
         (str (str/join " " (map (fn [[k v]] (str k "=" v)) env)) " "))
       command))

(defn run!
  ([ctx command] (run! ctx {} command))
  ([{:keys [dry-run?]} {:keys [env]} command]
   (println "+" (command-line env command))
   (if dry-run?
     {:exit 0}
     (shell (cond-> {:inherit true}
              (seq env) (assoc :extra-env env))
            command))))

(defn capture! [command]
  (str/trim (:out (shell {:out :string :err :inherit} command))))

(defn ensure-git-repo! [ctx]
  (run! ctx "git rev-parse --is-inside-work-tree"))

(defn ensure-clean! [{:keys [dry-run? allow-dirty?] :as ctx}]
  (when-not allow-dirty?
    (if dry-run?
      (println "+ git status --porcelain # must be empty")
      (let [status (capture! "git status --porcelain")]
        (when-not (str/blank? status)
          (die! (str "Working tree is not clean. Commit/stash changes before release, or pass --allow-dirty.\n\n" status))))))
  ctx)

(defn ensure-tag-free! [{:keys [dry-run? tag] :as ctx}]
  (if dry-run?
    (println "+" (str "git rev-parse -q --verify refs/tags/" tag " # must not exist"))
    (let [result (shell {:out :string :err :string :continue true}
                        (str "git rev-parse -q --verify refs/tags/" tag))]
      (when (zero? (:exit result))
        (die! (str "Tag already exists locally: " tag)))))
  ctx)

(defn ensure-credentials! [{:keys [deploy? dry-run?] :as ctx}]
  (when (and deploy? (not dry-run?))
    (doseq [name ["CLOJARS_USERNAME" "CLOJARS_PASSWORD"]]
      (when (str/blank? (System/getenv name))
        (die! (str name " is required for Clojars deploy.")))))
  ctx)

(defn current-branch []
  (capture! "git rev-parse --abbrev-ref HEAD"))

(defn verify! [{:keys [tests? lint?] :as ctx}]
  (when tests?
    (run! ctx "clojure -M:test"))
  (when lint?
    (run! ctx "clojure -M:lint"))
  (run! ctx "clojure -M -e \"(require '[clj-agent-tui.core :as tui]) (println :ok)\"")
  (run! ctx "curl -fsSL https://raw.githubusercontent.com/cljdoc/cljdoc/master/script/verify-cljdoc-edn | bash -s doc/cljdoc.edn"))

(defn build! [{:keys [version] :as ctx}]
  (run! ctx {:env {"VERSION" version}} "clojure -T:build jar"))

(defn tag! [{:keys [tag] :as ctx}]
  (run! ctx (str "git tag -a " tag " -m \"Release " tag "\"")))

(defn push! [{:keys [push? dry-run? tag] :as ctx}]
  (when push?
    (let [branch (if dry-run? "$(git rev-parse --abbrev-ref HEAD)" (current-branch))]
      (when (= branch "HEAD")
        (die! "Cannot push release from detached HEAD."))
      (run! ctx (str "git push origin " branch))
      (run! ctx (str "git push origin " tag)))))

(defn deploy! [{:keys [deploy? version] :as ctx}]
  (when deploy?
    (run! ctx {:env {"VERSION" version}} "clojure -T:build deploy")))

(defn -main [& args]
  (let [{:keys [tag-arg] :as opts} (parse-args args)]
    (when-not (valid-tag? tag-arg)
      (die! (str "Invalid tag: " tag-arg ". Expected vMAJOR.MINOR.PATCH[-QUALIFIER] or MAJOR.MINOR.PATCH[-QUALIFIER].")))
    (let [tag (normalize-tag tag-arg)
          version (tag->version tag)
          ctx (assoc opts :tag tag :version version)]
      (println "Release tag:" tag)
      (println "Maven version:" version)
      (println "Artifact: com.github.rainmote/clj-agent-tui")
      (ensure-git-repo! ctx)
      (ensure-clean! ctx)
      (ensure-tag-free! ctx)
      (ensure-credentials! ctx)
      (verify! ctx)
      (build! ctx)
      (tag! ctx)
      (push! ctx)
      (deploy! ctx)
      (println "Release task complete:" tag))))

(apply -main *command-line-args*)
