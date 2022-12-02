(ns kyleerhabor.treehouse.server.route
  (:require
   [clojure.string :as str]
   [kyleerhabor.treehouse.model.media :as-alias media]
   [kyleerhabor.treehouse.route :as route]
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.query :as eql]
   [kyleerhabor.treehouse.server.response :refer [doctype internal-server-error]]
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
   [reitit.ring.middleware.exception :as rrex]
   [ring.util.mime-type :refer [default-mime-types]]
   [ring.util.response :as res]))

(defn allowed
  "Returns a comma-separated string of the request methods supported by a request."
  [request]
  (->> (:result (rr/get-match request))
    (filter val)
    (map #(str/upper-case (name (key %))))
    (str/join ", ")))

(defn merge-expand [registry]
  (fn [data opts]
    (let [expand #(r/expand % opts)
          data* (expand data)]
      (if-let [name (:name data*)]
        (merge data* (expand (name registry))) ; Maybe do a deep merge?
        ;; Entry in map has no name for some reason, just don't bother.
        data*))))

(defn initial-state [root]
  (ssr/build-initial-state (comp/get-initial-state root) root))

(def root ui/Root)

(def root-initial-state (initial-state root))

(defn current-state [db match]
  (assoc db
    ::media/email (::media/email config)
    :route (route/route match)))

;; This could be faster as the router already knows what page it's on, making the need to compute the request
;; unnecessary and potentially making the page handler response instant.
(defn page-handler [request]
  (let [db (current-state root-initial-state (rr/get-match request))
        props (db->tree (comp/get-query root db) db db)
        app (app/fulcro-app {:initial-db db})
        html (binding [comp/*app* app]
               (dom/render-to-str (ui/ui-document props)))]
    (-> (res/response (str doctype html))
      (res/content-type (get default-mime-types "html")))))

(defn api-handler [{query :transit-params}]
  (let [r (eql/parse query)]
    (s/generate-response (merge (res/response r) (s/apply-response-augmentations r)))))

(def routes {:home {:get page-handler}
             :api {:post {:handler api-handler
                          :middleware [[:transit-params]
                                       [:transit-response]]}}
             :projects {:get page-handler}})

(def exception-middleware (rrex/create-exception-middleware {::rrex/default (constantly internal-server-error)}))

(def router (rr/router route/routes
              {:expand (merge-expand routes)
               ::rr/default-options-endpoint {:handler (comp res/response allowed)}
               ::rmw/registry {:exception exception-middleware
                               :transit-params [wrap-transit-params {:malformed-response (res/bad-request nil)}]
                               :transit-response wrap-transit-response}
               :data {:middleware [:exception]}}))
