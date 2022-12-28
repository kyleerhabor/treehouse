(ns kyleerhabor.treehouse.schema
  (:require
   [malli.core :as m]))

(def Element [:schema {:registry {::element [:catn
                                             [:name :symbol]
                                             [:props [:? [:map-of :keyword :any]]]
                                             [:children [:* [:or
                                                             [:schema [:ref ::element]]
                                                             :string]]]]}}
              [:ref ::element]])

(def parse-element (m/parser Element))
