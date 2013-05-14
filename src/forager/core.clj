(ns forager.core
  (:require [clojure.java.io :as io]
            [clojure.string  :as string]
            [stemmers.porter :as porter]))

(defn read-file
  "Reads a file into a list."
  [filename]
  (if (.exists (io/file filename))
    (with-open [rdr (io/reader filename)]
      (doall (line-seq rdr)))))

(defn relative->absolute
  "Returns absolute path of a relative file."
  [filename]
  (str (.getCanonicalPath (io/file "." filename))))

(defn tokenize
  "Splits a document into a sequence of terms."
  [document]
  (if (= (.length document) 0)
    []
    (string/split document #"\s+")))

(defn remove-nonalphanumerics
  "Retains only A-Z and 0-9 characters."
  [term]
  (clojure.string/replace term #"[^a-zA-Z0-9]" ""))

(def normalizers
  "Vector of functions to apply successively to a term."
  [clojure.string/trim
   clojure.string/lower-case
   remove-nonalphanumerics
   porter/stem])

(defn normalize-term
  "Applies a series of transformations to a term."
  [term]
  (reduce (fn [mod-term f] (f mod-term)) term normalizers))

(defn normalize
  "Normalizes a collection of terms."
  [terms]
  (map normalize-term terms))

(defn sorted-upsert
  "Associates a key with a value stored in a sorted-set."
  [index key value]
  (if-let [ids (get index key)]
    (assoc index key (conj ids value))
    (assoc index key (sorted-set value))))

(defn index-document
  "Adds a documents terms to an index and returns the index."
  [document doc-id index]
  (let [tokens (normalize (tokenize document))]
    (reduce (fn [mod-index term] (sorted-upsert mod-index term doc-id))
            index
            tokens)))

(defn list-directory
  "Returns a list of all files in a directory."
  [directory]
  ;; file-seq is lazy -- we want all of the files at once
  (doall (file-seq (io/file directory))))

(defstruct Database :ids :filenames :last-id)

(defn make-database
  "Returns a bidirectional struct enabling both ID and filename lookups."
  [directory]
  (reduce (fn [mod-db file]
            (let [new-id (inc (get mod-db :last-id 0))
                  filename (str file)]
              (struct Database
                      (assoc (get mod-db :ids) new-id filename)
                      (assoc (get mod-db :filenames) filename new-id)
                      new-id)))
          (struct Database {} {} 0)
          (list-directory directory)))

(defn filename->id
  "Returns a filename's ID."
  [database filename]
  (get (get database :filenames) filename))

(defn id->filename
  "Returns an ID's filename."
  [database id]
  (get (get database :ids) id))

(defn -main [& args]
  (let [db (make-database (relative->absolute "data/RiderHaggard/raw"))]
    (println "RESULTS")
    (println (filename->id db "/vagrant/src/forager/data/RiderHaggard/raw/Moon of Israel 2856.txt"))
    (println (id->filename db 32))))
