(ns boefie-invest.db.core
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [boefie-invest.db.schema :as schema]
            [clj-time.coerce :refer [to-sql-time to-date-time]]))

;;(defdb db schema/db-spec)

(defentity isins
  (pk :isin))

(defn dates-to-sql
  [{date-added :date_added date :date :as v}]
  (merge v
         (if date-added {:date_added (to-sql-time date-added)})
         (if date {:date (to-sql-time date)})))

(defn dates-to-date-time
  [{date-added :date_added date :date :as v}]
  (merge v
         (if date-added {:date_added (to-date-time date-added)})
         (if date {:date (to-date-time date)})))

(defentity securities
  (has-one isins)
  (entity-fields :isin :name :date_added)
  (prepare dates-to-sql)
  (transform dates-to-date-time))

(defentity shares
  (has-one isins)
  (entity-fields :isin :amount :date :date_added)
  (prepare dates-to-sql)
  (transform dates-to-date-time))
