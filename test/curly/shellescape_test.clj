(ns curly.shellescape-test
  (:require
    [clojure.test :refer [testing deftest is]]
    [curly.shellescape :as shellescape]
    ))


(deftest quote-str-test
  (testing "simple"
    (is (= "foobar" (shellescape/quote-str "foobar")))
    (is (= "'foo'\"'\"'bar'" (shellescape/quote-str "foo'bar")))
    (is (= "'foo=1&bar=2'" (shellescape/quote-str "foo=1&bar=2")))
    (is (= "'filename; rm -rf /'" (shellescape/quote-str "filename; rm -rf /")))
    )
  )
