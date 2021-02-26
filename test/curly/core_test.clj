(ns curly.core-test
  (:require
    [clojure.test :refer [testing deftest is]]
    [curly.core :as curly]
    ))

(deftest reduce-opts-test
  (testing "simple"
    (is (= {:c/foo :bar}
           (curly/reduce-opts {} [":c/foo" ":bar"])))
    (is (= {:c/foo "bar"}
           (curly/reduce-opts {} [":c/foo" "bar"])))
    (is (= {:c/foo {:bar :baz}}
           (curly/reduce-opts {} ["[:c/foo,:bar]" ":baz"])))
    )
  (testing "auto-body"
    (is (= {:c/body {:foo :baz}}
           (curly/reduce-opts {} [":foo" ":baz"])))
    (is (= {:c/body {"foo" "baz"}}
           (curly/reduce-opts {} ["foo" "baz"])))
    (is (= {:c/body {:foo {:bar "baz"}}}
           (curly/reduce-opts {} ["[:foo,:bar]" "baz"])))
    )
  )
