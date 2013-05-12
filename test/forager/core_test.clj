(ns forager.core-test
  (:use clojure.test
        forager.core))

(deftest test-tokenization
  (testing "splitting a document into words"
    (is (= '("foo" "bar" "baz") (tokenize "foo bar baz")))))
