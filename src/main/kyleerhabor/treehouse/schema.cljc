(ns kyleerhabor.treehouse.schema
  (:require
   [malli.core :as m]
   [malli.registry :as mr]))

(def registry (merge (m/base-schemas) (m/sequence-schemas) (m/type-schemas)))

#?(:cljs (mr/set-default-registry! registry))

(def id-len 4)

(def ID [:string {:min id-len
                  :max id-len}])

(def Element [:schema {:registry {::element [:catn
                                             [:name :symbol]
                                             [:props [:? [:map-of :keyword :any]]]
                                             [:children [:* [:or
                                                             [:schema [:ref ::element]]
                                                             :string]]]]}}
              [:ref ::element]])

(def parse-element (m/parser Element))
