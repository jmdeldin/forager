# Forager

Forager is a proof-of-concept search engine in Clojure. I am writing it
for my NLP independent study's final project. The goal is to get a
nuts-and-bolts understanding of language modeling (n-grams and
tokenizing), indexing with term frequency-inverse document frequency
(tf-idf), and retrieving documents with Boolean queries.

## Features

- interactive interface via a Clojure REPL
- indexing of documents via tf-idf
  - compression (low priority)
- Boolean query operators (`AND`, `OR`, `NOT`)
  - `NEAR` or `WITHIN` a certain distance (low priority)
- methods to evaluate information retrieval precision and recall

## Data

Initially, Forager will only support one of the following repositories:

- Plain text from [Project Gutenberg](http://www.gutenberg.org)
- [Wikipedia's data dump](http://en.wikipedia.org/wiki/Wikipedia:Database_download)
- [Reuters-21578 data set](http://www.daviddlewis.com/resources/testcollections/reuters21578/)

## Interface

I haven't put much thought towards this yet, but I imagine interaction
will be something like this, returning the document identifier and an
excerpt in some kind of structure:

```clojure
(index "path/to/reut2-000.sgm")

(query "butter")
;; => FROM `EC MINISTERS CONSIDER BIG AGRICULTURE PRICE CUTS'
;; => "Routine sales of BUTTER were made."

(query (AND "butter" "cereal"))
;; => FROM `EC MINISTERS CONSIDER BIG AGRICULTURE PRICE CUTS'
;; => "...16 mln tonnes of unwanted CEREALS, over one mln tonnes of BUTTER..."
```

## References

- [original tf-idf paper](http://dl.acm.org/citation.cfm?id=358466)
- [comparison of tf-idf interpretations](http://dl.acm.org/citation.cfm?id=1390334.1390409)
- [additional historical references](http://nlp.stanford.edu/IR-book/html/htmledition/references-and-further-reading-6.html)
- [Google's original paper](http://infolab.stanford.edu/~backrub/google.html)

## Author

Jon-Michael Deldin, `dev@jmdeldin.com`.
