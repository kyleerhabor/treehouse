(ns kyleerhabor.treehouse.server.config
  (:require
    [com.fulcrologic.fulcro.server.config :refer [load-config!]]))

(def config (load-config! {:config-path "config/config.edn"}))
