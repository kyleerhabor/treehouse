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
   [com.fulcrologic.fulcro.algorithms.do-not-use :refer [base64-encode]] ; Please...
   [com.fulcrologic.fulcro.algorithms.transit :as transit]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.dom-server :as dom]
   [com.fulcrologic.fulcro.server.api-middleware :as s :refer [wrap-transit-params wrap-transit-response]]
   [com.fulcrologic.fulcro-css.css-injection :refer [style-element]]
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

(defn page [db props]
  (dom/html
    (dom/head
      (dom/meta {:charset "UTF-8"})
      (dom/meta {:name "viewport"
                 :content "width=device-width, initial-scale=1"})
      (dom/title "Kyle Erhabor") 
      (dom/script
        (str "window.INITIAL_APP_STATE = \"" (base64-encode (transit/transit-clj->str db)) "\""))
      (style-element {:component ui/Root
                      :garden-flags {:pretty-print? debug?}}))
    (dom/body
      (dom/div :#app
        (ui/ui-root props))
      (dom/script {:src "/assets/main/js/compiled/main.js"}
        "kyleerhabor.treehouse.client.init_ssr();"))))

(def page-handler
  (constantly
    (let [db (-> (ssr/build-initial-state (comp/get-initial-state ui/Root) ui/Root)
               (assoc :email (:kyleerhabor.treehouse.media/email config)))
          props (db->tree (comp/get-query ui/Root) db db)
          html (dom/render-to-str (page db props))]
      (-> (str doctype html)
      ;; Fulcro escapes quotes, so I'm unescaping a specific instance here.
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
