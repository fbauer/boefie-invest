(ns boefie-invest.db.core
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [boefie-invest.db.schema :as schema]
            [clj-time.coerce :refer [to-sql-time to-date-time]]))

;;(defdb db schema/db-spec)

(defentity isins
  (pk :isin))


(defentity securities
  (has-one isins)
  (entity-fields :date_added :name :isin)
  (prepare (fn [{date-added :date_added :as v}]
             (assoc v :date_added (to-sql-time date-added))))
  (transform (fn [{date-added :date_added :as v}]
               (assoc v :date_added (to-date-time date-added)))))
