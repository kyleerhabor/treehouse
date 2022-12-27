(ns kyleerhabor.treehouse.client
  (:require
   [kyleerhabor.treehouse.client.app :refer [app mount]]
   [kyleerhabor.treehouse.client.route :refer [router]]
   [kyleerhabor.treehouse.mutation :as mut]
   [kyleerhabor.treehouse.route.ui :as route+]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [reitit.frontend.easy :as rfe]))

(defn init []
  (reset! (::app/state-atom app) (ssr/get-SSR-initial-state))
  (app/set-root! app ui/Root {:initialize-state? true})
  ;; I'd like to push initial routing to the server.
  (rfe/start! router (fn [match _]
                       (if-let [handler (:handler (:data match))]
                         (handler match))
                       (comp/transact! app [(mut/route (some-> match route+/props))])) {:use-fragment false})
  (mount {:hydrate? true
          :initialize-state? false}))

(defn refresh []
  (mount)
  (comp/refresh-dynamic-queries! app))
