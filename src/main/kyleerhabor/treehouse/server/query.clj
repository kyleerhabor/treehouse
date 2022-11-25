(ns kyleerhabor.treehouse.server.query
  (:require
   [clojure.core.cache.wrapped :as cache]
   [clojure.set :refer [rename-keys]]
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du] 
   [kyleerhabor.treehouse.server.remote.discord :as discord]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc :refer [defresolver]] 
   [tick.core :as tick]))

;; Would like to improve this in the future.
(def media (cache/ttl-cache-factory {:discord (discord/get-current-user (discord/current-access-token))}
             :ttl (tick/millis (tick/new-duration 1 :days))))

(defn current-discord-user []
  (cache/lookup-or-miss media :discord (fn [_] (discord/get-current-user (discord/current-access-token)))))

(defresolver discord []
  {::pc/output [{:discord [::du/id ::du/username ::du/discriminator]}]}
  {:discord (-> (current-discord-user)
               (select-keys [:id :username :discriminator])
               (rename-keys {:id ::du/id
                             :username ::du/username
                             :discriminator ::du/discriminator}))})

(def registry [discord])

(def parser (p/parser {::p/env {::p/reader [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                                ::pc/mutation-join-globals [:tempids]}
                       ::p/mutate pc/mutate
                       ::p/plugins [(pc/connect-plugin {::pc/register registry})
                                    p/error-handler-plugin
                                    p/elide-special-outputs-plugin]}))

(defn parse [query]
  (parser {} query))
