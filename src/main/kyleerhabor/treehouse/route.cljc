;; NOTE: This namespace will likely be moved under the ui one.
(ns kyleerhabor.treehouse.route
  (:require
   [kyleerhabor.treehouse.schema :as s]
   [kyleerhabor.treehouse.model.project :as-alias project]
   [kyleerhabor.treehouse.ui :as-alias ui]
   [reitit.core :as r]
   [reitit.coercion :as rc]
   [reitit.coercion.malli :refer [coercion]]))

;; It is likely that this namespace will run into circular dependency issues in the future due to the UI's need of the
;; router to create hrefs.

(defn props [{{{:keys [props]} :ui} :data
              :as match}]
  (props match))

(def routes [["/" {:name :home
                   :ui {:props (constantly {::ui/id ::ui/Home})}}]
             ["/api" :api]
             ["/projects" {:name :projects
                           :ui {:props (constantly {::ui/id ::ui/Projects})}}]
             ["/projects/:id" {:name :project
                               :coercion coercion
                               :parameters {:path [:map
                                                   [:id s/ID]]}
                               :ui {:props (fn [{{{:keys [id]} :path} :parameters}]
                                             {::project/id id})}}]])

(defn merge-expand [registry]
  (fn [data opts]
    (let [expand #(r/expand % opts)
          data* (expand data)]
      (if-let [name (:name data*)]
        (merge-with into data* (expand (name registry)))
        ;; Entry in map has no name for some reason, just ignore.
        data*))))

(def options {:compile rc/compile-request-coercers})

(def router (r/router routes options))

(defn href
  ([router name] (href router name nil))
  ([router name path] (href router name path nil))
  ([router name path query]
   (r/match->path (r/match-by-name router name path) query)))

(def href+ (partial href router))
