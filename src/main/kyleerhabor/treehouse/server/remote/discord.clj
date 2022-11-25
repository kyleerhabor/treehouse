(ns kyleerhabor.treehouse.server.remote.discord
  (:require
   [clojure.set :refer [rename-keys]]
   [clj-http.client :as http]
   [kyleerhabor.treehouse.server.db :as db]
   [datalevin.core :as d]))

(def api-base-url "https://discord.com/api")

(def api-version 10)

(def api-url (str api-base-url "/v" api-version))

(def current-user-url (str api-url "/users/@me"))

(def default-request {:as :json
                      :throw-entire-message true})

(defn request
  ([url method] (request url method {}))
  ([url method req]
   (:body (clj-http.client/request (merge default-request req {:url url
                                                               :method method})))))

(defn get-current-user [token]
  (request current-user-url :get {:oauth-token token}))

(defn current-access-token []
  (d/datom-v (db/latest-datom (d/datoms @db/conn :ave ::access-token))))

(comment
  (require '[kyleerhabor.treehouse.server.config :refer [config]])
  
  (def token-exchange-url (str api-url "/oauth2/token"))
  
  (defn exchange [params]
    (request token-exchange-url :post {:form-params (rename-keys params {:id :client_id
                                                                         :secret :client_secret
                                                                         :type :grant_type
                                                                         :redirect :redirect_uri})}))
  
  (def exchange-params {:id (:kyleerhabor.treehouse.server/discord-client-id config)
                        :secret (:kyleerhabor.treehouse.server/discord-client-secret config)
                        :redirect "http://localhost:3000/"})

  (defn save-access [res]
    (d/transact! db/conn [{::access-token (:access_token res)
                           ::refresh-token (:refresh_token res)}]))
  
  (defn access-token [code]
    (exchange (merge exchange-params {:type "authorization_code"
                                      :code code})))
  
  (defn refresh-token [refresh-token]
    (exchange (merge exchange-params {:type "refresh_token"
                                      :code refresh-token})))
  
  (defn refresh-stored-token []
    (refresh-token (d/datom-v (db/latest-datom (d/datoms @db/conn :ave ::refresh-token))))))
