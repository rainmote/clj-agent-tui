(ns clj-agent-tui.fuzzy
  "Small deterministic fuzzy matcher for suggestion lists."
  (:require
   [clojure.string :as str]))

(defn match-indices
  "Return sequential fuzzy match indices for query in candidate, or nil."
  [query candidate]
  (let [q (str/lower-case (or query ""))
        c (str/lower-case (or candidate ""))]
    (loop [remaining (seq q)
           start 0
           acc []]
      (if-not remaining
        acc
        (let [needle (str (first remaining))
              idx (str/index-of c needle start)]
          (when idx
            (recur (next remaining) (inc idx) (conj acc idx))))))))

(defn score
  "Score a query/candidate pair. Lower is better; nil means no match."
  [query candidate]
  (when-let [idxs (match-indices query candidate)]
    (let [span (if (seq idxs) (- (last idxs) (first idxs)) 0)
          prefix-bonus (if (= 0 (or (first idxs) 0)) -10 0)]
      (+ span (count candidate) prefix-bonus))))

(defn matches?
  [query candidate]
  (boolean (score query candidate)))

(defn- strip-leading-trigger
  [s]
  (let [s (or s "")]
    (if (and (seq s) (#{\/ \@} (first s)))
      (subs s 1)
      s)))

(defn- candidate-variants
  [candidate]
  (distinct [(or candidate "") (strip-leading-trigger candidate)]))

(defn- starts-with-ci?
  [query candidate]
  (str/starts-with? (str/lower-case (or candidate ""))
                    (str/lower-case (or query ""))))

(defn- field-match
  [query candidate field-rank]
  (let [variants (candidate-variants candidate)
        best (->> variants
                  (keep (fn [variant]
                          (cond
                            (= (str/lower-case query) (str/lower-case variant))
                            {:rank [0 (count variant) field-rank variant]
                             :score 0
                             :indices (vec (range (count variant)))}

                            (starts-with-ci? query variant)
                            {:rank [1 (count variant) field-rank variant]
                             :score (count variant)
                             :indices (vec (range (count query)))}

                            :else
                            (when-let [s (score query variant)]
                              {:rank [2 s field-rank variant]
                               :score s
                               :indices (match-indices query variant)}))))
                  (sort-by :rank)
                  first)]
    best))

(defn- option-primary-match
  [query option]
  (->> [[(:label option) 0] [(:value option) 1]]
       (keep (fn [[candidate field-rank]]
               (field-match query (str candidate) field-rank)))
       (sort-by :rank)
       first))

(defn- option-description-match
  [query option]
  (when-let [description (:description option)]
    (field-match query (str description) 2)))

(defn filter-options
  "Filter and sort option maps by fuzzy query.

  Labels and values are the primary match surface. Descriptions are only used as
  a fallback when no option matches by label/value, so command searches do not
  show unrelated commands just because their help text contains a query letter."
  [query options]
  (let [query (str/trim (or query ""))]
    (if (str/blank? query)
      (vec options)
      (let [primary (->> options
                         (keep (fn [option]
                                 (when-let [match (option-primary-match query option)]
                                   (assoc option
                                          :match-rank (:rank match)
                                          :match-score (:score match)
                                          :matched-indices (:indices match)))))
                         (sort-by (juxt :match-rank :label))
                         vec)]
        (if (seq primary)
          primary
          (->> options
               (keep (fn [option]
                       (when-let [match (option-description-match query option)]
                         (assoc option
                                :match-rank (:rank match)
                                :match-score (:score match)
                                :matched-indices (:indices match)))))
               (sort-by (juxt :match-rank :label))
               vec))))))
