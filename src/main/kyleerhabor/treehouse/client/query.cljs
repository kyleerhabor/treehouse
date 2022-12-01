(ns kyleerhabor.treehouse.client.query
  (:require
   [kyleerhabor.treehouse.route :refer [router]]
   [kyleerhabor.treehouse.model.route :as-alias route]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.history :as rfh]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc :refer [defmutation]]))

(rfe/start! router (constantly nil) {:use-fragment false
                                     :ignore-anchor-click? (constantly true)})

(defmutation route [{:keys [history]} {::route/keys [name path query]}]
  {::pc/sym 'kyleerhabor.treehouse.mutation/route}
  (rfh/push-state history name path query))

(def registry [route])

(def parser (p/async-parser {::p/env {::p/reader [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                                      ::pc/mutation-join-globals [:tempids]}
                             ::p/mutate pc/mutate
                             ::p/plugins [(pc/connect-plugin {::pc/register registry})
                                          p/error-handler-plugin
                                          p/elide-special-outputs-plugin]}))

(defn parse [query]
  (parser {:history @rfe/history} query))
