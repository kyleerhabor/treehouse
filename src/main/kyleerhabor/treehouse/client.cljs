(ns kyleerhabor.treehouse.client
  (:require
   [kyleerhabor.treehouse.client.app :refer [app mount]]
   [kyleerhabor.treehouse.client.route :refer [router]]
   [kyleerhabor.treehouse.mutation :as mut]
   [kyleerhabor.treehouse.route.ui :as route+]
   [kyleerhabor.treehouse.ui :as ui]
   [kyleerhabor.treehouse.util :refer [after noop]]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [reitit.frontend.easy :as rfe]))

(defn init []
  (reset! (::app/state-atom app) (ssr/get-SSR-initial-state))
  ;; Setting :initialize-state? to true causes the UI router to route to the not found page, then immediately route to
  ;; the correct page afterwards, which is not what I want.
  (app/set-root! app ui/Root {})
  (rfe/start! router (after noop (fn [match _]
                                   (if-let [handler (:handler (:data match))]
                                     (handler match))
                                   (comp/transact! app [(mut/route (some-> match route+/props))]))) {:use-fragment false})
  (mount {:hydrate? true
          :initialize-state? false}))

(defn refresh []
  (mount)
  (comp/refresh-dynamic-queries! app))
