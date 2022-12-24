(ns kyleerhabor.treehouse.server.config
  (:require
   [kyleerhabor.treehouse.server :as-alias server]
   [kyleerhabor.treehouse.server.database :as-alias db]
   [kyleerhabor.treehouse.server.remote.discord :as-alias discord]
   [kyleerhabor.treehouse.server.remote.github :as-alias github]
   [cprop.core :refer [load-config]]
   [cprop.source :refer [from-env]]
   [malli.core :as m]
   [malli.error :as me]
   [malli.util :as mu]
   [mount.core :refer [defstate]]))

(def Config
  [:map
   [::email :string]
   [::db/dir :string]
   [::discord/client-id :string]
   [::discord/client-secret :string]
   [::discord/redirect :string]
   [::github/token :string]
   ;; Could use more.
   [::server/port pos-int?]])

(def exp (m/explainer (mu/optional-keys Config)))

(defstate config
  :start (let [config (load-config :merge [(from-env)])]
           (if-let [exp (exp config)]
             (throw (ex-info (str "Invalid config: " (me/humanize exp)) exp))
             config)))
