(ns kyleerhabor.treehouse.build
  (:require
   [clojure.tools.build.api :as b]))

(def lib 'kyleerhabor/treehouse)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:aliases [:server]}))
(def file (str "target/" (name lib) "-" version "-standalone.jar"))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uberjar [_]
  (clean nil)
  ;; TODO: Figure out how to require shadow without it causing errors (adding it in :deps doesn't work)
  (b/process {:command-args ["npx" "shadow-cljs" "release" "main"]})
  (b/copy-dir {:src-dirs ["src/main" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :class-dir class-dir
                  :src-dirs ["src/main"]
                  :java-opts ["--add-opens=java.base/java.nio=ALL-UNNAMED" "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]
                  :compile-opts {:disable-locals-clearing false
                                 :elide-meta [:doc :added :line :column :file]
                                 :direct-linking true}})
  (b/uber {:uber-file file
           :class-dir class-dir
           :basis basis
           :main 'kyleerhabor.treehouse.server}))
