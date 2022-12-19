(ns kyleerhabor.treehouse.client.query
  (:require
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]))

(def registry [])

(def parser (p/async-parser {::p/env {::p/reader [p/map-reader pc/reader2 pc/ident-reader pc/index-reader]
                                      ::pc/mutation-join-globals [:tempids]}
                             ::p/mutate pc/mutate
                             ::p/plugins [(pc/connect-plugin {::pc/register registry})
                                          p/error-handler-plugin
                                          p/elide-special-outputs-plugin]}))

(defn parse [query]
  (parser {} query))
