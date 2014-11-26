(ns boefie-invest.db.schema-test
  (:require [clojure.test :refer :all]
            [boefie-invest.db.schema :refer :all]
            [boefie-invest.db.fixtures :refer [database connections]]
            [boefie-invest.bigmoney :refer [as-money]]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-sql-time to-date-time]]
            [clojure.java.io :refer [resource]])
  (:import [java.sql SQLException]))

(use-fixtures :each database)

(defn- try-insert!
  [db-spec table row]
  (try (jdbc/insert! db-spec table row :entities (jdbc/quoted \"))
       (catch java.sql.SQLException e
         (if (not (or (= (.getSQLState e) "23505")
                      (re-find #"UNIQUE constraint" (.getMessage e))))
           (throw e)))))

(defn add-security [db-spec sec]
  (do (try-insert! db-spec :isins {:isin (sec :isin)})
      (try-insert! db-spec :securities (assoc sec :date_added (to-sql-time (:date_added sec))))))

(defn db-read-all
  [db-spec]
  (for [row (jdbc/query db-spec ["SELECT * FROM \"securities\""])]
    (assoc row :date_added (to-date-time (:date_added row)))))

(deftest test-init-db
  (doseq [test-conn connections]
    (if (= (:subprotocol test-conn) "sqlite")
      (is (= '({:foreign_keys 1}) (jdbc/query test-conn ["PRAGMA foreign_keys;"]))))
    (let [row {:name "revenue" :isin "de1234567890" :date_added (date-time 2013 01 01 13 59 12)}]
      (do (add-security test-conn row)
          (let [result (vec (db-read-all test-conn))]
            (is (= org.joda.time.DateTime (class ((result 0) :date_added))))
            (is (= [(assoc row :id 1)] result)))))))

(deftest test-constraints
  (testing "It is not allowed to leave out either one of name, isin or
date_added when adding a new security to the database"
    (doseq [test-conn connections]
      (are [row] (thrown? SQLException (add-security test-conn row))
           {:name "revenue"}
           {:isin "de1234567890"}
           {:name "revenue" :isin "de1234567890"}))))

(deftest test-db-empty
  (doseq [test-conn connections]
    (is (= [] (db-read-all test-conn)))))

(deftest uniqueness-constraint
  (doseq [test-conn connections]
    (testing "can't insert a row that only differs in the date added"
      (let [row1 {:name "revenue" :isin "de1234567890" :date_added (date-time 2012 1 1)}
            row2 {:name "revenue" :isin "de1234567890" :date_added (date-time 2012 1 1)}]
        (add-security test-conn row1)
        (add-security test-conn row2)
        (let [result (vec (db-read-all test-conn))]
          (is (= result [(assoc row1 :id 1)])))))))
