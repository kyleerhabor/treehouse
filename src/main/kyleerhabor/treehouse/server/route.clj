(ns kyleerhabor.treehouse.server.route
  (:require
   [clojure.set :refer [rename-keys]]
   [clojure.string :as str]
   [kyleerhabor.treehouse.mutation :as mut]
   [kyleerhabor.treehouse.route :as route]
   [kyleerhabor.treehouse.route.ui :as route+]
   [kyleerhabor.treehouse.schema.article :as-alias article]
   [kyleerhabor.treehouse.schema.discord.user :as-alias du]
   [kyleerhabor.treehouse.schema.github.user :as-alias gu]
   [kyleerhabor.treehouse.schema.project :as-alias project]
   [kyleerhabor.treehouse.server.config :as-alias config :refer [config]]
   [kyleerhabor.treehouse.server.query :as q]
   [kyleerhabor.treehouse.server.query.cache :as qc]
   [kyleerhabor.treehouse.server.response :refer [doctype forbidden]]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro.algorithms.denormalize :refer [db->tree]]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.algorithms.server-render :as ssr]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.dom-server :as dom]
   [com.fulcrologic.fulcro.server.api-middleware :as s :refer [wrap-transit-params wrap-transit-response]]
   [reitit.core :as r]
   [reitit.middleware :as-alias rmw]
   [reitit.ring :as rr]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.exception :as rrex]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.x-headers :refer [wrap-content-type-options wrap-frame-options]]
   [ring.util.mime-type :refer [default-mime-types]]
   [ring.util.response :as res]))

(defn allowed
  "Returns a comma-separated string of the request methods supported by a request."
  [request]
  (->> (:result (rr/get-match request))
    (filter val)
    (map #(str/upper-case (name (key %))))
    (str/join ", ")))

(defn initial-db [root]
  (ssr/build-initial-state (comp/get-initial-state root) root))

(def root ui/Root)

(def root-initial-db (initial-db root))

(defn current-db [db match]
  (let [db (-> db
             (assoc :email (::config/email config))
             (merge/merge-component ui/DiscordUser (-> (qc/current-discord-user)
                                                     (select-keys [:id :username :discriminator])
                                                     (rename-keys {:id ::du/id
                                                                   :username ::du/username
                                                                   :discriminator ::du/discriminator}))
               :replace [:discord])
             (merge/merge-component ui/GithubUser (-> (qc/current-github-viewer)
                                                    (select-keys [:id :url])
                                                    (rename-keys {:id ::gu/id
                                                                  :url ::gu/url}))
               :replace [:github]))
        db (if-let [ui (:ui (:data match))]
             (mut/route* ((:handler ui) db match) (route+/props match))
             db)]
    db))

(defn page-handler [request]
  (let [db (current-db root-initial-db (assoc (rr/get-match request) :parameters (:parameters request)))
        props (db->tree (comp/get-query root db) db db)
        app (app/fulcro-app {:initial-db db})
        html (binding [comp/*app* app]
               (dom/render-to-str (ui/document db props {:anti-forgery-token (:anti-forgery-token request)})))]
    (-> (res/response (str doctype html))
      (res/content-type (get default-mime-types "html"))
      (res/charset "utf-8"))))

;; ::ui/app :route should be an ident pointing to some map for the route.

(defn home [db _]
  (merge db (q/parse [{:home [:name :props :children]}])))

(defn articles [db _]
  (merge/merge-component db ui/ArticlesItem (:articles (q/parse [{:articles [::article/id ::article/name]}])) :replace [:article]))

(defn article [db match]
  (let [ident (comp/get-ident ui/Project (route+/props match))]
    (merge/merge-component db ui/Project (get (q/parse [{ident [::project/id ::project/name]}]) ident))))

(defn projects [db _]
  (merge/merge-component db ui/ProjectsItem (:projects (q/parse [{:projects [::project/id ::project/name]}])) :replace [:projects]))

(defn project [db match]
  (let [ident (comp/get-ident ui/Project (route+/props match))]
    (merge/merge-component db ui/Project (get (q/parse [{ident [::project/id ::project/name ::project/content ::project/github]}]) ident))))

(defn api-handler [{query :transit-params}]
  (let [r (q/parse query)]
    (s/generate-response (merge (res/response r) (s/apply-response-augmentations r)))))

(def routes {:home {:middleware [[:session] [:anti-forgery]]
                    :get {:handler page-handler}
                    :ui {:handler home}}
             ;; Should anti-forgery be used here?
             :api {:middleware [[:transit-params] [:transit-response]]
                   :post {:handler api-handler}}
             :articles {:middleware [[:session] [:anti-forgery]]
                        :get {:handler page-handler}
                        :ui {:handler articles}}
             :article {:middleware [[:session] [:anti-forgery]]
                       :get {:handler page-handler}
                       :ui {:handler article}}
             :projects {:middleware [[:session] [:anti-forgery]]
                        :get {:handler page-handler}
                        :ui {:handler projects}}
             :project {:middleware [[:session] [:anti-forgery]]
                       :get {:handler page-handler}
                       :ui {:handler project}}})

(def exception-middleware (rrex/create-exception-middleware {::rrex/default #(page-handler %2)}))

(def router (rr/router (r/routes route+/router)
              (merge (dissoc (r/options route+/router) :compile)
                ;; TODO: Move wrap-content-type-options and wrap-frame-options so API requests aren't included.
                {:data {:middleware [rrc/coerce-request-middleware
                                     [wrap-content-type-options :nosniff]
                                     ;; Content-Security-Policy could replace this.
                                     [wrap-frame-options :deny]]}
                 :expand (route/merge-expand routes)
                 ::rmw/registry {:anti-forgery [wrap-anti-forgery {:error-response forbidden}]
                                 :session wrap-session
                                 :transit-params [wrap-transit-params {:malformed-response (res/bad-request nil)}]
                                 :transit-response wrap-transit-response}
                 ::rr/default-options-endpoint {:handler (comp res/response allowed)}})))

(def default-routes [["*" {:name :any
                           :middleware [[:session] [:anti-forgery]]
                           :get {:handler page-handler}}]])

(def default-router (rr/router default-routes
                      (r/options router)))
