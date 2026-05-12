(ns clj-agent-tui.selection
  "Fuzzy-searchable selection list state and rendering."
  (:require
   [charm.message :as msg]
   [charm.style.core :as style]
   [clj-agent-tui.fuzzy :as fuzzy]
   [clj-agent-tui.layout :as layout]
   [clj-agent-tui.theme :as theme]
   [clojure.string :as str]))

(def default-height 8)

(defn selection
  "Create a selection state from option maps with :label, :value, :description."
  [options & {:keys [query active height title]
              :or {query "" active 0 height default-height title "Suggestions"}}]
  {:type :selection
   :title title
   :options (vec options)
   :query query
   :active active
   :height height
   :closed? false
   :selected nil})

(defn visible-options [state]
  (fuzzy/filter-options (:query state) (:options state)))

(defn selected-option [state]
  (nth (visible-options state) (:active state 0) nil))

(defn clamp-active [state]
  (let [n (count (visible-options state))]
    (assoc state :active (if (pos? n) (max 0 (min (:active state 0) (dec n))) 0))))

(defn set-query [state query]
  (clamp-active (assoc state :query query :active 0)))

(defn move-active [state delta]
  (let [n (count (visible-options state))]
    (if (pos? n)
      (update state :active #(mod (+ (or % 0) delta) n))
      (assoc state :active 0))))

(defn select-active [state]
  (assoc state :selected (selected-option state) :closed? true))

(defn- printable-key
  [m]
  (let [k (:key m)]
    (when (and (msg/key-press? m)
               (string? k)
               (= 1 (count k))
               (not (msg/ctrl? m))
               (not (msg/alt? m)))
      k)))

(defn append-query [state text]
  (set-query state (str (:query state "") text)))

(defn delete-query-backward [state]
  (let [query (:query state "")]
    (if (seq query)
      (set-query state (subs query 0 (dec (count query))))
      state)))

(defn update-selection
  "Update selection state from charm key messages.

  Returns `[new-state action]` where action is nil, `:selected`, or `:closed`."
  [state m]
  (cond
    (or (msg/key-match? m :down) (msg/key-match? m "j"))
    [(move-active state 1) nil]

    (or (msg/key-match? m :up) (msg/key-match? m "k"))
    [(move-active state -1) nil]

    (or (msg/key-match? m "backspace") (msg/key-match? m :backspace))
    [(delete-query-backward state) nil]

    (msg/key-match? m "enter")
    [(select-active state) :selected]

    (or (msg/key-match? m "esc") (msg/key-match? m :escape))
    [(assoc state :closed? true) :closed]

    (printable-key m)
    [(append-query state (printable-key m)) nil]

    :else
    [state nil]))

(defn- visible-window
  [active height option-count]
  (let [height (max 1 height)
        active (max 0 (min active (max 0 (dec option-count))))
        start (cond
                (<= option-count height) 0
                (< active height) 0
                :else (min (- option-count height) (- active (dec height))))]
    {:start start
     :end (min option-count (+ start height))}))

(defn render-selection
  "Render a agent-code-like fuzzy suggestion surface."
  ([state] (render-selection state {}))
  ([state {:keys [width] :or {width 80}}]
   (let [options (visible-options state)
         height (:height state default-height)
         option-count (count options)
         active (:active state 0)
         {:keys [start end]} (visible-window active height option-count)
         visible (subvec (vec options) start end)
         rows (map-indexed
               (fn [idx option]
                 (let [global-idx (+ start idx)
                       active? (= global-idx active)
                       row-style (if active? theme/selected theme/muted)
                       pointer (if active? "❯ " "  ")
                       label (or (:label option) (:value option))
                       desc (when-let [d (:description option)] (str "  " d))]
                   (layout/line (style/render row-style (str pointer label desc)) width)))
               visible)
         count-line (style/render theme/muted (format "(%d/%d) query:%s" (min (inc active) (max 1 option-count)) option-count (:query state "")))]
     (layout/stack
      (style/render theme/accent (:title state "Suggestions"))
      (when (pos? start) (style/render theme/muted "▲"))
      (if (seq rows)
        (str/join "\n" rows)
        (style/render theme/muted "No suggestions"))
      (when (< end option-count) (style/render theme/muted "▼"))
      count-line))))
