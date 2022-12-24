(ns kyleerhabor.treehouse.server.query.cache ; Or remote?
  (:require
   [clojure.core.cache.wrapped :as cache]
   [kyleerhabor.treehouse.server.remote.discord :as discord]
   [kyleerhabor.treehouse.server.remote.github :as github]
   [tick.core :as tick]))

;; This model of wrapping requests for caches is unsustainable. A generic remote communications model is likely the best
;; solution, but I don't know how I'd develop such a thing (or if one already exists).

(def media (cache/ttl-cache-factory {} :ttl (tick/millis (tick/new-duration 1 :days))))

(def project-github (cache/ttl-cache-factory {} :ttl (tick/millis (tick/new-duration 6 :hours))))

(defn current-discord-user []
  (cache/lookup-or-miss media :discord (fn [_] (discord/use-token discord/get-current-user))))

(defn current-github-viewer []
  (cache/lookup-or-miss media :github (fn [_] (github/viewer))))

(defn project-github-repo [repo]
  (cache/lookup-or-miss project-github repo github/project-repo))
