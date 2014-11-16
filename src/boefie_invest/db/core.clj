(ns boefie-invest.db.core
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [boefie-invest.db.schema :as schema]
            [boefie-invest.bigmoney :refer [as-money]]
            [clj-time.coerce :refer [to-sql-time to-date-time]])
  (:import [org.joda.money CurrencyUnit]
           [org.joda.money BigMoney]))

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

(defn to-bigmoney
  [{amount :amount currency :currency scale :scale :as v}]
  (merge (dissoc v :currency :scale :amount)
         (if (and amount currency scale)
           {:amount (BigMoney/ofScale (CurrencyUnit/of currency)
                                      (.longValue amount)
                                      (.longValue scale))})))

(defn from-bigmoney
  [{amount :amount :as v}]
  (merge v
         (if amount
           {:currency (.getCode (.getCurrencyUnit amount))
            :scale (.longValue (.getScale amount))
            :amount (.longValue (.unscaledValue (.getAmount amount)))})))

(defentity securities
  (has-one isins)
  (prepare dates-to-sql)
  (transform dates-to-date-time))

(defentity shares
  (has-one isins)
  (prepare dates-to-sql)
  (transform dates-to-date-time))

(defentity amounts
  (has-one isins)
  (prepare dates-to-sql)
  (prepare from-bigmoney)
  (transform to-bigmoney)
  (transform dates-to-date-time))

(defentity per_share_amounts
  (has-one isins)
  (prepare dates-to-sql)
  (prepare from-bigmoney)
  (transform to-bigmoney)
  (transform dates-to-date-time))

(defn select-securities
  [securities]
  (select securities (fields :isin :name :date_added)))

(defn select-shares
  [shares]
  (select shares 
          (fields :isin :amount :date :date_added)))

(defn select-amounts
  [amounts]
  (select amounts
          (fields :isin :name :amount :scale :currency :date :date_added)))

(defn select-per-share-amounts
  [per_share_amounts]
  (select per_share_amounts
          (fields :isin :name :amount :scale :currency :date :date_added)))
