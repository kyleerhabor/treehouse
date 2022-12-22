(ns kyleerhabor.treehouse.server.route
  (:require
   [clojure.string :as str]
   [kyleerhabor.treehouse.model.media :as-alias media]
   [kyleerhabor.treehouse.mutation :as mut]
   [kyleerhabor.treehouse.route :as route]
   [kyleerhabor.treehouse.route.ui :as route+]
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.query :as eql]
   [kyleerhabor.treehouse.server.response :refer [doctype forbidden]]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.algorithms.denormalize :refer [db->tree]]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.dom-server :as dom]
   [com.fulcrologic.fulcro.server.api-middleware :as s :refer [wrap-transit-params wrap-transit-response]]
   [reitit.core :as r]
   [reitit.middleware :as-alias rmw]
   [reitit.ring :as rr]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.exception :as rrex]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.x-headers :refer [wrap-content-type-options wrap-frame-options]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.mime-type :refer [default-mime-types]]
   [ring.util.response :as res]))

(defn allowed
  "Returns a comma-separated string of the request methods supported by a request."
  [request]
  (->> (:result (rr/get-match request))
    (filter val)
    (map #(str/upper-case (name (key %))))
    (str/join ", ")))

(defn initial-db [root]
  (ssr/build-initial-state (comp/get-initial-state root) root))

(def root ui/Root)

(def root-initial-db (initial-db root))

;; This could potentially be simpler, since config and match don't *really* need to be *in* there.
(defn current-db [db match]
  (cond-> (assoc db :email (::media/email config))
    match (mut/route* (route+/props match))))

(defn page-handler [request]
  (let [db (current-db root-initial-db (rr/get-match request))
        props (db->tree (comp/get-query root db) db db)
        app (app/fulcro-app {:initial-db db})
        html (binding [comp/*app* app]
               (dom/render-to-str (ui/document db props {:anti-forgery-token (:anti-forgery-token request)})))]
    (-> (res/response (str doctype html))
      (res/content-type (get default-mime-types "html")))))

(defn api-handler [{query :transit-params}]
  (let [r (eql/parse query)]
    (s/generate-response (merge (res/response r) (s/apply-response-augmentations r)))))

(def routes {:home {:middleware [[:session] [:anti-forgery]]
                    :get {:handler page-handler}}
             ;; Should anti-forgery be used here?
             :api {:post {:handler api-handler
                          :middleware [[:transit-params]
                                       [:transit-response]]}}
             :projects {:middleware [[:session] [:anti-forgery]]
                        :get {:handler page-handler}}
             :project {:middleware [[:session] [:anti-forgery]]
                       :get {:handler page-handler}}})

(def exception-middleware (rrex/create-exception-middleware {::rrex/default #(page-handler %2)}))

(def router (rr/router (r/routes route+/router)
              (merge (dissoc (r/options route+/router) :compile)
                {:data {:middleware [rrc/coerce-request-middleware
                                     [wrap-content-type-options :nosniff]
                                     [wrap-frame-options :deny] ; Content-Security-Policy could replace this.
                                     exception-middleware]}
                 :expand (route/merge-expand routes)
                 ::rmw/registry {:anti-forgery [wrap-anti-forgery {:error-response forbidden}]
                                 :session wrap-session
                                 :transit-params [wrap-transit-params {:malformed-response (res/bad-request nil)}]
                                 :transit-response wrap-transit-response}
                 ::rr/default-options-endpoint {:handler (comp res/response allowed)}})))
