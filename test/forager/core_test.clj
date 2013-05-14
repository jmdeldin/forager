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

(deftest test-index-lines
  (testing "indexing a list of strings belonging to a document"
    (let [doc-id 1
          exp-keys (set '("foo" "bar"))]
      (is (= exp-keys (set (keys (index-lines '("foo" "bar") doc-id {}))))))))
