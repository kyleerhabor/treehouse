(ns kyleerhabor.treehouse.shadow
  (:require
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro-css.css-injection :refer [compute-css]]
   [shadow.build :as-alias build]))

;; It may be better to embed the CSS in production rather than link it, but that has no caching benefits.
(defn css
  {::build/stage :flush}
  [{::build/keys [mode]
    :as build}]
  (let [css (compute-css {:component ui/Root
                          :garden-flags {:pretty-print? (not= :release mode)}})]
    (spit "resources/public/assets/main/css/compiled/main.css" css)
    build))
