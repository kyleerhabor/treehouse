(ns kyleerhabor.treehouse.server.remote.discord
  (:require
   [kyleerhabor.treehouse.server.remote :as r]
   [kyleerhabor.treehouse.server.config :as config :refer [config]]
   [kyleerhabor.treehouse.server.database :as db]
   [kyleerhabor.treehouse.server.response :as res]
   [datalevin.core :as d]
   [martian.core :as m]
   [mount.core :as mount]
   [schema.core :as s]
   [taoensso.timbre :as log]))

(def api-url "https://discord.com/api")

(def api-version 10)

(def api-user-agent (str "DiscordBot (" config/url ", " (subs config/version 1) ")"))

(defn current-access-token []
  (d/datom-v (db/latest-datom (d/datoms @db/conn :ave ::access-token))))

(defn current-refresh-token []
  (d/datom-v (db/latest-datom (d/datoms @db/conn :ave ::refresh-token))))

(defn save-access [{access :access_token
                    refresh :refresh_token}]
  (d/transact! db/conn [{::access-token access
                         ::refresh-token refresh}]))

(def user-agent
  {:name ::user-agent
   :enter (fn [ctx]
            (assoc-in ctx [:request :headers "User-Agent"] api-user-agent))})

(def authorization (r/authorization (fn []
                                      {:type "Bearer"
                                       :token (current-access-token)})))

(def interceptors [authorization user-agent])

(def discord (m/bootstrap api-url [{:route-name :exchange-access-token
                                    :part-parts ["/oauth2/token"]
                                    :method :post
                                    :consumes ["application/x-www-form-urlencoded"]
                                    :produces ["application/json"]
                                    :form-schema {:client_id s/Str
                                                  :client_secret s/Str
                                                  :grant_type (s/eq "authorization_code")
                                                  :code s/Str
                                                  :redirect_uri s/Str}}
                                   {:route-name :exchange-refresh-token
                                    :part-parts ["/oauth2/token"]
                                    :method :post
                                    :consumes ["application/x-www-form-urlencoded"]
                                    :produces ["application/json"]
                                    :form-schema {:client_id s/Str
                                                  :client_secret s/Str
                                                  :grant_type (s/eq "refresh_token")
                                                  :refresh_token s/Str}}
                                   (r/version {:route-name :get-current-user
                                               :path-parts ["/users/@me"]
                                               :method :get
                                               :produces ["application/json"]
                                               :interceptors [authorization]})]
               {:interceptors (concat m/default-interceptors interceptors r/http-interceptors)}))

;; Config may not be initialized, so delay execution.
(def exchange-params* (delay {:client-id (::client-id config)
                              :client-secret (::client-secret config)}))

(def exchange-params (partial deref exchange-params*))

(defn request [route params]
  (let [res (m/response-for discord route params)
        res* (if (res/unauthorized? res)
               (do
                 (save-access (:body (m/response-for discord :exchange-refresh-token (assoc (exchange-params)
                                                                                       :grant-type "refresh_token"
                                                                                       :refresh-token (current-refresh-token)))))
                 (m/response-for discord route params))
               res)]
    (:body res*)))



;; Used by the :discord deps alias.
(defn store [{:keys [code token refresh]}]
  (mount/start)
  (cond
    code (save-access (:body (m/response-for discord :exchange-access-token (assoc (exchange-params)
                                                                              :grant-type "authorization_code"
                                                                              :code code
                                                                              :redirect-uri (::redirect config)))))
    (and token refresh) (save-access {:access_token token
                                      :refresh_token refresh})
    :else (log/error "No input.")))
