(ns boefie-invest.handler-test
  (:require [clojure.test :refer :all]
            [lobos.connectivity :refer :all]
            [ring.mock.request :refer :all]
            [boefie-invest.db.fixtures :refer [database]]
            [boefie-invest.handler :refer :all]))

(use-fixtures :each database)

(deftest test-app
  (doseq [conn (keys @global-connections)]
         (lobos.connectivity/with-connection conn
           (testing "main route"
             (let [response (app (request :get "/"))]
               (is (= 200 (:status response)))))
           (testing "home route"
             (let [response (app (request :get "/about"))]
               (is (= 200 (:status response)))))

           (testing "not-found route"
             (let [response (app (request :get "/invalid"))]
               (is (= 404 (:status response))))))))
