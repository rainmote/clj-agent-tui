(ns clj-agent-tui.layout
  "Fixed-size layout helpers for agent-code-style terminal shells."
  (:require
   [charm.ansi.width :as width]
   [clojure.string :as str]))

(defn split-lines
  "Split text into terminal lines, preserving an empty line for nil/empty input."
  [text]
  (if (or (nil? text) (empty? text))
    [""]
    (str/split (str/replace (str text) "\r\n" "\n") #"\n" -1)))

(defn line
  "Fit one display line to width by truncating at the tail and right-padding."
  [text width]
  (let [width (max 0 (long width))]
    (-> (or text "")
        (width/truncate width :tail "…")
        (width/pad-right width))))

(defn block
  "Fit text into a fixed width/height block.

  Options:
  - `:from :top|:bottom` controls which lines survive vertical overflow."
  [text width height & {:keys [from] :or {from :top}}]
  (let [width (max 0 (long width))
        height (max 0 (long height))]
    (if (or (zero? width) (zero? height))
      ""
      (let [lines (vec (split-lines text))
            clipped (if (> (count lines) height)
                      (case from
                        :bottom (subvec lines (- (count lines) height))
                        :top (subvec lines 0 height))
                      lines)
            padded (concat clipped (repeat ""))]
        (str/join "\n" (map #(line % width) (take height padded)))))))

(defn stack
  "Join vertical blocks, skipping nil parts."
  [& parts]
  (->> parts (remove nil?) (str/join "\n")))

(defn columns
  "Join fixed-size columns into a block of `height` rows.

  Each column is a map with `:content`, `:width`, and optional `:from`."
  [cols height]
  (let [height (max 0 (long height))
        prepared (mapv (fn [{:keys [content width from]}]
                         {:lines (split-lines (block content width height :from (or from :top)))})
                       cols)]
    (str/join "\n"
              (for [row (range height)]
                (apply str (map (fn [{:keys [lines]}] (nth lines row "")) prepared))))))

(defn rule
  "Return a horizontal rule of `width` columns."
  ([width] (rule width "─"))
  ([width ch]
   (apply str (repeat (max 0 (long width)) ch))))
