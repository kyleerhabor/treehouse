(ns kyleerhabor.treehouse.client.app
  (:require
   [kyleerhabor.treehouse.client.query :as eql]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.networking.http-remote :refer [fulcro-http-remote]]
   [com.fulcrologic.fulcro.networking.mock-server-remote :refer [mock-http-server]]))

(defonce app (app/fulcro-app {:remotes {:remote (fulcro-http-remote {})
                                        :browser (mock-http-server {:parser eql/parse})}}))

(def mount (partial app/mount! app ui/Root "app"))
