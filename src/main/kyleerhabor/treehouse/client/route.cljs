(ns kyleerhabor.treehouse.client.route
  (:require
   [kyleerhabor.treehouse.client.app :refer [app]]
   [kyleerhabor.treehouse.route :as route]
   [kyleerhabor.treehouse.route.ui :as route+]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.data-fetch :refer [load!]]
   [com.fulcrologic.fulcro.components :as comp]
   [reitit.core :as r]
   [reitit.frontend :as rf]))

(defn project [match]
  (load! app (comp/get-ident ui/Project (route+/props match)) ui/Project))

(def routes {:project project})

(def router (rf/router (r/routes route+/router)
              (merge (r/options route+/router)
                {:expand (route/merge-expand routes)})))
