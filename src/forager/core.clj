(ns forager.core
  (:require [clojure.java.io :as io]))

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

(def normalizers
  "String normalizers to apply successively to each token."
  [clojure.string/trim clojure.string/lower-case])

(defn -main [& args]
  (read-file (relative->absolute "data/queries.txt")))
