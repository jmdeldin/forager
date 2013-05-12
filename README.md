# Forager
[![Build Status](https://travis-ci.org/jmdeldin/forager.png)](https://travis-ci.org/jmdeldin/forager)

Forager is a proof-of-concept search engine in Clojure. I am writing it
for my NLP independent study's final project. The goal is to get a
nuts-and-bolts understanding of language modeling (n-grams and
tokenizing), indexing with term frequency-inverse document frequency
(tf-idf), and retrieving documents with Boolean queries.

## Features

- interactive interface via a Clojure REPL
- indexing of documents via tf-idf
- Boolean query operators (`AND`, `OR`, `NOT`)
  - `NEAR` or `WITHIN` a certain distance (low priority)
- methods to evaluate information retrieval precision and recall

### Optional Features

If time allows, these features would be nice to have:

- compressed indexing
- `NEAR` or `WITHIN` a certain distance operators

## Data

Initially, Forager will work on the plain-text short stories of Rider
Haggard, as the Coursera NLP course provides the data and a few sample
queries to evaluate Forager on.

If time allows, implementing support for one of the following data sets:

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

## Background

### Indexing

In small-scale information retrieval problems, one can create a matrix
of documents and term frequencies. However, this requires a lot of
memory to construct a matrix that's the #documents by all keywords. An
alternative, is to create an inverted index, which is a dictionary of
keywords, where each keyword points to a sorted list of document IDs.

#### `WITHIN` Operator

One way to implement a k-word proximity search is with a
divide-and-conquer approach, as described by
[this article](http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.26.3610).
The algorithm proposed in that article is:

1. Find the median *v* between the two keywords
2. Scan the list of keyword positions and divide the list into two
smaller lists, L (positions < *v*) and R (positions > *v*). Keep the
largest positions of each keyword in L and the smallest positions of
each keyword in R.
3. Find the minimal intervals which lie on both L and R with the
plane-sweep algorithm (this scans from left-to-right and finds intervals
[left-start, right-end] containing all keywords).
4. If L or R contains all *k* kewords, recursively find minimal
intervals in that list.

## References

- [original tf-idf paper](http://dl.acm.org/citation.cfm?id=358466)
- [comparison of tf-idf interpretations](http://dl.acm.org/citation.cfm?id=1390334.1390409)
- [additional historical references](http://nlp.stanford.edu/IR-book/html/htmledition/references-and-further-reading-6.html)
- [Google's original paper](http://infolab.stanford.edu/~backrub/google.html)
- [Text retrieval by using k-word proximity search](http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.26.3610)

### Evaluation

- Performance on Coursera data set
- Performance on test queries

## Author

Jon-Michael Deldin, `dev@jmdeldin.com`.
