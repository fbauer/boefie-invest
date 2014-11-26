(ns boefie-invest.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [boefie-invest.handler :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))
  (testing "home route"
    (let [response (app (request :get "/about"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response))))))
