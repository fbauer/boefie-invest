(ns boefie-invest.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [boefie-invest.db.fixtures :refer [database connections]]
            [noir.io :as io]
            [korma.db :refer [default-connection create-db]]
            [boefie-invest.handler :refer :all]))

(use-fixtures :each database)

(deftest test-app
  (doseq [conn connections]
    ;; Stub out the database initialization, as the test fixture
    ;; initializes the database already. 
    (with-redefs [boefie-invest.db.schema/initialized? (fn [] true)]
      ;; swap the lobos db connection
      (lobos.connectivity/with-connection conn
        ;; and also the korma db connection
        (default-connection (create-db conn))
        (init)
        (testing "main route"
          (let [response (app (request :get "/"))]
            (is (= 200 (:status response)))))
        (testing "home route"
          (let [response (app (request :get "/about"))]
            (is (= 200 (:status response)))))
        (testing "not-found route"
          (let [response (app (request :get "/invalid"))]
            (is (= 404 (:status response)))))))))
