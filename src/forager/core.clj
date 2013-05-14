(ns forager.core
  (:require [clojure.java.io :as io]
            [clojure.string  :as string]
            [clojure.set]
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

(defn stem
  "Reduces a word to its root."
  [word]
  (porter/stem word))

(def normalizers
  "Vector of functions to apply successively to a term."
  [clojure.string/trim
   clojure.string/lower-case
   remove-nonalphanumerics
   stem])

(defn normalize-term
  "Applies a series of transformations to a term."
  [term]
  (reduce (fn [mod-term f] (f mod-term)) term normalizers))

(defn normalize
  "Normalizes a collection of terms."
  [terms]
  (remove empty? (map normalize-term terms)))

(defn sorted-upsert
  "Associates a key with a value stored in a sorted-set."
  [index key value]
  (let [ids (get index key (sorted-set))]
    (assoc index key (conj ids value))))

(defn index-document
  "Adds a documents terms to an inverted index and returns the index."
  [document doc-id index]
  (let [tokens (normalize (tokenize document))]
    (reduce (fn [mod-index term] (sorted-upsert mod-index term doc-id))
            index
            tokens)))

(defn index-lines
  "Indexes a list of lines."
  [lines doc-id index]
  (reduce (fn [mod-index line] (index-document line doc-id mod-index))
          index
          lines))

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

(defn conjunction
  "Private API for performing a Boolean-AND query."
  ([index terms] (conjunction index terms #{}))
  ([index terms matched-docs]
     (if (empty? terms) matched-docs
         (if-let [doc-ids (get index (stem (first terms)))]
           (recur index
                  (rest terms)
                  (if (empty? matched-docs)
                    doc-ids
                    (clojure.set/intersection matched-docs doc-ids)))
           (recur index (rest terms) #{})))))

(defn AND
  "Returns a list of documents matching all given terms."
  ([index term] (conjunction index '(term)))
  ([index term & terms] (conjunction index (conj terms term))))

(defn -main [& args]
  (let [db (make-database (relative->absolute "data/RiderHaggard/raw"))]
    (println "RESULTS")
    (println (filename->id db "/vagrant/src/forager/data/RiderHaggard/raw/Moon of Israel 2856.txt"))
    (println (id->filename db 32))))
