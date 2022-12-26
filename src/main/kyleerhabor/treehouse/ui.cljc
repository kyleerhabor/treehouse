(ns kyleerhabor.treehouse.ui
  (:require
   [#?(:clj com.fulcrologic.fulcro.dom-server
       :cljs com.fulcrologic.fulcro.dom) :as dom]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro.algorithms.do-not-use :refer [base64-encode]] ; Please...
   [com.fulcrologic.fulcro.algorithms.transit :refer [transit-clj->str]]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du]
   [kyleerhabor.treehouse.model.media.github.user :as-alias gu]
   [kyleerhabor.treehouse.route :refer [href+]]
   [kyleerhabor.treehouse.schema.article :as-alias article]
   [kyleerhabor.treehouse.schema.github.repository :as-alias gr]
   [kyleerhabor.treehouse.schema.project :as-alias project]
   [kyleerhabor.treehouse.ui.icon :as icon]))

(defn singleton [id]
  [::id id])

(declare ui-content)

(defsc Content [_ {:keys [name children]}]
  {:query [:name :props :children]}
  (let [children (map #(if (string? %)
                         %
                         (ui-content %)) children)]
    (case (keyword name)
      :title (apply dom/h1 children)
      :text (apply dom/p children)
      (apply dom/div children))))

(def ui-content (comp/factory Content))

(defsc Home [_ {:keys [home]}]
  {:query [::id
           {[:home '_] (comp/get-query Content)}]
   :ident (fn [] (singleton ::Home))
   :initial-state {}}
  (dom/main
    (ui-content home)))

(def ui-home (comp/factory Home))

(defsc Article [_ {::article/keys [title content]}]
  {:query [::article/id ::article/title
           {::article/content (comp/get-query Content)}]
   :ident ::article/id}
  (dom/main
    (dom/h1 title)
    (ui-content content)))

(def ui-article (comp/factory Article))

(defsc ArticlesItem [_ {::article/keys [id title]}]
  {:query [::article/id ::article/title]
   :ident ::article/id}
  (dom/a {:href (href+ :article {:id id})}
    title))

(def ui-articles-item (comp/factory ArticlesItem))

(defsc Articles [_ {:keys [articles]}]
  {:query [::id
           {[:articles '_] (comp/get-query ArticlesItem)}]
   :ident (fn [] (singleton ::Articles))
   :initial-state {}}
  (dom/main
    (dom/h1 "Articles")
    (dom/ul
      (for [{::article/keys [id]
             :as article} articles]
        (dom/li {:key id}
          (ui-articles-item article))))))

(def ui-articles (comp/factory Articles))

(defsc Project [_ {::project/keys [name content github]}]
  {:query [::project/id ::project/name ::project/github
           {::project/content (comp/get-query Content)}]
   :ident ::project/id
   :css [[:.heading {:display "flex"
                     :justify-content "space-between"
                     :align-items "center"}]
         ;; I'd like the height to be the same as the title, but don't know how to do that. This works though.
         [:.icon {:height "2em"}]]}
  (let [{:keys [heading icon]} (css/get-classnames Project)]
    (dom/main
      (dom/div {:classes [heading]}
        (dom/h1 name)
        (when-let [{::gr/keys [url]} github]
          (dom/a {:classes [icon]
                  :href url}
            (icon/ui-github {}))))
      (ui-content content))))

(def ui-project (comp/factory Project))

(defsc ProjectsItem [_ {::project/keys [id name]}]
  {:query [::project/id ::project/name]
   :ident ::project/id}
  (dom/a {:href (href+ :project {:id id})}
    name))

(def ui-projects-item (comp/factory ProjectsItem))

(defsc Projects [_ {:keys [projects]}]
  {:query [::id
           {[:projects '_] (comp/get-query ProjectsItem)}]
   :ident (fn [] (singleton ::Projects))
   :initial-state {}}
  (dom/main
    (dom/h1 "Projects")
    (dom/ul
      (for [{::project/keys [id]
             :as proj} projects]
        (dom/li {:key id}
          (ui-projects-item proj))))))

(def ui-projects (comp/factory Projects))

(defsc NotFound [_ _]
  ;; TODO: Provide a body and stop the UI from present it in the UI for a split moment on page load.
  {:query [::id]
   :ident (fn [] (singleton ::?))
   :initial-state {}})

(def ui-not-found (comp/factory NotFound))

(defsc Router [this props]
  {:query (fn [] {::Home (comp/get-query Home)
                  ::Articles (comp/get-query Articles)
                  ::Projects (comp/get-query Projects)
                  ::article/id (comp/get-query Article)
                  ::project/id (comp/get-query Project)
                  ::? (comp/get-query NotFound)})
   :ident (fn []
            (if-let [single (::id props)]
              [single ::id] ; Flip.
              (let [comp (cond
                           (contains? props ::article/id) Article
                           (contains? props ::project/id) Project
                           :else NotFound)]
                (comp/get-ident comp props))))
   ;; Hope this won't cause issues.
   :initial-state (fn [_] {})}
  (let [[name] (comp/get-ident this)
        route (case name
                ::Home ui-home
                ::Articles ui-articles
                ::Projects ui-projects
                ::article/id ui-article
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
           {[:discord '_] (comp/get-query DiscordHeading)}
           {[:github '_] (comp/get-query GithubHeading)}]
   :initial-state {}
   :css [[:.nav {:display "flex"
                 :border-bottom "black solid"
                 :justify-content "space-between"
                 :gap "1em"}]
         [:.navlist {:display "flex"
                     :gap "0.4em"
                     :list-style "none"
                     :padding "0"}]]}
  (let [{:keys [nav navlist]} (css/get-classnames Heading)]
    (dom/header {:classes [nav]}
      (dom/nav
        (dom/ul {:classes [navlist]}
          (dom/li
            (dom/a {:href (href+ :home)}
              "Home"))
          (dom/li
            (dom/a {:href (href+ :articles)}
              "Articles"))
          (dom/li
            (dom/a {:href (href+ :projects)}
              "Projects"))))
      (dom/nav
        (dom/address
          (dom/ul {:classes [navlist]}
            (if email
              (dom/li
                (dom/div
                  (dom/a {:href (str "mailto:" email)}
                    "Email"))))
            (if github
              (dom/li
                (ui-github-heading github)))
            (if discord
              (dom/li
                (ui-discord-heading discord)))))))))

(def ui-heading (comp/factory Heading))

(defsc App [_ {::keys [heading]
               :keys [route]}]
  ;; I'd like :route to be global, but [:route '_] doesn't work for some reason.
  {:query [{:route (comp/get-query Router)}
           {::heading (comp/get-query Heading)}]
   :initial-state (fn [_] {:route (comp/get-initial-state Router)
                           ::heading (comp/get-initial-state Heading)})}
  (dom/div
    (ui-heading heading)
    (ui-router route)))

(def ui-app (comp/factory App))

(defsc Root [_ {::keys [app]}]
  {:query [{::app (comp/get-query App)}] 
   :initial-state (fn [_] {::app (comp/get-initial-state App)})}
  (ui-app app))

(def ui-root (comp/factory Root))

(defn document [db props {:keys [anti-forgery-token]}]
  (dom/html {:lang "en-US"}
    (dom/head
      (dom/meta {:charset "utf-8"})
      (dom/title "Kyle Erhabor")
      (dom/meta {:name "viewport"
                 :content "width=device-width, initial-scale=1"})
      (dom/meta {:name "description"
                 :content "Kyle Erhabor is a software developer known under the pseudonym Klay."})
      ;; It's kind of annoying that Fulcro prepends a space when using :classes even when :className and (dom/... :.class) aren't used.
      (dom/link {:href "/assets/main/css/compiled/main.css"
                 :rel "stylesheet"}))
    (dom/body
      (dom/div :#app
        (ui-root props))
      (dom/script {:dangerouslySetInnerHTML {:__html (str
                                                       "window.INITIAL_APP_STATE=\"" (base64-encode (transit-clj->str db)) "\";"
                                                       "var fulcro_network_csrf_token=\"" anti-forgery-token "\"")}})
      (dom/script {:src "/assets/main/js/compiled/main.js"}))))
