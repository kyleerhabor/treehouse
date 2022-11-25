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

(defn allowed [request]
  (->> (:result (rr/get-match request))
    (filter val)
    (map #(str/upper-case (name (key %))))
    (str/join ", ")))

(def page-handler
  (constantly
    (let [db (-> (ssr/build-initial-state (comp/get-initial-state ui/Root) ui/Root)
               (assoc :email (:kyleerhabor.treehouse.media/email config)))
          props (db->tree (comp/get-query ui/Root) db db)
          html (dom/render-to-str (ui/ui-document props {:db db}))]
      html
      (-> (str doctype html)
        ;; Fulcro escapes quotes, which is annoying here since I need to embed JavaScript in the code. I'd like to use
        ;; ssr/initial-state->script-tag, but that returns a string, and I can't embed the HTML in the head with
        ;; :dangerouslySetInnerHTML without introducing a non-valid element (e.g. div) in the head. The alternative
        ;; would be building raw HTML (i.e. using strings instead of dom/...), which would be worse. If Fulcro had a
        ;; special type for strings that shouldn't be unwrapped (like Hiccup does), this would be simpler.
        (str/replace #"window\.INITIAL_APP_STATE = &quot;(.+)&quot;" (fn [[_ s]]
                                                                       (str "window.INITIAL_APP_STATE = \"" s \")))
        res/response
        (res/content-type (get default-mime-types "html"))))))

(defn api-handler [{query :transit-params}]
  (let [r (eql/parse query)]
    (s/generate-response (merge (res/response r) (s/apply-response-augmentations r)))))

(def routes [["/" {:get page-handler}]
             ["/api" {:post {:handler api-handler
                             :middleware [[:transit-params]
                                          [:transit-response]]}}]])

(def exception-middleware (rrex/create-exception-middleware {::rrex/default (constantly internal-server-error)}))

(def router (rr/router routes
              {::rr/default-options-endpoint {:handler (comp res/response allowed)}
               ::rmw/registry {:transit-params [wrap-transit-params {:malformed-response (res/bad-request nil)}]
                               :transit-response wrap-transit-response
                               :exception exception-middleware}
               :data {:middleware [:exception]}}))

(def handler (rr/ring-handler router
               (rr/routes
                 (rr/create-resource-handler {:path "/"})
                 (rr/create-default-handler {:not-found (constantly (res/not-found nil))
                                             :method-not-allowed (comp method-not-allowed allowed)
                                             :not-acceptable (constantly not-acceptable)}))))

(defstate server
  :start (run-jetty handler {:port (::port config)
                             :join? false
                             :send-server-version? debug?})
  :stop (.stop server))
