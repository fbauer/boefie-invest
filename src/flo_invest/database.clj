
(ns flo-invest.database
  (:require [clojure.java.jdbc :as sql]))

(def db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "database.db"
   })

(defn init-db [db-spec]
  (sql/db-do-commands
    db-spec
    (sql/create-table-ddl
     :books
     [:id "integer" :primary :key :autoincrement]
     [:title "varchar(250)"]
     [:review "varchar(500)"]))
    )

(defn kill-db [db-spec]
  (sql/db-do-commands
   db-spec
   (sql/drop-table-ddl
    :books
    ))
  )

(defn add-book [db-spec book]
  (sql/insert! db-spec :books book))

(defn db-read-all
  [db-spec]
  (sql/query db-spec ["SELECT * FROM books"]))
