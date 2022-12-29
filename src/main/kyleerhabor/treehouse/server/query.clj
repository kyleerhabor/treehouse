(ns kyleerhabor.treehouse.server.query
  (:require
   [clojure.java.io :as io]
   [clojure.set :refer [rename-keys]]
   [kyleerhabor.treehouse.schema :as s]
   [kyleerhabor.treehouse.schema.article :as-alias article]
   [kyleerhabor.treehouse.schema.github.repository :as-alias gr]
   [kyleerhabor.treehouse.schema.project :as-alias project]
   [kyleerhabor.treehouse.server.database :as db]
   [kyleerhabor.treehouse.server.query.cache :as c]
   [kyleerhabor.treehouse.util :refer [load-edn]]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc :refer [defresolver]]))

(def home-content (s/parse-element (load-edn (io/resource "content/home.edn"))))

(def arts (update (load-edn (io/resource "content/articles.edn")) :articles
            (fn [articles]
              (map
                (fn [article]
                  (-> article
                    (update :path #(s/parse-element (load-edn (io/resource %))))
                    (rename-keys {:path :content}))) articles))))

(def arts-map (update arts :articles #(zipmap (map :id %) %)))

(def projs (update (load-edn (io/resource "content/projects.edn")) :projects
             (fn [projects]
               (map
                 (fn [project]
                   (-> project
                     (update :path #(s/parse-element (load-edn (io/resource %))))
                     (rename-keys {:path :content}))) projects))))

(def projs-map (update projs :projects #(zipmap (map :id %) %)))

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
  {:projects (map #(rename-keys (select-keys % [:id]) {:id ::project/id}) (:projects projs))})

(defresolver project-name [{::project/keys [id]}]
  {::pc/output [::project/name]}
  (if-let [project (get (:projects projs-map) id)]
    {::project/name (:name (:props (:content project)))}))

(defresolver project-content [{::project/keys [id]}]
  {::pc/output [::project/content]}
  (if-let [project (get (:projects projs-map) id)]
    {::project/content (:content project)}))

(defresolver project-github [{::project/keys [id]}]
  {::pc/output [{::project/github [::gr/url]}]}
  (if-let [repo (:github (:props (:content (get (:projects projs-map) id))))]
    {::project/github (rename-keys (c/project-github-repo repo) {:url ::gr/url})}))

(def home (pc/constantly-resolver :home home-content))

(def registry [articles article-title article-content projects project-name project-content project-github home])

(def parser (p/parser {::p/env {::p/reader [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                                ::pc/mutation-join-globals [:tempids]}
                       ::p/mutate pc/mutate
                       ::p/plugins [(pc/connect-plugin {::pc/register registry})
                                    p/error-handler-plugin
                                    p/elide-special-outputs-plugin]}))

(defn parse [query]
  (parser {::db @db/conn} query))
