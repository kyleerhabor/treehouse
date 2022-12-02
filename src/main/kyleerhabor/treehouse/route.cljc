(ns kyleerhabor.treehouse.route
  (:require
   [kyleerhabor.treehouse.model.route :as-alias route]
   [reitit.core :as r]))

(defn route [match]
  (let [{{:keys [name]} :data
         :keys [query-params path-params]} match]
    (cond-> {::route/name name
             ::route/path path-params}
      query-params (assoc ::route/query query-params))))

(def routes [["/" :home]
             ["/api" :api]
             ["/projects" :projects]])

(def router (r/router routes))

(defn href
  ([router name] (href router name nil))
  ([router name path] (href router name path nil))
  ([router name path query]
   (r/match->path (r/match-by-name router name path) query)))

(def href+ (partial href router))
