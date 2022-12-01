(ns kyleerhabor.treehouse.client
  (:require
   [kyleerhabor.treehouse.client.app :refer [app mount]]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.data-fetch :refer [load!]]))

(defn init []
  (reset! (::app/state-atom app) (ssr/get-SSR-initial-state))
  (app/set-root! app ui/Root {:initialize-state? true}) 
  (load! app :discord ui/DiscordUser)
  (load! app :github ui/GithubUser)
  (mount {:hydrate? true
          :initialize-state? false}))

(defn refresh []
  (mount)
  (comp/refresh-dynamic-queries! app))
