(ns kyleerhabor.treehouse.server.database
  (:require
    [kyleerhabor.treehouse.server.config :refer [config]]
    [datalevin.core :as d]))

(def schema {})

(def conn (d/get-conn (::dir config) schema))

(defn latest-datom [datoms]
  (apply max-key d/datom-e datoms))
