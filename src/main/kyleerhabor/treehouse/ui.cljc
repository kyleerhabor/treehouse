(ns kyleerhabor.treehouse.ui
  (:require
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du] 
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [#?(:clj com.fulcrologic.fulcro.dom-server
       :cljs com.fulcrologic.fulcro.dom) :as dom]))

(defn singleton [id]
  [::id id])

(defsc Discord [_ _]
  {:query [::du/id ::du/username ::du/discriminator]
   :ident ::du/id})

(defsc DiscordHeading [_ {::du/keys [username discriminator]}]
  {:query [::du/id ::du/username ::du/discriminator] 
   :ident (fn [] (singleton ::DiscordHeading))
   :initial-state {}}
  (dom/div
    (str username \# discriminator)))

(def ui-discord-heading (comp/factory DiscordHeading))

(defsc Heading [_ {:keys [discord email]}]
  {:query [{[:discord '_] (comp/get-query Discord)}
           [:email '_]]
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
          (if (::du/id discord)
            (dom/li
              (ui-discord-heading discord))))))))

(def ui-heading (comp/factory Heading))

(defsc App [_ {::keys [heading]}]
  {:query [{::heading (comp/get-query Heading)}]
   :ident (fn [] (singleton ::App))
   :initial-state (fn [_] {::heading (comp/get-initial-state Heading)})}
  (dom/div
    (ui-heading heading)))

(def ui-app (comp/factory App))

(defsc Root [_ {::keys [app]}]
  {:query [{::app (comp/get-query App)}] 
   :initial-state (fn [_] {::app (comp/get-initial-state App)})}
  (ui-app app))

(def ui-root (comp/factory Root))
