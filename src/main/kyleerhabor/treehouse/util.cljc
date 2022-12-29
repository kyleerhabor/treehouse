(ns kyleerhabor.treehouse.util
  (:require
   #?@(:clj [[clojure.edn :as edn]
             [clojure.java.io :as io]])))

(def noop (constantly nil))

;; This is almost a lookup or miss, but feeding (g v) into f makes it unique.
(defn retry [f x failed? g]
  (let [v (f x)]
    (if (failed? v)
      (f (g v))
      v)))

(defn after
  "Ignores the first call to `f` by calling `first` instead."
  [first f]
  (let [called? (atom false)]
    (fn [& args]
      (let [g (if @called?
                f
                (do (reset! called? true) first))]
        (apply g args)))))

(defmacro once
  "Returns a function that evaluates `body` only once."
  [& body]
  `(partial deref (delay ~@body)))

#?(:clj
   (defn edn [source]
     (edn/read (java.io.PushbackReader. (io/reader source)))))
