(ns kyleerhabor.treehouse.server.handler
  (:require
   [kyleerhabor.treehouse.model :as-alias model]
   [kyleerhabor.treehouse.model.media :as-alias media]
   [kyleerhabor.treehouse.route.common :refer [route]]
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.query :as eql]
   [kyleerhabor.treehouse.server.response :refer [doctype]]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.algorithms.denormalize :refer [db->tree]]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.dom-server :as dom]
   [com.fulcrologic.fulcro.server.api-middleware :as s]
   [ring.util.mime-type :refer [default-mime-types]]
   [ring.util.response :as res]
   [reitit.ring :as rr]))

(defn initial-state [root]
  (ssr/build-initial-state (comp/get-initial-state root) root))

(defn current-state [db match]
  (assoc db
    ::media/email (::media/email config)
    ::model/route (route match)))

(defn page-handler [request]
  (let [root ui/Root
        db (current-state (initial-state root) (rr/get-match request))
        props (db->tree (comp/get-query root db) db db)
        app (app/fulcro-app {:initial-db db})
        html (binding [comp/*app* app]
               (dom/render-to-str (ui/ui-document props {:token (:anti-forgery-token request)})))]
    (-> (res/response (str doctype html))
      (res/content-type (get default-mime-types "html")))))

(defn api-handler [{query :transit-params}]
  (let [r (eql/parse query)]
    (s/generate-response (merge (res/response r) (s/apply-response-augmentations r)))))
