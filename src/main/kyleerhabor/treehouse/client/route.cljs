(ns kyleerhabor.treehouse.client.route
  (:require
   [kyleerhabor.treehouse.client.app :refer [app]]
   [kyleerhabor.treehouse.route :as route]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.data-fetch :refer [load!]]
   [com.fulcrologic.fulcro.components :as comp]
   [reitit.frontend :as rf]))

(defn project [match]
  (load! app (comp/get-ident ui/Project (route/props match)) ui/Project))

(def routes {:project project})

(def router (rf/router route/routes
              (merge route/options
                {:expand (route/merge-expand routes)})))
