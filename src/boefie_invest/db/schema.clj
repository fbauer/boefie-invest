(ns boefie-invest.db.schema
  (:require [clojure.java.jdbc :as jdbc]
            [noir.io :as io]))

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
              :naming {:keys clojure.string/lower-case
                       :fields clojure.string/upper-case}})

(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "database.db"
   :foreign_keys 1})

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

  {:isins [[:isin "varchar(12) not null primary key"]
           ["unique (isin)"]]

   :securities [[:id "integer not null primary key autoincrement"]
                [:isin "varchar(12) not null"]
                [:name "text not null"]
                [:date_added "datetime not null"]
                ["unique (isin, name)"]
                ["foreign key(isin) references isins(isin)"]]

   :shares [[:id "integer not null primary key autoincrement"]
            [:isin "varchar(12) not null"]
            [:amount "integer not null"]
            [:date "datetime not null"]
            [:date_added "datetime not null"]
            ["unique (isin, amount, date)"]
            ["foreign key(isin) references isins(isin)"]]

   :per_share_amounts [[:id "integer not null primary key autoincrement"]
                       [:isin "varchar(12) not null"]
                       [:name "text not null"]
                       [:currency "text not null"]
                       [:amount "integer not null"]
                       [:date "datetime not null"]
                       [:date_added "datetime not null"]
                       ["unique (isin, name, currency, amount, date)"]
                       ["foreign key(isin) references isins(isin)"]]

   :amounts [[:id "integer not null primary key autoincrement"]
             [:isin "varchar(12) not null"]
             [:name "text not null"]
             [:currency "text not null"]
             [:amount "integer not null"]
             [:date "datetime not null"]
             [:date_added "datetime not null"]
             ["unique (isin, name, currency, amount, date)"]
             ["foreign key(isin) references isins(isin)"]]})

(defn init-db
  "Create all tables. Assumes that none of the tables exists before.
   Table definitions are taken from the table-definitions var."
  [db-spec]
  (doseq [table-name (keys table-definitions)]
    (jdbc/db-do-commands
     db-spec
     (apply jdbc/create-table-ddl table-name
            (table-definitions table-name)))))

(defn kill-db
  "Drop all tables defined in table-definitions."
  [db-spec]
  (apply jdbc/db-do-commands
   db-spec
   (map #(format "DROP TABLE IF EXISTS %s"(jdbc/as-sql-name identity %))
        ;; Vector of table names as defined in table-definitions. I
        ;; can't use (key table-definitions) as I need to drop tables
        ;; in the correct order. :isins needs to be dropped last,
        ;; otherwise I generate foreign key constraint violations.
        [:amounts :per_share_amounts :shares :securities :isins])))


