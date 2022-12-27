(ns kyleerhabor.treehouse.server
  (:require
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.response :as res]
   [kyleerhabor.treehouse.server.route :as r]
   [mount.core :as m :refer [defstate]]
   [reitit.ring :as rr]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.not-modified :refer [wrap-not-modified]]
   [taoensso.timbre :as log])
  (:gen-class))

(def method-not-allowed (comp res/method-not-allowed r/allowed))

(def default-handler-options {:method-not-allowed method-not-allowed})

(def default-handler (rr/ring-handler r/default-router
                       (rr/create-default-handler default-handler-options)))

(def handler (rr/ring-handler r/router
               (rr/routes
                 (rr/redirect-trailing-slash-handler)
                 ;; TODO: Serve resources myself (for caching benefits).
                 (rr/create-resource-handler {:path "/"})
                 ;; TODO: Figure out what to do with :not-acceptable.
                 (rr/create-default-handler (assoc default-handler-options :not-found default-handler)))
               {:middleware [wrap-not-modified
                             wrap-gzip
                             r/exception-middleware]}))

(defstate server
  :start (let [port (::port config)]
           (run-jetty handler {:port port
                               :join? false
                               :send-server-version? false})
           (log/info "Server running on port" port))
  :stop (.stop server))

(defn -main []
  ;; This may be a little problematic in production since it doesn't naturally stop the states.
  (m/start))
