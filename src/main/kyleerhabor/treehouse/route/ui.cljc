(ns kyleerhabor.treehouse.route.ui
  (:require
   [kyleerhabor.treehouse.model.project :as-alias project]
   [kyleerhabor.treehouse.route :as route]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.components :as comp]
   [reitit.core :as r]))

(defn ident [props]
  (comp/get-ident ui/Router props))

(defn props [{{{:keys [props]} :ui} :data
              :as match}]
  (props match))

(def routes {:home {:ui {:props (constantly {::ui/id ::ui/Home})}}
             :projects {:ui {:props (constantly {::ui/id ::ui/Projects})}}
             :project {:ui {:props (fn [{{{:keys [id]} :path} :parameters}]
                                     {::project/id id})}}})

(def router (r/router route/routes
              (merge route/options
                {:expand (route/merge-expand routes)})))
