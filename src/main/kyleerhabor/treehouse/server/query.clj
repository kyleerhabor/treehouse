(ns kyleerhabor.treehouse.server.query
  (:require
   [clojure.core.cache.wrapped :as cache]
   [clojure.set :refer [rename-keys]]
   [kyleerhabor.treehouse.model.project :as-alias project]
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du]
   [kyleerhabor.treehouse.model.media.github.user :as-alias gu]
   [kyleerhabor.treehouse.server.database :as db]
   [kyleerhabor.treehouse.server.remote.discord :as discord]
   [kyleerhabor.treehouse.server.remote.github :as github]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc :refer [defresolver]]
   [datalevin.core :as d]
   [tick.core :as tick]))

;; Would like to improve this in the future.
(def media (cache/ttl-cache-factory {:discord (discord/get-current-user (discord/current-access-token))
                                     :github (github/viewer)}
             :ttl (tick/millis (tick/new-duration 1 :days))))

(defn current-discord-user []
  (cache/lookup-or-miss media :discord (fn [_] (discord/get-current-user (discord/current-access-token)))))

(defn current-github-user []
  (cache/lookup-or-miss media :github (fn [_] (github/viewer))))

(defresolver discord []
  {::pc/output [{:discord [::du/id ::du/username ::du/discriminator]}]}
  {:discord (-> (current-discord-user)
               (select-keys [:id :username :discriminator])
               (rename-keys {:id ::du/id
                             :username ::du/username
                             :discriminator ::du/discriminator}))})

(defresolver github []
  {::pc/output [{:github [::gu/id ::gu/url]}]}
  {:github (rename-keys (current-github-user) {:id ::gu/id
                                               :url ::gu/url})})

(defresolver projects [{::keys [db]} _]
  {::pc/output [{:projects [::project/id]}]}
  {:projects (map (fn [datom] {::project/id (d/datom-v datom)}) (d/datoms db :ave ::project/id))})

(defresolver project-name [{::keys [db]} {::project/keys [id]}]
  {::pc/output [::project/name]}
  (select-keys (d/entity db [::project/id id]) [::project/name]))

(def registry [discord github projects project-name])

(def parser (p/parser {::p/env {::p/reader [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                                ::pc/mutation-join-globals [:tempids]}
                       ::p/mutate pc/mutate
                       ::p/plugins [(pc/connect-plugin {::pc/register registry})
                                    p/error-handler-plugin
                                    p/elide-special-outputs-plugin]}))

(defn parse [query]
  (parser {::db @db/conn} query))
