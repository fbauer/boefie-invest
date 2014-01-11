
(ns flo-invest.database
  (:require [clojure.java.jdbc :as sql]))

(def db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "database.db"
   })

(defn dbg [a] (do (print a) a))

(defn init-db [db-spec]
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :securities
    [:id "integer not null primary key autoincrement"]
    [:isin "text not null"]
    [:name "text not null"]
    [:date_added "datetime not null"]
    ["unique (isin, name) on conflict ignore"]))
  )

(defn kill-db [db-spec]
  (sql/db-do-commands
   db-spec
   (sql/drop-table-ddl
    :securities
    ))
  )

(defn add-security [db-spec sec]
  (sql/insert! db-spec :securities sec))

(defn db-read-all
  [db-spec]
  (sql/query db-spec ["SELECT * FROM securities"]))

(defn db-read-date
  [db-spec read-date]
  (sql/query db-spec ["Select distinct id, isin, name, max(date_added) as date_added
from securities where date_added <= ? group by isin " read-date  ]))
