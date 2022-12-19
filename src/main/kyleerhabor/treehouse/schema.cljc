(ns kyleerhabor.treehouse.schema)

(def id-len 4)

(def ID [:string {:min id-len
                  :max id-len}])
