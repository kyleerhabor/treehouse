;; TODO: Transform this namespace into one for convenience via `declare-mutation`.
(ns kyleerhabor.treehouse.mutation
  (:require
   [kyleerhabor.treehouse.route.ui :as route+]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.algorithms.data-targeting :as target]
   [com.fulcrologic.fulcro.mutations :refer [defmutation]]))

(defn route* [db route]
  (let [ident (route+/ident route)]
    (-> db
      (assoc-in ident route)
      (target/integrate-ident* ident :replace [::ui/app :route]))))

(defn remove-route* [db]
  (update db ::ui/app dissoc :route))

(defmutation route [route] ; Maybe wrap `route` in another map for options? (e.g. {:route ...})
  (action [{:keys [state]}]
    (if route
      (swap! state route* route)
      (swap! state remove-route*))))
