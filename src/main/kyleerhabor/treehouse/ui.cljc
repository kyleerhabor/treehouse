(ns kyleerhabor.treehouse.ui
  (:require
   [kyleerhabor.treehouse.model.project :as-alias project]
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du]
   [kyleerhabor.treehouse.model.media.github.user :as-alias gu]
   [kyleerhabor.treehouse.route :refer [href+]]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.algorithms.do-not-use :refer [base64-encode]] ; Please...
   [com.fulcrologic.fulcro.algorithms.transit :refer [transit-clj->str]]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [#?(:clj com.fulcrologic.fulcro.dom-server
       :cljs com.fulcrologic.fulcro.dom) :as dom]
   [com.fulcrologic.fulcro-css.css-injection :refer [style-element]]))

(defn singleton [id]
  [::id id])

(defsc Home [_ _]
  {:query [::id]
   :ident (fn [] (singleton ::Home))
   :initial-state {}}
  (dom/div
    (dom/h1 "Hello!")
    (dom/p "I'm Kyle Erhabor, a software developer known under the pseudonym Klay.")))

(def ui-home (comp/factory Home))

(defsc Projects [_ _]
  {:query [::id]
   :ident (fn [] (singleton ::Projects))
   :initial-state {}}
  (dom/div
    (dom/h1 "Projects")))

(def ui-projects (comp/factory Projects))

(defsc Project [_ {::project/keys [name]}]
  {:query [::project/id ::project/name]
   :ident ::project/id}
  (dom/main
    (dom/h1 name)))

(def ui-project (comp/factory Project))

(defsc NotFound [_ _]
  (dom/p "?"))

(def ui-not-found (comp/factory NotFound))

(defsc Router [this props]
  {:query (fn [] {::Home (comp/get-query Home)
                  ::Projects (comp/get-query Projects)
                  ::project/id (comp/get-query Project)})
   :ident (fn []
            (if-let [single (::id props)]
              [single ::id]
              (comp/get-ident Project props)))}
  (let [[name] (comp/get-ident this)
        route (case name
                ::Home ui-home
                ::Projects ui-projects
                ::project/id ui-project
                ui-not-found)]
    (route props)))

(def ui-router (comp/factory Router))

(defsc GithubUser [_ _]
  {:query [::gu/id ::gu/url]
   :ident ::gu/id})

(defsc GithubHeading [_ {::gu/keys [url]}]
  {:query [::gu/url]}
  (dom/div
    (dom/a {:href url}
      "GitHub")))

(def ui-github-heading (comp/factory GithubHeading))

(defsc DiscordUser [_ _]
  {:query [::du/id ::du/username ::du/discriminator]
   :ident ::du/id})

(defsc DiscordHeading [_ {::du/keys [username discriminator]}]
  {:query [::du/username ::du/discriminator]}
  (dom/div
    "Discord: "
    (str username \# discriminator)))

(def ui-discord-heading (comp/factory DiscordHeading))

(defsc Heading [_ {:keys [discord email github]}]
  {:query [[:email '_]
           {[:discord '_] (comp/get-query DiscordUser)}
           {[:github '_] (comp/get-query GithubUser)}]
   :initial-state {}} 
  (dom/header
    (dom/nav
      (dom/ul
        (dom/li
          (dom/a {:href (href+ :home)}
            "Home"))
        (dom/li
          (dom/a {:href (href+ :projects)}
            "Projects")))) 
    (dom/nav
      (dom/address
        (dom/ul
          (if email
            (dom/li
              (dom/a {:href (str "mailto:" email)}
                "Email")))
          (if github
            (dom/li
              (ui-github-heading github)))
          (if discord
            (dom/li
              (ui-discord-heading discord))))))))

(def ui-heading (comp/factory Heading))

(defsc App [_ {::keys [heading]
               :keys [route]}]
  ;; I'd like :route to be global, but [:route '_], for some reason, doesn't work.
  {:query [{:route (comp/get-query Router)}
           {::heading (comp/get-query Heading)}]
   :initial-state (fn [_] {::heading (comp/get-initial-state Heading)})}
  (dom/div
    (ui-heading heading)
    (if route
      (ui-router route)
      "?")))

(def ui-app (comp/factory App))

(defsc Root [_ {::keys [app]}]
  {:query [{::app (comp/get-query App)}] 
   :initial-state (fn [_] {::app (comp/get-initial-state App)})}
  (ui-app app))

(def ui-root (comp/factory Root))

(defsc Document [this props]
  (dom/html
    (dom/head
      (dom/meta {:charset "UTF-8"})
      (dom/meta {:name "viewport"
                 :content "width=device-width, initial-scale=1"})
      (dom/title "Kyle Erhabor")
      (dom/script {:dangerouslySetInnerHTML {:__html (str "window.INITIAL_APP_STATE=\"" (-> (app/current-state this)
                                                                                          transit-clj->str
                                                                                          base64-encode) "\"")}})
      ;; It's kind of annoying that Fulcro prepends a space when using :classes even when :className and .class aren't used.
      (style-element {:component Root
                      :garden-flags {:pretty-print? false}}))
    (dom/body
      (dom/div :#app
        (ui-root props))
      (dom/script {:src "/assets/main/js/compiled/main.js"}))))

(def ui-document (comp/computed-factory Document))
