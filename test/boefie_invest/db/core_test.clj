(ns boefie-invest.db.core-test
  (:require [clojure.test :refer :all]
            [boefie-invest.db.core :as dbc]
            [boefie-invest.db.fixtures :refer [database connections]]
            [boefie-invest.bigmoney :refer [as-money]]
            [korma.core :refer [insert values select]]
            [korma.db :only [defdb]]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-sql-time to-date-time]]
            [clojure.java.io :refer [resource]])
  (:import [java.sql SQLException]))

(use-fixtures :each database)

(defn- insert-no-print
  "Sqlkorma prints exceptions to *out* before rethrowing them.
This helper suppresses that for inserts that should throw an Exception."
  [table the-values]
  (with-out-str (insert table (values the-values))))

(deftest test-isins-entity
  (doseq [test-conn connections]
    (testing (format "Test table isins in db %s" (:subprotocol test-conn))
      (let [isins (korma.core/database dbc/isins test-conn)]
        (testing "Insertion of first isin succeeds"
          (insert isins (values [{:isin "isin-1"}]))
          (is (= [{:isin "isin-1"}] (select isins))))
        
        (testing "Insertion of same isin throws SQLException"
          (is (thrown? SQLException (insert-no-print isins [{:isin "isin-1"}]))))
        
        (testing "Insertion of same isin within transaction inserts nothing"
          (is (thrown? SQLException (insert-no-print isins [{:isin "isin-2"}
                                                            {:isin "isin-1"}])))
          (is (= [{:isin "isin-1"}] (select isins))))))))

(defn test-entity
  [entity item1 item2 item3]
  (doseq [test-conn connections]
    (testing (format "Test table %s in db %s.\n" (:name entity) (:subprotocol test-conn))
      (let [isins (korma.core/database dbc/isins test-conn)
            shares (korma.core/database entity test-conn)]
        ;; setup
        (insert isins (values [{:isin "isin-1"}]))
        
        (testing (format "Insertion of first item succeeds" shares)
          (insert shares (values [item1]))
          (is (= [item1] (select shares))))
        
        (testing "Insertion of same item throws SQLException"
          (is (thrown? SQLException (insert-no-print shares [item1]))))

        (testing "Insertion of item that just differs in :date_added throws SQLException"
          (is (thrown? SQLException (insert-no-print shares [item3]))))
        
        (testing "Insertion of same item within transaction inserts nothing"
          (is (thrown? SQLException (insert-no-print shares [item2 item1])))
          (is (= [item1] (select shares))))))))

(deftest test-securities-entity
  (test-entity dbc/securities
               {:name "security-1" :isin "isin-1"
                :date_added (date-time 2014 10 1)}
               {:name "security-2" :isin "isin-1"
                :date_added (date-time 2014 10 10)}
               {:name "security-1" :isin "isin-1"
                :date_added (date-time 2014 10 2)}))

(deftest test-shares-entity
  (test-entity
   dbc/shares
   {:amount 20000 :isin "isin-1"
    :date (date-time 2014 10 1)
    :date_added (date-time 2014 10 10)}
   {:amount 30000 :isin "isin-1"
    :date (date-time 2014 10 2)
    :date_added (date-time 2014 10 10)}
   {:amount 20000 :isin "isin-1"
    :date (date-time 2014 10 1)
    :date_added (date-time 2014 10 11)}))

(deftest test-to-bigmoney
  (are [expected input] (= expected (dbc/to-bigmoney input))
       {:amount (as-money "10" "EUR")}
       {:amount 10 :scale 0 :currency "EUR"}
       
       {:amount (as-money "10.10" "EUR")}
       {:amount 1010 :scale 2 :currency "EUR"}
       
       {:amount (as-money "10.5" "EUR")}
       {:amount 105 :scale 1 :currency "EUR"}
       
       {:name "a financial amount"
        :amount (as-money "20000.1245" "EUR")
        :isin "isin-1"
        :date (date-time 2014 10 1)
        :date_added (date-time 2014 10 10)}
       {:name "a financial amount"
        :isin "isin-1"
        :scale 4
        :amount 200001245
        :currency "EUR"
        :date (date-time 2014 10 1)
        :date_added (date-time 2014 10 10)}))

(deftest test-from-bigmoney
  (are [expected input] (= expected (dbc/from-bigmoney input))
       {:amount 1010 :scale 2 :currency "EUR"}
       {:amount (as-money "10.10" "EUR")}
       {:name "a financial amount"
        :isin "isin-1"
        :scale 4
        :amount 200001245
        :currency "EUR"
        :date (date-time 2014 10 1)
        :date_added (date-time 2014 10 10)}
       {:name "a financial amount"
        :amount (as-money "20000.1245" "EUR")
        :isin "isin-1"
        :date (date-time 2014 10 1)
        :date_added (date-time 2014 10 10)}))

(deftest test-amounts-entity
  (test-entity
   dbc/amounts
   {:name "a financial amount"
    :amount (as-money "20000.1245" "EUR")
    :isin "isin-1"
    :date (date-time 2014 10 1)
    :date_added (date-time 2014 10 10)}
   {:name "another financial amount"
    :amount (as-money "30000.1245" "EUR")
    :isin "isin-1"
    :date (date-time 2014 10 2)
    :date_added (date-time 2014 10 10)}
   {:name "a financial amount"
    :amount (as-money "20000.1245" "EUR")
    :isin "isin-1"
    :date (date-time 2014 10 1)
    :date_added (date-time 2014 10 11)}))

(deftest test-per-share-amounts-entity
  (test-entity
   dbc/per_share_amounts
   {:name "a financial amount"
    :amount (as-money "20000.1245" "EUR")
    :isin "isin-1"
    :date (date-time 2014 10 1)
    :date_added (date-time 2014 10 10)}
   {:name "another financial amount"
    :amount (as-money "30000.1245" "EUR")
    :isin "isin-1"
    :date (date-time 2014 10 2)
    :date_added (date-time 2014 10 10)}
   {:name "a financial amount"
    :amount (as-money "20000.1245" "EUR")
    :isin "isin-1"
    :date (date-time 2014 10 1)
    :date_added (date-time 2014 10 11)}))

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

(defn mytest
  [order]
  (doseq [test-conn connections]
    (testing (str "Test database state. Insert order" order)
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

