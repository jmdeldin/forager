(ns forager.core
  (:require [clojure.java.io :as io]
            [clojure.string  :as string]))

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
  (string/split document #"\s+"))

(defn -main [& args]
  (read-file (relative->absolute "data/queries.txt")))
