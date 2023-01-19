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

;; TODO: Implement CORS.
(def handler (rr/ring-handler r/router
               (rr/routes
                 (rr/redirect-trailing-slash-handler)
                 ;; TODO: Serve resources myself (for caching benefits).
                 (rr/create-resource-handler {:path "/"})
                 (rr/create-default-handler (assoc default-handler-options :not-found default-handler)))
               {:middleware [wrap-not-modified wrap-gzip r/exception-middleware]}))

(defstate server
  :start (let [port (::port config)
               server (run-jetty handler {:port port
                                          :join? false
                                          :send-server-version? false})]
           (log/info "Server running on port" port)
           server)
  :stop (.stop server))

(defn run []
  (m/start))

(defn stop []
  (m/stop))

(defn -main []
  (run)
  (.addShutdownHook (Runtime/getRuntime)
    (Thread.
      (fn []
        (stop)
        (shutdown-agents)))))
