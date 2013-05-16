(ns forager.core-test
  (:use clojure.test
        forager.core))

(deftest test-tokenization
  (testing "splitting a document into words"
    (is (= '("foo" "bar" "baz") (tokenize "foo bar baz")))
    (is (= '("foo") (tokenize "foo")))
    (is (empty? (tokenize "")))))

(deftest test-normalization
  (testing "removes excess whitespace"
    (is (= "foo" (normalize-term "\n\r foo\t "))))
  (testing "downcases words"
    (is (= "foo" (normalize-term "FOO"))))
  (testing "removes non-alphanumeric characters"
    (is (= "foo" (normalize-term "f-oo'!"))))
  (testing "stemming words"
    (is (= "argu" (normalize-term "arguing"))))
  (testing "operating on a collection of terms"
    (let [input '("FOO" "ARGUING" "BAR")
          exp '("foo" "argu" "bar")]
      (is (= exp (normalize input)))))
  (testing "operating on a collection of ignoreable terms"
    (is (= '("foo") (normalize '("FOO!!!;" "'-.."))))))

(deftest test-sorted-upsert
  (testing "into an empty hashmap"
    (is (= {"term" #{1}} (sorted-upsert {} "term" 1))))
  (testing "into a non-empty hashmap"
    (let [existing {"term" (sorted-set 1 8)}]
      (is (= {"term" #{1 2 8}} (sorted-upsert existing "term" 2))))))

(deftest test-index-document
  (testing "indexing a short string"
    (let [doc-id 1
          exp-keys (set '("foo" "bar"))
          exp-docs #{doc-id}
          dict {}]
      (is (= exp-keys (set (keys (index-document "foo bar" doc-id dict)))))))
  (testing "indexing an empty string"
    (is (= {} (index-document "" 1 {})))))

(deftest test-index-tokens
  (is (= {"foo" #{1} "bar" #{1}} (index-tokens {} '("foo" "bar") 1))))

(deftest test-index-lines
  (testing "indexing a list of strings belonging to a document"
    (let [doc-id 1
          exp-keys (set '("foo" "bar"))]
      (is (= exp-keys (set (keys (index-lines '("foo" "bar") doc-id {}))))))))

(deftest test-conjunction
  (let [doc-set #{1}
        index   {"foo" doc-set "bar" doc-set}]
    (testing "with a singleton term list"
      (is (= doc-set (conjunction index '("foo")))))
    (testing "with multiple matching terms from the same document"
      (is (= doc-set (conjunction index '("foo" "bar")))))
    (testing "with only one match"
      (is (= #{} (conjunction index '("foo" "baz")))))))

(deftest test-disjunction
  (let [doc-set #{1}
        index   {"foo" doc-set "bar" doc-set}]
    (testing "with a singleton term list"
      (is (= doc-set (disjunction index '("foo")))))
    (testing "with multiple matching terms from the same document"
      (is (= doc-set (disjunction index '("foo" "bar")))))
    (testing "with only one match"
      (is (= doc-set (disjunction index '("foo" "baz")))))))

(deftest test-boolean-retrieval
  (let
      [doc1 "the quick brown fox jumped over the lazy dog"
       doc2 "the quick brown cow jumped over the lazy squid"
       index (index-document doc2 2 (index-document doc1 1 {}))]
    (testing "AND"
      (is (= #{1} (AND index "dog")))
      (is (= #{1} (AND index "quick" "fox")))
      (is (= #{}  (AND index "fox" "cow"))))
    (testing "OR"
      (is (= #{1 2} (OR index "fox" "cow"))))))
