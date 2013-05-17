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

(defn index-tokens
  "Indexes a list of already normalized tokens."
  [index tokens doc-id]
  (reduce (fn [mod-index token]
            (sorted-upsert mod-index token doc-id))
          index
          tokens))

(defn list-directory
  "Returns a list of all files in a directory."
  [directory]
  ;; file-seq is lazy -- we want all of the files at once
  (doall (filter (fn [f] (.isFile f)) (file-seq (io/file directory)))))

(defstruct Database :ids :filenames :last-id :vocabulary :index :documents)

(defn make-database [&{:keys [ids filenames last-id vocabulary index documents]
                       :or {ids {} filenames {} last-id 0 vocabulary #{} index {} documents {}}}]
  {:ids ids :filenames filenames :last-id last-id :vocabulary vocabulary :index index :documents documents})

(defn index-directory
  [directory]
  (reduce (fn [mod-db file]
            (println "Indexing" (str file))
            (let [new-id (inc (get mod-db :last-id))
                  filename (str file)
                  new-ids (assoc (get mod-db :ids) new-id filename)
                  new-filenames (assoc (get mod-db :filenames) filename new-id)
                  lines (read-file filename)
                  tokens (flatten (map #(normalize (tokenize %)) lines))
                  new-documents (assoc (get mod-db :documents) new-id tokens)
                  new-index (index-tokens (get mod-db :index) tokens new-id)
                  new-vocabulary (clojure.set/union (get mod-db :vocabulary)
                                                    (set (keys new-index)))
              ]
              (make-database
               :ids new-ids
               :filenames new-filenames
               :last-id new-id
               :index new-index
               :vocabulary new-vocabulary
               :documents new-documents)

            ))
          (make-database)
          (list-directory directory)))

(defn filename->id
  "Returns a filename's ID."
  [database filename]
  (get (get database :filenames) filename))

(defn id->filename
  "Returns an ID's filename."
  [database id]
  (get (get database :ids) id))

(defn retrieve
  "Private API for performing a Boolean queries.

  handle-match is a function(old-matches, new-matches) called when a doc-id is
  found for a stemmed term.

  handle-no-docs is a function(old-matches) called when no documents are found
  for a stemmed term."
  [index terms handle-match handle-no-docs]
  ((fn [term-xs matches]
    (if (empty? term-xs)
      matches
      (if-let [doc-ids (get index (stem (first term-xs)))]
        (recur (rest term-xs) (if (empty? matches) doc-ids
                                (handle-match matches doc-ids)))
        (recur (rest term-xs) (handle-no-docs matches)))))
   terms
   #{}))

(defn conjunction
  "Private API for performing a Boolean-AND query."
  [index terms]
  (retrieve index terms
            (fn [old-ids new-ids] (clojure.set/intersection old-ids new-ids))
            (fn [old-ids] #{})))

(defn disjunction
  "Private API for performing a Boolean-OR query."
  [index terms]
  (retrieve index terms
            (fn [old-ids new-ids] (clojure.set/union old-ids new-ids))
            (fn [old-ids] old-ids)))

(defn run-boolean-query
  [index term terms function]
  (function index (conj terms term)))

(defn AND
  "Returns a list of documents matching all given terms."
  [index term & terms]
  (run-boolean-query index term terms conjunction))

(defn OR
  "Returns a list of documents any given terms."
  [index term & terms]
  (run-boolean-query index term terms disjunction))

(defn fetch-token-frequencies
  "coll -- hash of term => {doc-id => value}"
  [coll doc-id tokens]
  (reduce (fn [mod-coll token->count]
            (let [token (first token->count)
                  old-doc-hash (get mod-coll token {})
                  old-counts (get old-doc-hash doc-id 0)
                  new-counts (+ old-counts (last token->count))
                  new-doc-hash (assoc old-doc-hash doc-id new-counts)
                  ]
              (assoc mod-coll token new-doc-hash)))
          coll (frequencies tokens)))

(defn fetch-tf
  [database]
  (reduce (fn [mod-tf doc-id]
            (fetch-token-frequencies mod-tf doc-id (get-in database [:documents doc-id])))
          {}
          (keys (get database :documents))))

(defn compute-df
  [tf-hash term]
  (float (count (get tf-hash term))))

(defn compute-idf
  [tf-hash term]
  (let [num-docs (float (count #{vals tf-hash}))]
    (Math/log10 (/ num-docs (compute-df tf-hash term)))))

(defn tf-idf
  [tf-hash term doc-id]
  (let [tf (get-in tf-hash [term doc-id])]
    (* tf (compute-idf tf-hash term))))
