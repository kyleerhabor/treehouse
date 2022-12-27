(ns kyleerhabor.treehouse.server.remote
  (:require
   [martian.clj-http :as http]
   [martian.interceptors :as mi]
   [schema.core :as s]))

(defn authorization [f]
  {:name ::authorization
   :enter (fn [ctx]
            (let [{:keys [type token]} (f)]
              (assoc-in ctx [:request :headers "Authorization"] (str type " " token))))})

(def user-agent
  {:name ::user-agent
   :enter (fn [ctx]
            (assoc-in ctx [:request :headers "User-Agent"] "Treehouse (https://github.com/KyleErhabor/treehouse, 0.1.0)"))})

(defn prepend-version [handler]
  (concat ["/v" :version] handler))

(defn version [handler]
  (-> handler
    (update :path-parts prepend-version)
    (update :path-schema assoc :version s/Num)))

(def http-interceptors [mi/default-encode-body mi/default-coerce-response http/perform-request])
