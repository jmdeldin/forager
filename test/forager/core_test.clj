(ns forager.core-test
  (:use clojure.test
        forager.core))

(deftest test-tokenization
  (testing "splitting a document into words"
    (is (= '("foo" "bar" "baz") (tokenize "foo bar baz")))
    (is (= '("foo") (tokenize "foo")))
    (is (empty? (tokenize "")))))

(deftest test-normalization
  (testing "downcases words"
    (is (= "foo" (normalize-term "FOO"))))
  (testing "stemming words"
    (is (= "argu" (normalize-term "arguing"))))
  (testing "operating on a collection of terms"
    (let [input '("FOO" "ARGUING" "BAR")
          exp '("foo" "argu" "bar")]
      (is (= exp (normalize input))))))
