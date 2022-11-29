(ns kyleerhabor.treehouse.client
  (:require
   [kyleerhabor.treehouse.client.app :refer [app mount]]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.data-fetch :refer [load!]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as route]
   [com.fulcrologic.rad.routing.history :refer [install-route-history!]]
   [com.fulcrologic.rad.routing.html5-history :refer [new-html5-history restore-route!]]))

(defn init []
  (reset! (::app/state-atom app) (ssr/get-SSR-initial-state))
  (app/set-root! app ui/Root {:initialize-state? true}) 
  (route/initialize! app)
  (install-route-history! app (new-html5-history {:app app}))
  (restore-route! app ui/Home {})
  (load! app :discord ui/Discord)
  (load! app :github ui/Github)
  (mount {:hydrate? true
          :initialize-state? false})
  (println "Hydrated!"))

(defn refresh []
  (mount)
  (comp/refresh-dynamic-queries! app)
  (println "Reloaded!"))
