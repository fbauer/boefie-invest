(ns boefie-invest.database-test
  (:require [clojure.test :refer :all]
            [boefie-invest.db.schema :refer :all]
            [boefie-invest.bigmoney :refer [as-money]]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-string to-sql-time
                                     to-date-time from-sql-time]]
            [clojure.java.io :refer [resource]])
  (:import [java.sql BatchUpdateException SQLException]
           [java.util]))

(def test-conn-sqlite {:classname "org.sqlite.JDBC"
                       :subprotocol "sqlite"
                       :subname "testdb.sqlite"
                       :foreign_keys 1})

(def test-conn-h2 {:classname "org.h2.Driver"
                   :subprotocol "h2"
                   :subname (str (resource "resources/") "testdb.h2")
                   :user "sa"
                   :password ""
                   :make-pool? true
                   :naming {:keys clojure.string/lower-case
                            :fields clojure.string/upper-case}})

(def connections [test-conn-sqlite
                  test-conn-h2  ; FIXME: correct sql for h2 database
                  ])

(defn setup-dbs
  [connections]
  (doseq [conn connections]
    (init-db conn)))

(defn teardown-dbs
  [connections]
  (doseq [conn connections]
    (try
      (kill-db conn)
      (catch BatchUpdateException e (println e)))))

(defn database [f]
  (do
    (teardown-dbs connections)
    (try (do (setup-dbs connections)
             (f))
         (finally
           (teardown-dbs connections)))))

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

(defn db-read-date
  [db-spec read-date]
  (for [row (jdbc/query db-spec ["select * from (Select distinct \"sec1\".*
                                   from \"securities\" as \"sec1\" left join \"securities\" as \"sec2\" on (\"sec1\".\"isin\" = \"sec2\".\"isin\" and
\"sec1\".\"date_added\" < \"sec2\".\"date_added\")
where \"sec2\".\"date_added\" is null)  where \"date_added\" <= ? " (to-sql-time read-date)] :entities (jdbc/quoted \"))]
    (assoc row :date_added (to-date-time (:date_added row)))))

(deftest test-init-db
  (doseq [test-conn connections]
    (if (= (:subprotocol test-conn) "sqlite")
      (is (= (jdbc/query test-conn ["PRAGMA foreign_keys;"]) '({:foreign_keys 1}))))
    (let [row {:name "revenue" :isin "de1234567890" :date_added (date-time 2013 01 01 13 59 12)}]
      (do (add-security test-conn row)
          (let [result (vec (db-read-all test-conn))]
            (is (= org.joda.time.DateTime (class ((result 0) :date_added))))
            (is (= [(assoc row :id 1)] result)))))))

(deftest test-constraints
  "It is not allowed to leave out either one of name, isin or
date_added when adding a new security to the database"
  (doseq [test-conn connections]
    (are [row] (thrown? SQLException (add-security test-conn row))
         {:name "revenue"}
         {:isin "de1234567890"}
         {:name "revenue" :isin "de1234567890"})))

(deftest test-db-empty
  "loading test data succeeds"
  (doseq [test-conn connections]
    (is (= (db-read-all test-conn) []))))

(deftest uniqueness-constraint
  (doseq [test-conn connections]
    (testing "can't insert a row that only differs in the date added"
      (let [row1 {:name "revenue" :isin "de1234567890" :date_added (date-time 2012 1 1)}
            row2 {:name "revenue" :isin "de1234567890" :date_added (date-time 2012 1 1)}]
        (add-security test-conn row1)
        (add-security test-conn row2)
        (let [result (vec (db-read-all test-conn))]
          (is (= result [(assoc row1 :id 1)])))))))

(defn mytest
  [order]
  (doseq [test-conn connections]
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
            [res1 res2 res3]
            (for [[row num] (map vector rows (map (zipmap order [1 2 3]) [0 1 2]))]
              (assoc row :id num))]
        (is (= (set (db-read-all test-conn)) #{}) "Sanity check that db is empty. Must never fail.")
        (doseq [row (map rows order)]
          (add-security test-conn row))
        (testing (str "Insertion Order:\n" (clojure.string/join "\n"  (map rows order)))
          (is (= #{res1 res2 res3} (set (db-read-all test-conn))) "read all")
          (is (= #{} (set (db-read-date test-conn (date-time 2011)))) "2011")
          (is (= #{res1} (set (db-read-date test-conn (date-time 2012 2 1)))) "2012-02-01")
          (is (= #{res1 res2} (set (db-read-date test-conn (date-time 2012 6 1)))) "2012-06-01")
          (is (= #{res2 res3} (set (db-read-date test-conn (date-time 2013 6 1)))) "2013-06-01"))))))

;; (deftest query-date-snapshots-0-1-2 (mytest [0 1 2]))
;; (deftest query-date-snapshots-0-2-1 (mytest [0 2 1]))
;; (deftest query-date-snapshots-1-0-2 (mytest [1 0 2]))
;; (deftest query-date-snapshots-1-2-0 (mytest [1 2 0]))
;; (deftest query-date-snapshots-2-0-1 (mytest [2 0 1]))
;; (deftest query-date-snapshots-2-1-0 (mytest [2 1 0]))

