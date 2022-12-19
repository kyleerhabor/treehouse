(ns kyleerhabor.treehouse.server.remote.github
  (:require
   [clojure.data.json :as json]
   [kyleerhabor.treehouse.server.config :refer [config]]
   [graphql-query.core :as g]
   [clj-http.client :as http]))

(def api-url "https://api.github.com/graphql")

(defn query [q]
  (:body (http/post api-url {:body (json/write-str {:query (g/graphql-query q)})
                             ;; This is actually a personal access token, but :oauth-token produces the correct header.
                             :oauth-token (::token config)
                             :as :json})))

(defn viewer []
  (:viewer (:data (query {:queries [[:viewer
                                     [:id :url]]]}))))
