(ns kyleerhabor.treehouse.route.common
  (:require
   [kyleerhabor.treehouse.model.route :as-alias route]))

;; Needs to be called on the client and server.
(defn route [match]
  (let [{{:keys [name]} :data
         :keys [query-params path-params]} match]
    (cond-> {::route/name name
             ::route/path path-params}
      query-params (assoc ::route/query query-params))))
