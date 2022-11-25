;; Maybe move this to the client folder?
(ns kyleerhabor.treehouse.app
  (:require 
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.networking.http-remote :as http]))

(defonce app (app/fulcro-app {:remotes {:remote (http/fulcro-http-remote {})}}))

(def state ::app/state-atom)

(def mount (partial app/mount! app ui/Root "app"))
