(ns kyleerhabor.treehouse.shadow
  (:require
   [shadow.build :as-alias build]
   [kyleerhabor.treehouse.ui :as ui]
   [com.fulcrologic.fulcro-css.css-injection :refer [compute-css]]))

(defn css
  {::build/stage :flush}
  [{::build/keys [mode]
    :as build}]
  (let [css (compute-css {:component ui/Root
                          :garden-flags {:pretty-print? (not= :release mode)}})]
    (spit "resources/public/assets/main/css/compiled/main.css" css)
    build))
