(ns kyleerhabor.treehouse.server
  (:require
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.response :as res]
   [kyleerhabor.treehouse.server.route :as r]
   [mount.core :as m :refer [defstate]]
   [reitit.ring :as rr]
   [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(def method-not-allowed (comp res/method-not-allowed r/allowed))

(def default-handler-options {:method-not-allowed method-not-allowed})

(def default-handler (rr/ring-handler r/default-router
                       (rr/create-default-handler default-handler-options)))

(def handler (rr/ring-handler r/router
               (rr/routes
                 (rr/redirect-trailing-slash-handler)
                 (rr/create-resource-handler {:path "/"})
                 ;; TODO: Figure out what to do with :not-acceptable.
                 (rr/create-default-handler (assoc default-handler-options :not-found default-handler)))))

(defstate server
  :start (run-jetty handler {:port (::port config)
                             :join? false
                             :send-server-version? false})
  :stop (.stop server))

(defn -main []
  ;; This may be a little problematic in production since it doesn't naturally stop the states.
  (m/start))
