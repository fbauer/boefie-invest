(ns flo-invest.database-test
  (:require [clojure.test :refer :all]
            [flo-invest.database :refer :all]
            [flo-invest.bigmoney :refer [as-money]]
            )
  )

(def test-conn {:classname "org.sqlite.JDBC"
                :subprotocol "sqlite"
                :subname "testdb.sqlite"})

(defn database [f]
  (do
    (try
      (kill-db test-conn)
      (catch Exception e ))
    (try (do (init-db test-conn)
             (f))
         (finally 
           (kill-db test-conn)))))

(use-fixtures :each database)

(deftest test-init-db
  (do (add-book test-conn {:title "My first book" :review "I liked it"})
      (is (= (db-read-all test-conn) {:title "My first book" :review "I liked it" :id nil}))
      ))

(deftest test-db-empty
  (testing "loading test data succeeds"
    (is (= (db-read-all test-conn) []))))
