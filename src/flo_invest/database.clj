
(ns flo-invest.database
  (:require [clojure.java.jdbc :as sql]))

(def db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "database.db"
   :foreign_keys 1
   })

(defn dbg [a] (do (print a) a))

;; ## Table definitions

(def table-definitions
  "## isins

   The isins table lists all valid isins. It is responsible to enforce
   foreign key constraints. I'm not sure yet whether this is a great
   design decision or not.

   ## securities

   The securities table is used to map an ISIN to the name of a
   security. It is append-only. An ISIN can never change, but the name
   of the corresponding security might. Therefore, the table has an
   additional column date_added which stores the date a column has
   been added. For equal ISINs, the most recently added row is the one
   that should be returned on a query.

   ## shares

   The shares table stores the amount of shares that were available at
   a given date.

   ## Sqlite implementation notes

   As sqlite has no native datetime column type, we store date_added
   with text affinity. The value is expected to be an utf-8 encoded
   string representation in iso datetime format and utc timezone."

  {:isins [[:isin "text not null primary key"]
           ["unique (isin) on conflict ignore"]]

   :securities [[:id "integer not null primary key autoincrement"]
                [:isin "text not null"]
                [:name "text not null"]
                [:date_added "datetime not null"]
                ["unique (isin, name) on conflict ignore"]
                ["foreign key(isin) references isins(isin)"]]

   :shares [[:id "integer not null primary key autoincrement"]
            [:isin "text not null"]
            [:amount "integer not null"]
            [:date "datetime not null"]
            [:date_added "datetime not null"]
            ["unique (isin, amount, date) on conflict ignore"]
            ["foreign key(isin) references isins(isin)"]]

   :per_share_amounts [[:id "integer not null primary key autoincrement"]
                       [:isin "text not null"]
                       [:name "text not null"]
                       [:currency "text not null"]
                       [:amount "integer not null"]
                       [:date "datetime not null"]
                       [:date_added "datetime not null"]
                       ["unique (isin, name, currency, amount, date) on conflict ignore"]
                       ["foreign key(isin) references isins(isin)"]]

   :amounts [[:id "integer not null primary key autoincrement"]
             [:isin "text not null"]
             [:name "text not null"]
             [:currency "text not null"]
             [:amount "integer not null"]
             [:date "datetime not null"]
             [:date_added "datetime not null"]
             ["unique (isin, name, currency, amount, date) on conflict ignore"]
             ["foreign key(isin) references isins(isin)"]]})

(defn init-db
  "Create all tables. Assumes that none of the tables exists before.
   Table definitions are taken from the table-definitions var."
  [db-spec]
  (sql/with-connection
    db-spec
    (doseq [table-name (keys table-definitions)]
      (apply sql/create-table table-name
             (table-definitions table-name)))))

(defn kill-db
  "Drop all tables defined in table-definitions."
  [db-spec]
  (sql/with-connection
    db-spec
    (doseq [table-name (keys table-definitions)]
      (sql/drop-table table-name))))

(defn add-security [db-spec sec]
  (sql/with-connection
    db-spec
    (do (sql/insert-record :isins {:isin (sec :isin)})
        (sql/insert-record :securities sec))))

(defn query
  [db-spec sql-params]
  (sql/with-connection
    db-spec
    (sql/with-query-results results sql-params (vec results))))

(defn db-read-all
  [db-spec]
  (query db-spec ["SELECT * FROM securities"]))

(defn db-read-date
  [db-spec read-date]
  (query db-spec ["Select distinct id, isin, name, max(date_added) as date_added
from securities where date_added <= ? group by isin " read-date]))
