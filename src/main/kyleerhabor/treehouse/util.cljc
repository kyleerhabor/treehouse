(ns kyleerhabor.treehouse.util
  (:require
   #?@(:clj [[clojure.edn :as edn]
             [clojure.java.io :as io]])))

#?(:clj
   (defn load-edn [source]
     (edn/read (java.io.PushbackReader. (io/reader source)))))
