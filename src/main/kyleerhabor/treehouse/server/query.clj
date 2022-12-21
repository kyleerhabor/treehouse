(ns kyleerhabor.treehouse.server.query
  (:require
   [clojure.java.io :as io]
   [clojure.set :refer [rename-keys]]
   [kyleerhabor.treehouse.model.project :as-alias project]
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du]
   [kyleerhabor.treehouse.model.media.github.user :as-alias gu]
   [kyleerhabor.treehouse.schema :as s]
   [kyleerhabor.treehouse.schema.article :as-alias article]
   [kyleerhabor.treehouse.schema.github.repository :as-alias gr]
   [kyleerhabor.treehouse.server.database :as db]
   [kyleerhabor.treehouse.server.query.cache :as c]
   [kyleerhabor.treehouse.util :refer [load-edn]]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc :refer [defresolver]]))

(def home-content (s/parse-element (load-edn (io/resource "content/home.edn"))))

(def arts (update (load-edn (io/resource "content/articles.edn")) :articles
            (fn [projs]
              (map
                (fn [proj]
                  (-> proj
                    (update :path #(s/parse-element (load-edn (io/resource %))))
                    (rename-keys {:path :content}))) projs))))

(def arts-map (update arts :articles #(zipmap (map :id %) %)))

(def projs (update (load-edn (io/resource "content/projects.edn")) :projects
             (fn [projs]
               (map
                 (fn [proj]
                   (-> proj
                     (update :path #(s/parse-element (load-edn (io/resource %))))
                     (rename-keys {:path :content}))) projs))))

(def projs-map (update projs :projects #(zipmap (map :id %) %)))

(defresolver discord []
  {::pc/output [{:discord [::du/id ::du/username ::du/discriminator]}]}
  {:discord (-> (c/current-discord-user)
              (select-keys [:id :username :discriminator])
              (rename-keys {:id ::du/id
                            :username ::du/username
                            :discriminator ::du/discriminator}))})

(defresolver github []
  {::pc/output [{:github [::gu/id ::gu/url]}]}
  {:github (rename-keys (c/current-github-viewer) {:id ::gu/id
                                                   :url ::gu/url})})

(defresolver articles []
  {::pc/output [{:articles [::article/id]}]}
  {:articles (map #(rename-keys (select-keys % [:id]) {:id ::article/id}) (:articles arts))})

(defresolver article-title [{::article/keys [id]}]
  {::pc/output [::article/title]}
  (if-let [article (get (:articles arts-map) id)]
    {::article/title (:title (:props (:content article)))}))

(defresolver article-content [{::article/keys [id]}]
  {::pc/output [::article/content]}
  (if-let [article (get (:articles arts-map) id)]
    {::article/content (:content article)}))

(defresolver projects []
  {::pc/output [{:projects [::project/id]}]}
  {:projects (for [{:keys [id]} (:projects projs)]
               {::project/id id})})

(defresolver project-name [{::project/keys [id]}]
  {::project/name (:name (:props (:content (get (:projects projs-map) id))))})

(defresolver project-content [{::project/keys [id]}]
  {::project/content (:content (get (:projects projs-map) id))})

(defresolver project-github [{::project/keys [id]}]
  {::pc/output [{::project/github [::gr/url]}]}
  (if-let [repo (:github (:props (:content (get (:projects projs-map) id))))]
    {::project/github (rename-keys (c/project-github-repo repo) {:url ::gr/url})}))

(def home (pc/constantly-resolver :home home-content))

(def registry [discord github articles article-title article-content projects project-name project-content project-github home])

(def parser (p/parser {::p/env {::p/reader [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                                ::pc/mutation-join-globals [:tempids]}
                       ::p/mutate pc/mutate
                       ::p/plugins [(pc/connect-plugin {::pc/register registry})
                                    p/error-handler-plugin
                                    p/elide-special-outputs-plugin]}))

(defn parse [query]
  (parser {::db @db/conn} query))
