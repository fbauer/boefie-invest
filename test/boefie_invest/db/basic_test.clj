(ns boefie-invest.db.basic-test
  (:require [clojure.test :refer :all]
            [boefie-invest.db.schema :refer :all]
            [lobos.connectivity]
            [boefie-invest.db.fixtures :refer :all]
            
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]))

(def test-conn-sqlite-disk {:classname "org.sqlite.JDBC"
                            :subprotocol "sqlite"
                            :subname "testdb.sqlite"
                            :make-pool? true
                            :foreign_keys 1})

(defmulti test-setup-dbs (fn [x y] (x :subprotocol)))

(defmethod test-setup-dbs "sqlite"
  [db-spec message]
  (testing message
    (swap! lobos.connectivity/global-connections {})
    (setup-dbs [db-spec])
    (is (= [{:name "isins"}
             {:name "securities"}
             {:name "shares"}
             {:name "per_share_amounts"}
             {:name "amounts"}]
           (clojure.java.jdbc/query
            (clojure.java.jdbc/add-connection
             db-spec
             ((@lobos.connectivity/global-connections :sqlite)
              :connection))
            "SELECT name FROM sqlite_master WHERE type='table';")))
    (teardown-dbs [db-spec])
    (io/delete-file "testdb.sqlite" true)))

(defmethod test-setup-dbs "h2"
  [db-spec message]
  (testing message
    (swap! lobos.connectivity/global-connections {})
    (setup-dbs [db-spec])
    (is (= [{:name "shares"}
             {:name "amounts"}
             {:name "isins"}
             {:name "securities"}
             {:name "per_share_amounts"}]
           (clojure.java.jdbc/query
            (clojure.java.jdbc/add-connection
             db-spec
             ((@lobos.connectivity/global-connections :h2)
              :connection))
            "SELECT TABLE_NAME AS NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'TABLE'; ")))
    (teardown-dbs [db-spec])))



(deftest test-setup-dbs-on-disk-sqlite
  (test-setup-dbs test-conn-sqlite-disk "init-db with on-disk sqlite db"))

(deftest test-setup-dbs-memory-sqlite
  (test-setup-dbs test-conn-sqlite "init-db with in-memory sqlite db"))

(deftest test-setup-dbs-memory-h2
  (test-setup-dbs test-conn-h2 "init-db with in-memory h2 db"))
