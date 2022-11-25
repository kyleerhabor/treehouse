(ns kyleerhabor.treehouse.ui
  (:require
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du]
   [kyleerhabor.treehouse.model.media.github.user :as-alias gu]
   [kyleerhabor.treehouse.util :refer [debug?]] 
   [com.fulcrologic.fulcro.algorithms.do-not-use :refer [base64-encode]] ; Please...
   [com.fulcrologic.fulcro.algorithms.transit :refer [transit-clj->str]] 
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [#?(:clj com.fulcrologic.fulcro.dom-server
       :cljs com.fulcrologic.fulcro.dom) :as dom]
   [com.fulcrologic.fulcro-css.css-injection :refer [style-element]]))

(defn singleton [id]
  [::id id])

(defsc Github [_ _]
  {:query [::gu/id ::gu/url]
   :ident ::gu/id})

(defsc GithubHeading [_ {::gu/keys [url]}]
  {:query [::gu/url]
   :ident (fn [] (singleton ::GithubHeading))
   :initial-state {}}
  (dom/div
    (dom/a {:href url}
      "GitHub")))

(def ui-github-heading (comp/factory GithubHeading))

(defsc Discord [_ _]
  {:query [::du/id ::du/username ::du/discriminator]
   :ident ::du/id})

(defsc DiscordHeading [_ {::du/keys [username discriminator]}]
  {:query [::du/username ::du/discriminator] 
   :ident (fn [] (singleton ::DiscordHeading))
   :initial-state {}}
  (dom/div
    (str username \# discriminator)))

(def ui-discord-heading (comp/factory DiscordHeading))

(defsc Heading [_ {:keys [discord email github]}]
  {:query [[:email '_]
           {[:discord '_] (comp/get-query Discord)}
           {[:github '_] (comp/get-query Github)}]
   :initial-state {}
   :ident (fn [] (singleton ::Heading))}
  (dom/header
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

(defsc Home [_ _] 
  (dom/div
    (dom/h1 "Hello!")
    (dom/p "I'm Kyle Erhabor, a software developer known under the pseudonym Klay.")))

(def ui-home (comp/factory Home))

(defsc App [_ {::keys [heading]}]
  {:query [{::heading (comp/get-query Heading)}]
   :ident (fn [] (singleton ::App))
   :initial-state (fn [_] {::heading (comp/get-initial-state Heading)})}
  (dom/div
    (ui-heading heading)
    (ui-home)))

(def ui-app (comp/factory App))

(defsc Root [_ {::keys [app]}]
  {:query [{::app (comp/get-query App)}] 
   :initial-state (fn [_] {::app (comp/get-initial-state App)})}
  (ui-app app))

(def ui-root (comp/factory Root))

(defn document [db props]
  (dom/html
    (dom/head
      (dom/meta {:charset "UTF-8"})
      (dom/meta {:name "viewport"
                 :content "width=device-width, initial-scale=1"})
      (dom/title "Kyle Erhabor")
      (dom/script
        (str "window.INITIAL_APP_STATE = \"" (base64-encode (transit-clj->str db)) \"))
      (style-element {:component Root
                      :garden-flags {:pretty-print? debug?}}))
    (dom/body
      (dom/div :#app
        (ui-root props))
      (dom/script {:src "/assets/main/js/compiled/main.js"}))))
