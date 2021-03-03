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
    (is (= {:r/body {:foo :baz}}
           (curly/reduce-opts {} [":foo" ":baz"])))
    (is (= {:r/body "baz"}
           (curly/reduce-opts {} [":r/body" "baz"])))
    (is (= {:r/body {"foo" "baz"}}
           (curly/reduce-opts {} ["foo" "baz"])))
    (is (= {:r/body {:foo {:bar "baz"}}}
           (curly/reduce-opts {} ["[:foo,:bar]" "baz"])))
    )
  )
(deftest encode-body-test
  (testing "json"
    (is (= "{\"foo\":\"bar\"}"
           (curly/encode-body {:foo :bar} {:Content-Type "application/json"})))
    (is (= "@body.json"
           (curly/encode-body "@body.json" {:Content-Type "application/json"})))
    )
  (testing "form-params"
    (is (= "foo=bar&baz=qux&ns/key=sym&int=23&float=2.2&ratio=2/3&boolean=false"
           (curly/encode-body {:foo :bar :baz "qux" :ns/key 'sym :int 23 :float 2.20 :ratio (/ 2 3) :boolean false} {})))
    (is (= "foo=bar&baz=qux"
           (curly/encode-body "foo=bar&baz=qux" {})))
    )
  )
