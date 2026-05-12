(ns build
  "Build and deployment tasks for clj-agent-tui."
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def lib 'rainmote/clj-agent-tui)
(def version (or (System/getenv "VERSION") "0.1.0-alpha1"))
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def basis (delay (b/create-basis {:project "deps.edn"})))

(def scm-url
  (or (System/getenv "SCM_URL")
      "https://github.com/rainmote/clj-agent-tui"))

(defn clean
  "Delete build outputs."
  [_]
  (b/delete {:path "target"}))

(defn- pom-data []
  [[:description "Composable agent terminal UI components and runtime seams for Clojure"]
   [:url scm-url]
   [:licenses
    [:license
     [:name "MIT License"]
     [:url "https://opensource.org/licenses/MIT"]]]])

(defn jar
  "Build a source jar plus pom under target/. Set VERSION to override the release version."
  [_]
  (clean nil)
  (let [basis @basis
        src-dirs (:paths basis)]
    (b/write-pom {:class-dir class-dir
                  :lib lib
                  :version version
                  :basis basis
                  :src-dirs src-dirs
                  :scm {:url scm-url
                        :connection (str "scm:git:" scm-url ".git")
                        :developerConnection "scm:git:ssh://git@github.com/rainmote/clj-agent-tui.git"
                        :tag (str "v" version)}
                  :pom-data (pom-data)})
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir class-dir})
    (doseq [file ["README.md" "CHANGELOG.md" "LICENSE"]]
      (b/copy-file {:src file :target (str class-dir "/" file)}))
    (b/jar {:class-dir class-dir
            :jar-file jar-file}))
  (println (str "Built " jar-file)))

(defn deploy
  "Deploy the built artifact to Clojars. Requires CLOJARS credentials."
  [opts]
  (jar opts)
  (dd/deploy {:installer :remote
              :artifact (b/resolve-path jar-file)
              :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))
