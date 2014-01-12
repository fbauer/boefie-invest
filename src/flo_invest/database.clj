
(ns flo-invest.database
  (:require [clojure.java.jdbc :as sql]))

(def db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "database.db"
   :foreign_keys 1
   })

(defn dbg [a] (do (print a) a))

(defn init-db [db-spec]
  "Create all tables in the database specified by db-spec.

  Assumes that none of the tables exists before."
  (sql/db-do-commands
   db-spec
   ;; The isins table lists all valid isins.
   ;; It is responsible to enforce foreign key constraints.
   ;; I'm not sure yet whether this is a great design decision or not.
   (sql/create-table-ddl
    :isins
    [:isin "text not null primary key"]
    ["unique (isin) on conflict ignore"]
    )
   ;; The securities table is used to map an ISIN to the name of a
   ;; security. It is append-only. An ISIN can never change, but the
   ;; name of the corresponding security might. Therefore, the table
   ;; has an additional column date_added which stores the date a
   ;; column has been added. For equal ISINs, the most recently added
   ;; row is the one that should be returned on a query.
   ;;
   ;; Sqlite implementation note:
   ;; As sqlite has no native datetime column type, we store
   ;; date_added with text affinity. The value is expected to be an
   ;; utf-8 encoded string representation in iso datetime format and
   ;; utc timezone.
   (sql/create-table-ddl
    :securities
    [:id "integer not null primary key autoincrement"]
    [:isin "text not null"]
    [:name "text not null"]
    [:date_added "datetime not null"]
    ["unique (isin, name) on conflict ignore"]
    ["foreign key(isin) references isins(isin)"])
   (sql/create-table-ddl
    :shares
    [:id "integer not null primary key autoincrement"]
    [:isin "text not null"]
    [:amount "integer not null"]
    [:date "datetime not null"]
    [:date_added "datetime not null"]
    ["unique (isin, amount, date) on conflict ignore"]
    ["foreign key(isin) references isins(isin)"])
   (sql/create-table-ddl
    :per_share_amounts
    [:id "integer not null primary key autoincrement"]
    [:isin "text not null"]
    [:name "text not null"]
    [:currency "text not null"]
    [:amount "integer not null"]
    [:date "datetime not null"]
    [:date_added "datetime not null"]
    ["unique (isin, name, currency, amount, date) on conflict ignore"]
    ["foreign key(isin) references isins(isin)"])
   (sql/create-table-ddl
    :amounts
    [:id "integer not null primary key autoincrement"]
    [:isin "text not null"]
    [:name "text not null"]
    [:currency "text not null"]
    [:amount "integer not null"]
    [:date "datetime not null"]
    [:date_added "datetime not null"]
    ["unique (isin, name, currency, amount, date) on conflict ignore"]
    ["foreign key(isin) references isins(isin)"])
   ))

(defn kill-db [db-spec]
  (sql/db-do-commands
   db-spec
   (sql/drop-table-ddl :securities)
   (sql/drop-table-ddl :shares)
   (sql/drop-table-ddl :amounts)
   (sql/drop-table-ddl :per_share_amounts)
   (sql/drop-table-ddl :isins)
   )
  )

(defn add-security [db-spec sec]
  (do (sql/insert! db-spec :isins {:isin (sec :isin)})
      (sql/insert! db-spec :securities sec)))

(defn db-read-all
  [db-spec]
  (sql/query db-spec ["SELECT * FROM securities"]))

(defn db-read-date
  [db-spec read-date]
  (sql/query db-spec ["Select distinct id, isin, name, max(date_added) as date_added
from securities where date_added <= ? group by isin " read-date  ]))
