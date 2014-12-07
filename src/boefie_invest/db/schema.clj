(ns boefie-invest.db.schema
  (:refer-clojure :exclude [drop])
  (:require [clojure.java.jdbc :as jdbc]
            [noir.io :as io]
            [lobos.schema :refer [table integer timestamp
                                  varchar unique default]]
            [lobos.core :refer [create drop]]))

(def db-store "site.db")

(defn initialized?
  "checks to see if the database schema is present"
  []
  (.exists (new java.io.File (str (io/resource-path) db-store ".mv.db"))))

(def db-spec {:classname "org.h2.Driver"
              :subprotocol "h2"
              :subname (str (io/resource-path) db-store)
              :user "sa"
              :password ""
              :make-pool? true
              })

;; ## Table definitions

(def table-definitions
  "The database schema.

It consists of the following tables:

### isins

The isins table lists all valid isins. It is responsible to enforce
foreign key constraints. I'm not sure yet whether this is a great
design decision or not.

### securities

The securities table is used to map an ISIN to the name of a security.
It is append-only. An ISIN can never change, but the name of the
corresponding security might. Therefore, the table has an additional
column date_added which stores the date a column has been added. For
equal ISINs, the most recently added row is the one that should be
returned on a query.

### shares

The shares table stores the amount of shares that were available at a
given date.

### amounts and per_share_amounts

These two tables have an identical schema. Their intended useage
differs. amounts stores the value a certain financial figure had at a
certain date (per company), while per_share_amount stores the value of
financial figures per share. Eg share price would be stored in the
per_share_amounts table, while current assets would be stored in the
amounts table.

### Sqlite implementation notes

As sqlite has no native datetime column type, we store date_added with
text affinity. The value is expected to be an utf-8 encoded string
representation in iso datetime format and utc timezone."

  [(table :isins
          (varchar :isin 12 :not-null :primary-key :unique))
   
   (table :securities
          (integer :id :not-null :primary-key :auto-inc)
          (varchar :isin 12 :not-null [:refer :isins :isin])
          (varchar :name  200 :not-null)
          (timestamp :date_added :not-null (default (now)))
          (unique [:isin :name]))

   (table :shares
          (integer :id :not-null :primary-key :auto-inc)
          (varchar :isin 12 :not-null [:refer :isins :isin])
          (integer :amount  :not-null)
          (timestamp :date :not-null)
          (timestamp :date_added :not-null (default (now)))
          (unique [:isin :amount :date]))
   
   (table :per_share_amounts
          (integer :id :not-null :primary-key :auto-inc)
          (varchar :isin 12 :not-null [:refer :isins :isin])
          (varchar :name  200 :not-null)
          (varchar :currency  3 :not-null)
          (integer :amount  :not-null)
          (integer :scale  :not-null)
          (timestamp :date :not-null)
          (timestamp :date_added :not-null (default (now)))
          (unique [:isin :name :currency :amount :date]))
   
   (table :amounts
          (integer :id :not-null :primary-key :auto-inc)
          (varchar :isin 12 :not-null  [:refer :isins :isin])
          (varchar :name  200 :not-null)
          (varchar :currency  3 :not-null)
          (integer :amount :not-null)
          (integer :scale :not-null)
          (timestamp :date :not-null)
          (timestamp :date_added :not-null (default (now)))
          (unique [:isin :name :currency :amount :date]))])

(defn init-db
  "Create all tables. Assumes that none of the tables exists before.
   Table definitions are taken from the table-definitions var."
  [connection-name]
  (assert (keyword? connection-name))
  (lobos.connectivity/with-connection connection-name
    (doseq [table-def table-definitions]
      (create table-def))))

(defn kill-db
  "Drop all tables"
  [connection-name]
  (assert (keyword? connection-name))
  (lobos.connectivity/with-connection connection-name
    (doseq [table-name
            [:amounts :per_share_amounts :shares :securities :isins]]
      (try (drop (table table-name))
           (catch java.sql.SQLException e (println e))))))
