(ns kyleerhabor.treehouse.server.remote.discord
  (:require
   [clojure.set :refer [rename-keys]]
   [clj-http.client :as http]
   [kyleerhabor.treehouse.server.config :refer [config]]
   [kyleerhabor.treehouse.server.database :as db]
   [kyleerhabor.treehouse.server.response :refer [unauthorized?]]
   [kyleerhabor.treehouse.util :refer [retry]]
   [datalevin.core :as d]))

(def api-base-url "https://discord.com/api")

(def api-version 10)

(def api-url (str api-base-url "/v" api-version))

(def current-user-url (str api-url "/users/@me"))

(def token-exchange-url (str api-url "/oauth2/token"))

(def default-request {:as :json
                      ;; TODO: Don't hardcode this.
                      :headers {"User-Agent" "Treehouse (https://github.com/KyleErhabor/treehouse, 0.1.0)"}
                      :throw-exceptions false})

;; TODO: Make requesting more data-oriented.

(defn request
  ([url method] (request url method {}))
  ([url method req]
   (http/request (merge default-request req {:url url
                                             :method method}))))

(defn exchange [params]
  (request token-exchange-url :post {:form-params (rename-keys params {:id :client_id
                                                                       :secret :client_secret
                                                                       :type :grant_type
                                                                       :redirect :redirect_uri})}))

(def exchange-params {:id (::client-id config)
                      :secret (::client-secret config)
                      :redirect "http://localhost:3000/"})

(defn refresh-token [token]
  (:body (exchange (merge exchange-params {:type "refresh_token"
                                           :refresh_token token}))))

(defn get-current-user [token]
  (request current-user-url :get {:oauth-token token
                                  :throw-exceptions false}))

(defn current-access-token []
  (d/datom-v (db/latest-datom (d/datoms @db/conn :ave ::access-token))))

(defn current-refresh-token []
  (d/datom-v (db/latest-datom (d/datoms @db/conn :ave ::refresh-token))))

(defn save-access [res]
  (d/transact! db/conn [{::access-token (:access_token res)
                         ::refresh-token (:refresh_token res)}]))

(defn retry-token [f token refresh]
  (:body (retry f token unauthorized? refresh)))

(defn use-token [f]
  (retry-token f (current-access-token) (fn [_]
                                          (let [res (refresh-token (current-refresh-token))]
                                            (save-access res)
                                            (:access_token res)))))

(comment
  ;; TODO: Allow this function to be called in some "install" process (e.g. a deps alias)
  (defn access-token [code]
    (exchange (merge exchange-params {:type "authorization_code"
                                      :code code})))
  
  (defn refresh-stored-token []
    (save-access (refresh-token (current-refresh-token)))))
