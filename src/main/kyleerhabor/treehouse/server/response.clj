(ns kyleerhabor.treehouse.server.response)

(def status-codes {:forbidden 403
                   :method-not-allowed 405
                   :not-acceptable 406
                   :internal-server-error 500})

(def forbidden {:status (:forbidden status-codes)})

(defn method-not-allowed [methods]
  {:status (:method-not-allowed status-codes)
   :headers {"Allow" methods}})

(def not-acceptable {:status (:not-acceptable status-codes)})

(def internal-server-error {:status (:internal-server-error status-codes)})

(def doctype "<!DOCTYPE html>")
