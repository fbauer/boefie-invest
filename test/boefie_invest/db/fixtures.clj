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
                   :make-pool? true})

(def connections [test-conn-sqlite
                  test-conn-h2])

(defn setup-dbs
  [connections]
  (doseq [db-spec connections]
    (setup-db db-spec)))

(defn teardown-dbs
  [connections]
  (doseq [db-spec connections]
    (try
      (kill-db (db-name db-spec))
      (catch BatchUpdateException e (println e)))
    (close-global (db-name db-spec))))

(defn database
  [f]
  (do (setup-dbs connections)
      (f)
      (teardown-dbs connections)))

