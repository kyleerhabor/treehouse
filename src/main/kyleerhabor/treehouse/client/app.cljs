(ns kyleerhabor.treehouse.client.app
  (:require
   #_[kyleerhabor.treehouse.client.query :as eql]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.networking.http-remote :refer [fulcro-http-remote wrap-csrf-token wrap-fulcro-request]]
   #_[com.fulcrologic.fulcro.networking.mock-server-remote :refer [mock-http-server]]))

(def request-middleware
  (-> identity
    (wrap-csrf-token js/fulcro_network_csrf_token)
    wrap-fulcro-request))

(defonce app (app/fulcro-app {:remotes {:remote (fulcro-http-remote {:request-middleware request-middleware})
                                        #_:browser #_(mock-http-server {:parser eql/parse})}}))

(def mount (partial app/mount! app ui/Root "app"))
