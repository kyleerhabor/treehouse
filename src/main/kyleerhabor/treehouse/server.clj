(ns kyleerhabor.treehouse.server
  (:require
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.response :refer [method-not-allowed]]
   [kyleerhabor.treehouse.server.route :as r]
   [mount.core :as m :refer [defstate]]
   [reitit.ring :as rr]
   [ring.adapter.jetty :refer [run-jetty]]))

(def handler (rr/ring-handler r/router
               (rr/routes
                 (rr/redirect-trailing-slash-handler)
                 (rr/create-resource-handler {:path "/"})
                 (rr/create-default-handler {:not-found r/page-handler
                                             :method-not-allowed (comp method-not-allowed r/allowed)
                                             :not-acceptable r/page-handler}))))

(defstate server
  :start (run-jetty handler {:port (::port config)
                             :join? false
                             :send-server-version? false})
  :stop (.stop server))

(defn -main []
  ;; This may be a little problematic in production since it doesn't naturally stop the states.
  (m/start))
