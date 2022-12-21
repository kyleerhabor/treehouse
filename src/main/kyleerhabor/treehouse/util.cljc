(ns kyleerhabor.treehouse.util
  (:require
   #?@(:clj [[clojure.edn :as edn]
             [clojure.java.io :as io]])))

;; This is almost a lookup or miss, but feeding (g v) into f makes it unique.
(defn retry [f x failed? g]
  (let [v (f x)]
    (if (failed? v)
      (f (g v))
      v)))

#?(:clj
   (defn load-edn [source]
     (edn/read (java.io.PushbackReader. (io/reader source)))))
