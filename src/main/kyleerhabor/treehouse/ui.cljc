(ns kyleerhabor.treehouse.ui
  (:require
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du]
   [kyleerhabor.treehouse.model.media.github.user :as-alias gu]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.algorithms.do-not-use :refer [base64-encode]] ; Please...
   [com.fulcrologic.fulcro.algorithms.transit :refer [transit-clj->str]]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [#?(:clj com.fulcrologic.fulcro.dom-server
       :cljs com.fulcrologic.fulcro.dom) :as dom]
   [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
   [com.fulcrologic.fulcro.ui-state-machines :as uism]
   [com.fulcrologic.fulcro-css.css-injection :refer [style-element]]
   [com.fulcrologic.rad.routing :as r]))

(defn singleton [id]
  [::id id])

(defsc Home [_ _]
  {:query []
   :initial-state {}
   :ident (fn [] (singleton ::Home))
   :route-segment ["home"]}
  (dom/div
    (dom/h1 "Hello!")
    (dom/p "I'm Kyle Erhabor, a software developer known under the pseudonym Klay.")))

(defsc Projects [_ _]
  {:query []
   :initial-state {}
   :ident (fn [] (singleton ::Projects))
   :route-segment ["projects"]}
  (dom/div
    (dom/h1 "Projects")))

(defsc Router [_ {:keys [state]}]
  {:query [:state]}
  (case state
    :initial "?"
    :pending "..."
    :failed "!"
    nil))

(def ui-router (comp/factory Router))

(defrouter AppRouter [_ {state :current-state}]
  {:router-targets [Home Projects]}
  (ui-router {:state state}))

(def ui-app-router (comp/factory AppRouter))

(defsc Github [_ _]
  {:query [::gu/id ::gu/url]
   :ident ::gu/id})

(defsc GithubHeading [_ {::gu/keys [url]}]
  {:query [::gu/url]}
  (dom/div
    (dom/a {:href url}
      "GitHub")))

(def ui-github-heading (comp/factory GithubHeading))

(defsc Discord [_ _]
  {:query [::du/id ::du/username ::du/discriminator]
   :ident ::du/id})

(defsc DiscordHeading [_ {::du/keys [username discriminator]}]
  {:query [::du/username ::du/discriminator]}
  (dom/div
    "Discord: "
    (str username \# discriminator)))

(def ui-discord-heading (comp/factory DiscordHeading))

(defsc Heading [this {:keys [discord email github]}]
  {:query [[:email '_]
           [::uism/asm-id '_]
           {[:discord '_] (comp/get-query Discord)}
           {[:github '_] (comp/get-query Github)}]
   :initial-state {}} 
  (dom/header
    (if (uism/get-active-state this ::AppRouter)
      (dom/nav
        (dom/ul
          (dom/li
            (dom/button {:onClick #(r/route-to! this Home {})}
              "Home"))
          (dom/li
            (dom/button {:onClick #(r/route-to! this Projects {})}
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

(defsc App [_ {::keys [heading router]}]
  {:query [{::heading (comp/get-query Heading)}
           {::router (comp/get-query AppRouter)}]
   :initial-state (fn [_] {::heading (comp/get-initial-state Heading)
                           ::router (comp/get-initial-state AppRouter)})}
  (dom/div
    (ui-heading heading)
    (ui-app-router router)))

(def ui-app (comp/factory App))

(defsc Root [_ {::keys [app]}]
  {:query [{::app (comp/get-query App)}] 
   :initial-state (fn [_] {::app (comp/get-initial-state App)})}
  (ui-app app))

(def ui-root (comp/factory Root))

(defsc Document [this props {:keys [token]}]
  (dom/html
    (dom/head
      (dom/meta {:charset "UTF-8"})
      (dom/meta {:name "viewport"
                 :content "width=device-width, initial-scale=1"})
      (dom/title "Kyle Erhabor")
      (dom/script {:dangerouslySetInnerHTML {:__html (str
                                                       "var fulcro_network_csrf_token=\"" token "\";"
                                                       "window.INITIAL_APP_STATE=\"" (-> this
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
