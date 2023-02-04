(ns kyleerhabor.treehouse.build
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.build.api :as b]))

(defn edn [source]
  (edn/read (java.io.PushbackReader. (io/reader source))))

(def project (edn "resources/project.edn"))

(def lib 'kyleerhabor/treehouse)
(def version (:version project))
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
                              "project.edn"
                              "content/**"
                              "public/robots.txt"
                              "public/assets/main/css/compiled/main.css"]}}]
  (clean nil)
  (release nil)
  (b/copy-dir {:src-dirs ["src/main" "resources"]
               :include (glob include)
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src/main"]
                  :class-dir class-dir
                  :java-opts ["--add-opens=java.base/java.nio=ALL-UNNAMED" "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]
                  :compile-opts {:disable-locals-clearing false
                                 :elide-meta [:doc :added :line :column :file]
                                 :direct-linking true}})
  (b/uber {:uber-file file
           :class-dir class-dir
           :basis basis
           :main 'kyleerhabor.treehouse.server}))
