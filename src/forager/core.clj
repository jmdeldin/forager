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

(def normalizers
  "Vector of functions to apply successively to a term."
  [clojure.string/lower-case porter/stem])

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

(defn -main [& args]
  (read-file (relative->absolute "data/queries.txt")))
