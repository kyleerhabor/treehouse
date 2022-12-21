(ns kyleerhabor.treehouse.ui
  (:require
   [kyleerhabor.treehouse.model.project :as-alias project]
   [kyleerhabor.treehouse.model.media.discord.user :as-alias du]
   [kyleerhabor.treehouse.model.media.github.user :as-alias gu]
   [kyleerhabor.treehouse.route :refer [href+]]
   [kyleerhabor.treehouse.schema.article :as-alias article]
   [kyleerhabor.treehouse.schema.github.repository :as-alias gr]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.algorithms.do-not-use :refer [base64-encode]] ; Please...
   [com.fulcrologic.fulcro.algorithms.transit :refer [transit-clj->str]]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [#?(:clj com.fulcrologic.fulcro.dom-server
       :cljs com.fulcrologic.fulcro.dom) :as dom]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro-css.css-injection :refer [style-element]]))

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
                     :align-items "center"}]]}
  (let [{:keys [heading]} (css/get-classnames Project)]
    (dom/main
      (dom/div {:classes [heading]}
        (dom/h1 name)
        (when-let [{::gr/keys [url]} github]
          (dom/a {:href url}
            "GitHub")))
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
  (dom/p "?"))

(def ui-not-found (comp/factory NotFound))

(defsc Router [this props]
  {:query (fn [] {::Home (comp/get-query Home)
                  ::Articles (comp/get-query Articles)
                  ::Projects (comp/get-query Projects)
                  ::article/id (comp/get-query Article)
                  ::project/id (comp/get-query Project)})
   :ident (fn []
            (if-let [single (::id props)]
              [single ::id] ; Flip.
              (let [c (cond
                        (contains? props ::article/id) Article
                        :else Project)]
                (comp/get-ident c props))))}
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
    ;; Use the Discord logo instead? But then it'd also make sense to modify the email and github.
    "Discord: "
    (str username \# discriminator)))

(def ui-discord-heading (comp/factory DiscordHeading))

(defsc Heading [_ {:keys [discord email github]}]
  {:query [[:email '_]
           {[:discord '_] (comp/get-query DiscordUser)}
           {[:github '_] (comp/get-query GithubUser)}]
   :initial-state {}
   :css [[:.nav {:display "flex"
                 :justify-content "space-between"}]
         [:.navlist {:display "flex"
                     :gap "0.4em"
                     :list-style "none"
                     :padding 0}]]}
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
                (dom/a {:href (str "mailto:" email)}
                  "Email")))
            (if github
              (dom/li
                (ui-github-heading github)))
            (if discord
              (dom/li
                (ui-discord-heading discord)))))))))

(def ui-heading (comp/factory Heading))

(defsc App [_ {::keys [heading]
               :keys [route]}]
  ;; I'd like :route to be global, but [:route '_], for some reason, doesn't work.
  {:query [{:route (comp/get-query Router)}
           {::heading (comp/get-query Heading)}]
   :initial-state (fn [_] {::heading (comp/get-initial-state Heading)})}
  (dom/div
    (ui-heading heading)
    (dom/hr)
    (if route
      (ui-router route)
      (ui-not-found {}))))

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
