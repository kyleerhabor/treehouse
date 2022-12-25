(ns kyleerhabor.treehouse.build
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.build.api :as b]))

(def lib 'kyleerhabor/treehouse)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:aliases [:server]}))
(def file (str "target/" (name lib) "-" version "-standalone.jar"))

(def compiled "public/assets/main/js/compiled")
(def manifest (str compiled "/manifest.edn"))

(defn edn [source]
  (edn/read (java.io.PushbackReader. (io/reader source))))

(defn glob
  "Produces a glob string matching a number of patterns in the `patterns` collection."
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
                              ;; Used by the server to resolve the main JS file.
                              manifest]}}]
  (clean nil)
  (release nil)
  ;; The main JS file is hashed and we only want the file produced for this release.
  (let [module (first (edn (io/resource manifest)))
        main (str compiled "/" (:output-name module))
        include (conj include main (str main ".map"))]
    (b/copy-dir {:src-dirs ["src/main" "resources"]
                 :include (glob include)
                 :target-dir class-dir}))
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
