(ns kyleerhabor.treehouse.client.app
  (:require
   [kyleerhabor.treehouse.client.query :as eql]
   [kyleerhabor.treehouse.ui :as ui]
   [edn-query-language.core :refer [ast->query]]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.algorithms.tx-processing :as-alias tx]
   [com.fulcrologic.fulcro.networking.http-remote :refer [fulcro-http-remote]]))

(defonce app (app/fulcro-app {:remotes {:remote (fulcro-http-remote {})
                                        :browser {:transmit! (fn [_ {handler ::tx/result-handler
                                                                     ::tx/keys [ast]
                                                                     :as tx}]
                                                               (handler {:body (eql/parse (ast->query ast))
                                                                         :original-transaction tx
                                                                         :status-code 200}))}}}))

(def mount (partial app/mount! app ui/Root "app"))
