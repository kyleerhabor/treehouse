(ns kyleerhabor.treehouse.client.app
  (:require
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.rad.application :refer [fulcro-rad-app]]))

(defonce app (fulcro-rad-app {}))

(def mount (partial app/mount! app ui/Root "app"))
