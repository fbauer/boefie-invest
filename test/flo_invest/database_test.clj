(ns flo-invest.database-test
  (:require [clojure.test :refer :all]
            [flo-invest.database :refer :all]
            [flo-invest.bigmoney :refer [as-money]]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-string]]
            [clojure.java.jdbc :as sql])
  (:import [java.sql BatchUpdateException SQLException]
           [java.util]))

(def test-conn {:classname "org.sqlite.JDBC"
                :subprotocol "sqlite"
                :subname "testdb.sqlite"
                :foreign_keys 1})

(defn database [f]
  (do
    (try
      (kill-db test-conn)
      (catch BatchUpdateException e) )
    (try (do (init-db test-conn)
             (f))
         (finally
           (try
             (kill-db test-conn)
             (catch BatchUpdateException e))))))

(use-fixtures :each database)

(deftest test-init-db
  (is (= (query test-conn ["PRAGMA foreign_keys;"]) '({:foreign_keys 1})))
  (let [row {:name "revenue" :isin "de1234567890" :date_added (date-time 2013 01 01 13 59 12)}]
    (do (add-security test-conn row)
        (let [result (vec (db-read-all test-conn))]
          (is (= (class ((result 0) :date_added)) String))
          (is (= result [(assoc  row :date_added (to-string (row :date_added)) :id 1)]))))))

(deftest test-constraints
  "It is not allowed to leave out either one of name, isin or
date_added when adding a new security to the database"
  (are [row] (thrown? SQLException (add-security test-conn row))
       {:name "revenue"}
       {:isin "de1234567890"}
       {:name "revenue" :isin "de1234567890"}))

(deftest test-db-empty
  "loading test data succeeds"
  (is (= (db-read-all test-conn) [])))

(deftest uniqueness-constraint
  (testing "can't insert a row that only differs in the date added"
    (let [row1 {:name "revenue" :isin "de1234567890" :date_added (date-time 2012 1 1)}
          row2 {:name "revenue" :isin "de1234567890" :date_added (date-time 2012 1 1)}]
      (add-security test-conn row1)
      (add-security test-conn row2)
      (let [result (vec (db-read-all test-conn))]
        (is (= result [(assoc row1 :date_added (to-string (row1 :date_added)) :id 1)]))))))

(defn mytest
  [order]
  (testing (str  "Test database state. Insert order" order)
    ;; Insert security date for ACME 2012-1-1
    ;; Add a new company also called ACME on 2012-5-1
    ;; Rename first ACME to Silly corp on 2013-1-1
    (let [rows [{:name "ACME corp"
                 :isin "de1234567890" :date_added (date-time 2012 1 1)}
                {:name "ACME corp"
                 :isin "de1234567891" :date_added (date-time 2012 5 1)}
                {:name "Silly corp (formerly ACME)"
                 :isin "de1234567890" :date_added (date-time 2013 1 1)}]
          [res1 res2 res3] (for [[row num] (map vector rows (map (zipmap order [1 2 3]) [0 1 2]))]
                             (assoc row :date_added (to-string (row :date_added)) :id num))]
      (is (= (set (db-read-all test-conn)) #{}) "Sanity check that db is empty. Must never fail.")
      (doseq [row (map rows order)] (add-security test-conn row))
      (is (= (set (db-read-all test-conn)) #{res1 res2 res3}))
      (is (= (set (db-read-date test-conn (date-time 2011))) #{}))
      (is (= (set (db-read-date test-conn (date-time 2012 2 1))) #{res1}))
      (is (= (set (db-read-date test-conn (date-time 2012 6 1))) #{res1 res2}))
      (is (= (set (db-read-date test-conn (date-time 2013 6 1))) #{res2 res3})))))

(deftest query-date-snapshots-0-1-2 (mytest [0 1 2]))
(deftest query-date-snapshots-0-2-1 (mytest [0 2 1]))
(deftest query-date-snapshots-1-0-2 (mytest [1 0 2]))
(deftest query-date-snapshots-1-2-0 (mytest [1 2 0]))
(deftest query-date-snapshots-2-0-1 (mytest [2 0 1]))
(deftest query-date-snapshots-2-1-0 (mytest [2 1 0]))
