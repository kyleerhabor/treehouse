(ns kyleerhabor.treehouse.build
  (:require
   [clojure.string :as str]
   [clojure.tools.build.api :as b]))

(def lib 'kyleerhabor/treehouse)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:aliases [:server]}))
(def file (str "target/" (name lib) "-" version "-standalone.jar"))

(defn glob
  "Returns a glob string matching patterns in `patterns`. Cannot contain curly brackets."
  [patterns]
  (str "{" (str/join "," patterns) "}"))

(defn clean [_]
  (b/delete {:path "target"}))

(defn release [_]
  ;; TODO: Figure out how to require shadow without it causing errors (adding it in :deps doesn't work)
  (b/process {:command-args ["npx" "shadow-cljs" "release" "main"]}))

(defn uberjar [{:keys [include]
                :or {include ["kyleerhabor/**"
                              "content/**"
                              "articles/**"
                              "public/robots.txt"
                              "public/assets/main/js/compiled/main.js"
                              "public/assets/main/js/compiled/main.js.map" ; For debugging in production.
                              "public/assets/main/css/compiled/stylo.css"]}}]
  (clean nil)
  (release nil)
  ;; The main JS file is hashed and we only want the file produced for this release.
  (b/copy-dir {:src-dirs ["src/main" "resources"]
               :include (glob include)
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
