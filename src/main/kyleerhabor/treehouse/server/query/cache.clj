(ns kyleerhabor.treehouse.server.query.cache ; Or remote?
  (:require
   [clojure.core.cache.wrapped :as cache]
   [kyleerhabor.treehouse.server.remote.discord :as discord]
   [kyleerhabor.treehouse.server.remote.github :as github]
   [tick.core :as tick]))

(def media (cache/ttl-cache-factory {} :ttl (tick/millis (tick/new-duration 1 :days))))

(def project-github (cache/ttl-cache-factory {} :ttl (tick/millis (tick/new-duration 6 :hours))))

(defn current-discord-user []
  (cache/lookup-or-miss media :discord (fn [_] (discord/request :get-current-user {:version discord/api-version}))))

(defn current-github-viewer []
  (cache/lookup-or-miss media :github (fn [_] (github/viewer))))

(defn project-github-repo [repo]
  (cache/lookup-or-miss project-github repo github/project-repo))
