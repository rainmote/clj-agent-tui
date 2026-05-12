(ns clj-agent-tui.session
  "EDN session persistence helpers for clj-agent-tui demos."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(defn serializable-state
  "Drop runtime-only values from state before persistence."
  [state]
  (dissoc state :input))

(defn save-session!
  [path state]
  (let [file (io/file path)]
    (io/make-parents file)
    (spit file (pr-str (serializable-state state)))
    {:path (.getPath file) :bytes (.length file)}))

(defn load-session
  [path]
  (edn/read-string (slurp (io/file path))))
