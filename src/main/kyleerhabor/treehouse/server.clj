(ns kyleerhabor.treehouse.server
  (:require
   [clojure.string :as str]
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.query :as eql]
   [kyleerhabor.treehouse.server.response :refer [doctype internal-server-error method-not-allowed not-acceptable]]
   [kyleerhabor.treehouse.ui :as ui]
   [kyleerhabor.treehouse.util :refer [debug?]]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.algorithms.denormalize :refer [db->tree]] 
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.dom-server :as dom]
   [com.fulcrologic.fulcro.server.api-middleware :as s :refer [wrap-transit-params wrap-transit-response]]
   [mount.core :refer [defstate]]
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

(def page-handler
  (constantly
    (let [db (-> (ssr/build-initial-state (comp/get-initial-state ui/Root) ui/Root)
               (assoc :email (:kyleerhabor.treehouse.media/email config)))
          props (db->tree (comp/get-query ui/Root) db db)
          ;; In the Fulcro docs, comp/*app* is bound before rendering. The server is written in .clj, however, and my
          ;; app is in .cljs (since it uses remotes). Hopefully this won't cause issues.
          html (dom/render-to-str (ui/document db props))]
      (-> (str doctype html)
        ;; Fulcro escapes quotes, which is annoying here since I need to embed JavaScript in the code. I'd like to use
        ;; ssr/initial-state->script-tag, but it returns a string, which can't be embedded in the HTML head with
        ;; :dangerouslySetInnerHTML without introducing a non-valid element (e.g. div). The alternative would be
        ;; building raw HTML (i.e. using strings instead of dom/...), which would be worse. If Fulcro had a special type
        ;; for preserving characters (like Hiccup does), this would be simpler.
        (str/replace #"window\.INITIAL_APP_STATE = &quot;(.+)&quot;" (fn [[_ s]]
                                                                       (str "window.INITIAL_APP_STATE = \"" s \")))
        res/response
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
                             :send-server-version? debug?})
  :stop (.stop server))
