(ns clj-agent-tui.theme
  "Agent Dark-inspired style tokens for clj-agent-tui."
  (:require
   [charm.style.border :as border]
   [charm.style.core :as style]))

(def agent-dark
  {:background "#0b0e14"
   :foreground "#bfbdb6"
   :light-blue "#59C2FF"
   :accent-blue "#39BAE6"
   :accent-purple "#D2A6FF"
   :accent-cyan "#95E6CB"
   :accent-green "#AAD94C"
   :accent-yellow "#FFD700"
   :accent-red "#F26D78"
   :comment "#646A71"
   :gray "#3D4149"})

(defn color [k]
  (style/hex (get agent-dark k)))

(def title (style/style :fg (color :accent-yellow) :bold true))
(def logo (style/style :fg (color :accent-purple) :bold true))
(def accent (style/style :fg (color :accent-cyan) :bold true))
(def primary (style/style :fg (color :foreground)))
(def muted (style/style :fg (color :comment)))
(def subtle (style/style :fg (color :gray)))
(def success (style/style :fg (color :accent-green)))
(def warning (style/style :fg (color :accent-yellow)))
(def danger (style/style :fg (color :accent-red)))
(def selected (style/style :fg (color :background) :bg (color :accent-cyan) :bold true))
(def panel (style/style :border border/rounded :border-fg (color :gray) :padding [0 1]))
(def active-panel (style/style :border border/rounded :border-fg (color :accent-cyan) :padding [0 1]))
