(ns kyleerhabor.treehouse.server
  (:require
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.response :refer [method-not-allowed not-acceptable]]
   [kyleerhabor.treehouse.server.route :as r]
   [mount.core :as m :refer [defstate]]
   [reitit.ring :as rr]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.util.response :as res]))

(def handler (rr/ring-handler r/router
               (rr/routes
                 ;; Shadow produces a main.js file which is meant to be consumed by the user when loading the page. In
                 ;; development, however, it also produces a cljs-runtime folder, which is not required for release mode.
                 ;; As such, in production, users should not request resources from the cljs-runtime folder. The server,
                 ;; however, cannot tell whether or not it is running in development or release mode. For that reason,
                 ;; the server should make no attempt to filter which files from the resources/public folder should be
                 ;; presented. Instead, the folder should be allowed to be populated by shadow in development mode, but,
                 ;; when running on, say, a dedicated server, the cljs-runtime folder should be excluded from the file
                 ;; system.
                 ;;
                 ;; Another solution may be to purge the cljs-runtime folder when compiling under release mode, though
                 ;; this would be slightly more complicated and annoying when switching back to development mode.
                 (rr/create-resource-handler {:path "/"})
                 (rr/create-default-handler {:not-found (constantly (res/not-found nil))
                                             :method-not-allowed (comp method-not-allowed r/allowed)
                                             :not-acceptable (constantly not-acceptable)}))))

(defstate server
  :start (run-jetty handler {:port (::port config)
                             :join? false
                             :send-server-version? false})
  :stop (.stop server))

(defn -main []
  ;; This may be a little problematic in production since it doesn't naturally stop the states.
  (m/start))
