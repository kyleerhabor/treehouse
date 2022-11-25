(ns kyleerhabor.treehouse.client
  (:require
   [kyleerhabor.treehouse.app :as app :refer [app mount]]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.data-fetch :refer [load!]]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]))

(defn ^:export init []
  (reset! (app/state app) (ssr/get-SSR-initial-state))
  (mount {:hydrate? true})
  (load! app :discord ui/Discord)
  (load! app :github ui/Github)
  (println "Hydrated!"))

(defn refresh []
  (mount)
  (comp/refresh-dynamic-queries! app)
  (println "Reloaded!"))
