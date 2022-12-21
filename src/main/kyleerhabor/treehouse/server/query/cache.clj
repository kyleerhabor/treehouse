(ns kyleerhabor.treehouse.server.query.cache
  (:require
   [clojure.core.cache.wrapped :as cache]
   [kyleerhabor.treehouse.server.remote.discord :as discord]
   [kyleerhabor.treehouse.server.remote.github :as github]
   [tick.core :as tick]))

;; This model of wrapping requests for caches is unsustainable. A generic remote communications model is likely the best
;; solution, but I don't know how I'd develop such a thing (or if one already exists).

(def media (cache/ttl-cache-factory {:discord (discord/get-current-user (discord/current-access-token))
                                     :github (github/viewer)}
             :ttl (tick/millis (tick/new-duration 1 :days))))

(def project-github (cache/ttl-cache-factory {} :ttl (tick/millis (tick/new-duration 1 :days))))

(defn current-discord-user []
  (cache/lookup-or-miss media :discord (fn [_] (discord/get-current-user (discord/current-access-token)))))

(defn current-github-user []
  (cache/lookup-or-miss media :github (fn [_] (github/viewer))))

(defn project-github-repo [repo]
  (cache/lookup-or-miss project-github repo github/project-repo))
