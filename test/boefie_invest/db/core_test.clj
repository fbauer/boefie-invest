(ns boefie-invest.db.core-test
  (:require [clojure.test :refer :all]
            [boefie-invest.db.core :as dbc]
            [boefie-invest.db.fixtures :refer [database connections]]
            [boefie-invest.bigmoney :refer [as-money]]
            [korma.core :refer [defentity entity-fields table
                                insert values select
                                where join subselect fields
                                dry-run]]
            [korma.db :only [defdb]]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :refer [date-time today now before?]]
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
            my-entity (korma.core/database entity test-conn)]
        ;; setup
        (insert isins (values [{:isin "isin-1"}]))

        (testing (format "Insertion of first item succeeds" my-entity)
          (insert my-entity (values [item1]))
          (is (= [item1] (dbc/select-all my-entity))))

        (testing "Insertion of same item throws SQLException"
          (is (thrown? SQLException (insert-no-print my-entity [item1]))))

        (testing "Insertion of item that just differs in :date_added throws SQLException"
          (is (thrown? SQLException (insert-no-print my-entity [item3]))))

        (testing "Insertion of same item within transaction inserts nothing"
          (is (thrown? SQLException (insert-no-print my-entity [item2 item1])))
          (is (= [item1] (dbc/select-all my-entity))))))))

(deftest test-securities-entity
  (test-entity
   dbc/securities
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

(defn db-read-date
  [securities read-date]
  (defentity sel2
    (table (subselect
            securities
            (where {:date_added [<= (to-sql-time read-date)]})) :sel2))
  (select securities
          (fields :securities.date_added :securities.name :securities.isin)
          (join sel2 {:securities.date_added [< :sel2.date_added]
                      :securities.isin [= :sel2.isin]})
          (where {:sel2.isin nil
                  :date_added [<= (to-sql-time read-date)]})))

(defn mytest
  [order]
  (doseq [test-conn connections]
    (testing (str "Test database state.\n")
      ;; Insert security date for ACME 2012-1-1
      ;; Add a new company also called ACME on 2012-5-1
      ;; Rename first ACME to Silly corp on 2013-1-1
      (let [rows [{:name "ACME corp"
                   :isin "de1234567890" :date_added (date-time 2012 1 1)}
                  {:name "ACME corp"
                   :isin "de1234567891" :date_added (date-time 2012 5 1)}
                  {:name "Silly corp (formerly ACME)"
                   :isin "de1234567890" :date_added (date-time 2013 1 1)}]
            [res1 res2 res3] (for [[row num]
                                   (map vector rows (map (zipmap order [1 2 3]) [0 1 2]))]
                               row)
            isins (korma.core/database dbc/isins test-conn)
            securities (korma.core/database dbc/securities test-conn)]
        (insert isins (values (set (map #(dissoc % :name :date_added) rows))))
        (is (= [] (select securities))
            "Sanity check that db is empty. Must never fail.")
        (insert securities (values (map rows order)))
        (is (= #{res1 res2 res3} (set (dbc/select-all securities)))
            "There should be 3 rows in the securities table.")
        (testing (str "Insertion Order:\n"
                      (clojure.string/join "\n" (map rows order)))
          (doseq [[expected date] [[#{} (date-time 2011)]
                                   [#{res1} (date-time 2012 2 1)]
                                   [#{res1 res2} (date-time 2012 6 1)]
                                   [#{res2 res3} (date-time 2013 6 1)]]]
            (is (= expected (set (db-read-date securities date)))
                (format
                 "Query for date %s\nSQL:\n %s"
                 date
                 (with-out-str (dry-run (db-read-date securities date)))))))))))

(deftest query-date-snapshots-0-1-2 (mytest [0 1 2]))
(deftest query-date-snapshots-0-2-1 (mytest [0 2 1]))
(deftest query-date-snapshots-1-0-2 (mytest [1 0 2]))
(deftest query-date-snapshots-1-2-0 (mytest [1 2 0]))
(deftest query-date-snapshots-2-0-1 (mytest [2 0 1]))
(deftest query-date-snapshots-2-1-0 (mytest [2 1 0]))


(deftest test-insert-if-new
  (doseq [test-conn connections]
    (let [isins (korma.core/database dbc/isins test-conn)
          securities (korma.core/database dbc/securities test-conn)
          ;; we move before-inserts 1 second into the past to allow
          ;; for databases with 1 second granularity in the time stamps

          before-inserts (clj-time.core/minus (now) (clj-time.core/seconds 1))
          a-row {:isin "ab1234567890" :name "foobar"}
          row-with-date {:isin "de1234567890" :name "barbaz"
                         :date_added before-inserts}
          another-row {:isin "de1234567890" :name "new name"}]
      (insert isins (values [{:isin "ab1234567890"}
                             {:isin "de1234567890"}]))
      (is (= [{:isin "ab1234567890"} {:isin "de1234567890"}]
             (dbc/select-all isins)))

      (testing "default date_added is now"
        (dbc/insert-if-new securities [a-row])
        (let [first-result (first (dbc/select-all securities))]
          (is (contains? first-result :date_added))
          (is (before? before-inserts (:date_added a-row)))
          (is (= [a-row] [(dissoc first-result :date_added)]))))

      (testing "date_added can be set explicitly"
        (dbc/insert-if-new securities [row-with-date])
        (let [results (dbc/select-all securities)
              first-result (first results)]
          (is (contains? first-result :date_added))
          (is (before? before-inserts (:date_added a-row)))
          (is (= [a-row] [(dissoc first-result :date_added)]))
          (is (= [row-with-date] [(second results)]))
          (is (= 2 (count results)))))

      (testing "adding an existing row has no effect"
        (dbc/insert-if-new securities [row-with-date])
        (let [results (dbc/select-all securities)
              first-result (first results)]
          (is (contains? first-result :date_added))
          (is (before? before-inserts (:date_added a-row)))
          (is (= [a-row] [(dissoc first-result :date_added)]))
          (is (= [row-with-date] [(second results)]))
          (is (= 2 (count results)))))
      
      (testing "existing rows are ignored, but the rest of them are inserted"
        (dbc/insert-if-new securities [row-with-date
                                       a-row
                                       another-row
                                       a-row])
        (let [results (dbc/select-all securities)
              first-result (first results)
              third-result (nth results 2)]
          (is (contains? first-result :date_added))
          (is (before? before-inserts (:date_added a-row)))
          (is (= [a-row] [(dissoc first-result :date_added)]))
          (is (= [row-with-date] [(second results)]))
          (is (= [another-row] [(dissoc third-result :date_added)]))
          (is (= 3 (count results))))))))
