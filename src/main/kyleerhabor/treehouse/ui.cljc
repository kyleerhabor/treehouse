(ns kyleerhabor.treehouse.ui
  (:require
   [kyleerhabor.treehouse.model.media :as-alias media]
   [kyleerhabor.treehouse.model.route :as-alias route]
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du]
   [kyleerhabor.treehouse.model.media.github.user :as-alias gu]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.algorithms.do-not-use :refer [base64-encode]] ; Please...
   [com.fulcrologic.fulcro.algorithms.transit :refer [transit-clj->str]]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [#?(:clj com.fulcrologic.fulcro.dom-server
       :cljs com.fulcrologic.fulcro.dom) :as dom]
   [com.fulcrologic.fulcro-css.css-injection :refer [style-element]]
   #?(:cljs [kyleerhabor.treehouse.route :refer [href+]])))

(defn singleton [id]
  [::id id])

(defsc Home [_ _]
  (dom/div
    (dom/h1 "Hello!")
    (dom/p "I'm Kyle Erhabor, a software developer known under the pseudonym Klay.")))

(def ui-home (comp/factory Home))

(defsc Projects [_ _]
  (dom/div
    (dom/h1 "Projects")))

(def ui-projects (comp/factory Projects))

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

(defsc Heading [_ {::media/keys [email]
                   :keys [discord github]}]
  {:query [[::media/email '_]
           {[:discord '_] (comp/get-query DiscordUser)}
           {[:github '_] (comp/get-query GithubUser)}]
   :initial-state {}} 
  (dom/header
    (dom/nav
      #?(:cljs
         (dom/ul 
           (dom/li
             (dom/a {:href (href+ :home)}
               "Home"))
           (dom/li
             (dom/a {:href (href+ :projects)}
               "Projects"))))) 
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

(defsc Router [_ {::route/keys [name]}]
  ;; There's also ::route/path and ::route/query
  {:query [::route/name]}
  (case name
    :home (ui-home {})
    :projects (ui-projects {})
    "?"))

(def ui-router (comp/factory Router))

(defsc App [_ {::keys [heading]
               :keys [route]}]
  {:query [[:route '_]
           {::heading (comp/get-query Heading)}]
   :initial-state (fn [_] {::heading (comp/get-initial-state Heading)})}
  (dom/div
    (ui-heading heading)
    (ui-router route)))

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
      (dom/script {:dangerouslySetInnerHTML {:__html (str "window.INITIAL_APP_STATE=\"" (-> this
                                                                                          app/current-state
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
