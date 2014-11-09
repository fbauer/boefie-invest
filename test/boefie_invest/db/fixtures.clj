(ns boefie-invest.db.fixtures
  (:require [boefie-invest.db.schema :refer :all]
            [clojure.java.io :refer [resource]])
  (:import [java.sql BatchUpdateException]))

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

