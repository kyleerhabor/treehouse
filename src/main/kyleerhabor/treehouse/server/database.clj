(ns kyleerhabor.treehouse.server.database
  (:require
    [kyleerhabor.treehouse.server.config :refer [config]]
    [datalevin.core :as d]
    [mount.core :refer [defstate]]))

(def unique {:db/unique :db.unique/identity})

(def schema {})

(defstate conn
  :start (d/get-conn (::dir config) schema))

(defn latest-datom [datoms]
  (apply max-key d/datom-e datoms))
