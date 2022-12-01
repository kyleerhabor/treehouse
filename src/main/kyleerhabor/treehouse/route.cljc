(ns kyleerhabor.treehouse.route
  (:require
   #?@(:clj [[clojure.string :as str]
             [kyleerhabor.treehouse.server.handler :refer [api-handler page-handler]]
             [kyleerhabor.treehouse.server.response :refer [internal-server-error]]
             [com.fulcrologic.fulcro.server.api-middleware :as s :refer [wrap-transit-params wrap-transit-response]]])
   #?(:cljs [reitit.frontend :as rf])
   #?@(:clj [[reitit.ring :as rr]
             [reitit.ring.middleware.exception :as rrex]
             [ring.util.response :as res]])))

;; NOTE: The compiler complains about invalid keywords even when :as or :as-alias is used, so I'm using their full
;; qualified value. Would like to fix.

#?(:clj
   (defn allowed
     "Returns a comma-separated string of the request methods supported by a request."
     [request]
     (->> (:result (rr/get-match request))
       (filter val)
       (map #(str/upper-case (name (key %))))
       (str/join ", "))))

(def routes [["/" {:name :home
                   #?@(:clj [:get page-handler])}]
             ["/api" {:name :api
                      #?@(:clj [:post {:handler api-handler
                                       :middleware [[:transit-params]
                                                    [:transit-response]]}])}]
             ["/projects" {:name :projects
                           #?@(:clj [:get page-handler])}]])

#?(:clj (def exception-middleware (rrex/create-exception-middleware {:reitit.ring.middleware.exception/default (constantly internal-server-error)})))

(def router (#?(:clj rr/router
                :cljs rf/router) routes
                                 #?(:clj {:reitit.ring/default-options-endpoint {:handler (comp res/response allowed)}
                                          :reitit.middleware/registry {:exception exception-middleware
                                                                       :transit-params [wrap-transit-params {:malformed-response (res/bad-request nil)}]
                                                                       :transit-response wrap-transit-response}
                                          :data {:middleware [:exception]}})))
