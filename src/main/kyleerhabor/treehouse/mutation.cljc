(ns kyleerhabor.treehouse.mutation
  (:require [com.fulcrologic.fulcro.mutations :as mut :refer [defmutation]]))

;; TODO: Have this serve only as a convenience namespace (i.e. only redeclare symbols)
(defmutation route [route]
  (action [{:keys [state]}]
    (swap! state assoc :route route)))
