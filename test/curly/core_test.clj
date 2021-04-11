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
    (is (= {:r/body {:other/foo :baz}}
           (curly/reduce-opts {} [":other/foo" ":baz"])))
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

(def test-hosts
  {:prod "https://prod.com"
   :qa "https://qa.com"})

(deftest curl-command-generation
  (testing "blank"
    (is (= ["curl" "-s"]
           (curly/command->curl {} {}))))

  (testing "curl-opts"
    (is (= ["curl" "-v"]
           (curly/command->curl {:c/verbose true} {}))))

  (testing "hosts"
    (is (= ["curl" "-s" "https://prod.com/foo"]
           (curly/command->curl {:r/host :prod :r/path "/foo"} test-hosts)))
    (is (= ["curl" "-s" "https://qa.com/foo"]
           (curly/command->curl {:r/host :qa :r/path "/foo"} test-hosts)))
    (is (= ["curl" "-s" "https://one-off.com/foo"]
           (curly/command->curl {:r/host "https://one-off.com" :r/path "/foo"} test-hosts)))
    (is (= ["curl" "-s" "https://full-url.com/another"]
           (curly/command->curl {:r/url "https://full-url.com/another"} test-hosts)))
    )

  (testing "methods"
    (is (= ["curl" "-s" "-X" "POST" "https://prod.com/post"]
           (curly/command->curl {:r/host :prod :r/path "/post" :r/method :post} test-hosts)))
    (is (= ["curl" "-s" "-d" "foo=bar" "https://prod.com/post"]
           (curly/command->curl {:r/host :prod :r/path "/post" :r/method :post :r/body {:foo :bar}} test-hosts)))
    (is (= ["curl" "-s" "-X" "PUT" "-d" "foo=bar" "https://prod.com/put"]
           (curly/command->curl {:r/host :prod :r/path "/put" :r/method :put :r/body {:foo :bar}} test-hosts)))
    (is (= ["curl" "-s" "https://prod.com/get"]
           (curly/command->curl {:r/host :prod :r/path "/get" :r/method :get :r/body {:foo :bar}} test-hosts)))

    )

  (testing "headers"
    (is (= ["curl" "-s"
            "-d" "{\"foo\":\"bar\"}"
            "-H" "Content-Type: application/json"
            "-H" "Authorization: foo/bar"
            "-H" "Content-Length: 1524"
            "https://prod.com/post"]
           (curly/command->curl
             {:r/host :prod
              :r/path "/post"
              :r/method :post
              :r/headers {:Content-Type "application/json"
                          "Authorization" :foo/bar
                          'Content-Length 1524
                          }
              :r/body {:foo :bar}}
             test-hosts)))
    )

  (testing "body encode"
    (is (= ["curl" "-s" "-d" "foo=bar" "https://prod.com/post"]
           (curly/command->curl {:r/host :prod :r/path "/post" :r/method :post :r/body {:foo :bar}} test-hosts)))
    (is (= ["curl" "-s"
            "-d" "{\"foo\":\"bar\"}"
            "-H" "Content-Type: application/json"
            "https://prod.com/post"]
           (curly/command->curl
             {:r/host :prod
              :r/path "/post"
              :r/method :post
              :r/headers {:Content-Type "application/json"}
              :r/body {:foo :bar}}
             test-hosts)))
    )
  )
