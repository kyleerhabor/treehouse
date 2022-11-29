(ns kyleerhabor.treehouse.server
  (:require
   [clojure.string :as str]
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.query :as eql]
   [kyleerhabor.treehouse.server.response :refer [doctype internal-server-error method-not-allowed not-acceptable]]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.algorithms.denormalize :refer [db->tree]]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.dom-server :as dom]
   [com.fulcrologic.fulcro.server.api-middleware :as s :refer [wrap-transit-params wrap-transit-response]]
   [mount.core :as m :refer [defstate]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.util.mime-type :refer [default-mime-types]]
   [ring.util.response :as res]
   [reitit.ring :as rr]
   [reitit.middleware :as-alias rmw]
   [reitit.ring.middleware.exception :as rrex]))

(defn allowed
  "Returns a comma-separated string of the request methods supported by a request."
  [request]
  (->> (:result (rr/get-match request))
    (filter val)
    (map #(str/upper-case (name (key %))))
    (str/join ", ")))

(defn initial-state [root]
  (ssr/build-initial-state (comp/get-initial-state root) root))

(def page-handler
  (constantly
    (let [root ui/Root
          db (-> (initial-state root)
               (assoc :email (:kyleerhabor.treehouse.media/email config)))
          props (db->tree (comp/get-query root db) db db)
          app (app/fulcro-app {:initial-db db})
          ;; Routing is client-only, which is annoying for this use case. Maybe reitit can help?
          html (binding [comp/*app* app]
                 (dom/render-to-str (ui/ui-document props)))]
      (-> (res/response (str doctype html))
        (res/content-type (get default-mime-types "html"))))))

(defn api-handler [{query :transit-params}]
  (let [r (eql/parse query)]
    (s/generate-response (merge (res/response r) (s/apply-response-augmentations r)))))

(def routes [["/api" {:post {:handler api-handler
                             :middleware [[:transit-params]
                                          [:transit-response]]}}]])

(def exception-middleware (rrex/create-exception-middleware {::rrex/default (constantly internal-server-error)}))

(def default-options-endpoint {:handler (comp res/response allowed)})

(def router (rr/router routes
              {::rr/default-options-endpoint default-options-endpoint
               ::rmw/registry {:transit-params [wrap-transit-params {:malformed-response (res/bad-request nil)}]
                               :transit-response wrap-transit-response
                               :exception exception-middleware}
               :data {:middleware [:exception]}}))

(def default-handler-options
  {:method-not-allowed (comp method-not-allowed allowed)
   :not-acceptable (constantly not-acceptable)})

(def default-routes [["*" {:get page-handler}]])

(def default-router (rr/router default-routes {::rr/default-options-endpoint default-options-endpoint}))

(def default-handler (rr/ring-handler default-router
                       ;; As the default route accepts any route, it should be impossible to return a not found response.
                       (rr/create-default-handler default-handler-options)))

(def handler (rr/ring-handler router
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
                 (rr/create-default-handler (assoc default-handler-options :not-found default-handler)))))

(defstate server
  :start (run-jetty handler {:port (::port config)
                             :join? false
                             :send-server-version? false})
  :stop (.stop server))

(defn -main []
  ;; This may be a little problematic in production since it doesn't naturally stop the states.
  (m/start))
