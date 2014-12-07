(ns boefie-invest.db.fixtures
  (:require [boefie-invest.db.schema :refer :all]
            [clojure.java.io :refer [resource]]
            [lobos.connectivity :refer :all])
  (:import [java.sql BatchUpdateException]))

(def test-conn-sqlite {:classname "org.sqlite.JDBC"
                       :subprotocol "sqlite"
                       :subname "file::memory:?cache=shared"
                       :make-pool? true
                       :foreign_keys 1})

(def test-conn-h2 {:classname "org.h2.Driver"
                   :subprotocol "h2"
                   :subname "mem:testdb"
                   :user "sa"
                   :password ""
                   :make-pool? true
                   :naming {:keys clojure.string/lower-case
                            :fields clojure.string/upper-case}})

(def connections [test-conn-sqlite
                  test-conn-h2])

;; copied from the lobos test suite
(defn test-db-name [db-spec]
  (keyword (:subprotocol db-spec)))

(defn setup-dbs
  [connections]
  (doseq [db-spec connections
          :let [connection-name (test-db-name db-spec)]]
    (if-not (connection-name @global-connections)
      (open-global connection-name db-spec))
    (init-db connection-name)))

(defn teardown-dbs
  [connections]
  (doseq [db-spec connections]
    (try
      (kill-db (test-db-name db-spec))
      (catch BatchUpdateException e (println e)))
    (close-global (test-db-name db-spec))))

(defn database [f]
  (do
    (do (setup-dbs connections)
        (f)
        (teardown-dbs connections))))

