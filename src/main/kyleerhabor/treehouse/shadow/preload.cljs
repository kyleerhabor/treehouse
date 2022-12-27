(ns kyleerhabor.treehouse.shadow.preload
  (:require
   [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
   [taoensso.timbre :as log]))

(log/set-min-level! :debug)
(log/merge-config! {:output-fn prefix-output-fn
                    :appenders {:console (console-appender)}})
