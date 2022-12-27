(ns kyleerhabor.treehouse.route
  (:require
   [reitit.core :as r]
   [reitit.coercion :as rc]
   [reitit.coercion.malli :refer [coercion]]))

(def routes [["/" :home]
             ["/api" :api]
             ["/articles" :articles]
             ["/articles/:id" {:name :article
                               :coercion coercion
                               :parameters {:path [:map
                                                   [:id :keyword]]}}]
             ["/projects" :projects]
             ["/projects/:id" {:name :project
                               :coercion coercion
                               :parameters {:path [:map
                                                   [:id :keyword]]}}]])

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
