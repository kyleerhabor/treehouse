(ns kyleerhabor.treehouse.server.database
  (:require
    [kyleerhabor.treehouse.model.project :as-alias project]
    [kyleerhabor.treehouse.server.config :refer [config]]
    [datalevin.core :as d]))

(def unique {:db/unique :db.unique/identity})

(def schema {::project/id unique})

(def conn (d/get-conn (::dir config) schema))

(defn latest-datom [datoms]
  (apply max-key d/datom-e datoms))

(comment
  (def conn (d/create-conn (::dir config) schema)))
