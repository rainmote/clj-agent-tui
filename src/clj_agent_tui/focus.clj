(ns clj-agent-tui.focus
  "Small focus-stack helpers for clj-agent-tui state maps.")

(def default-stack [:composer])

(defn stack
  [state]
  (vec (or (:focus-stack state) default-stack)))

(defn active
  "Return the currently focused surface keyword."
  [state]
  (peek (stack state)))

(defn push-focus
  "Push a focus surface, removing older duplicates first."
  [state surface]
  (assoc state :focus-stack (conj (vec (remove #{surface} (stack state))) surface)))

(defn pop-focus
  "Pop the active focus surface, falling back to :composer."
  [state]
  (let [next-stack (pop (stack state))]
    (assoc state :focus-stack (if (seq next-stack) next-stack default-stack))))

(defn focus
  "Replace focus stack with a single surface."
  [state surface]
  (assoc state :focus-stack [surface]))
