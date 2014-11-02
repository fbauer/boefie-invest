(ns boefie-invest.db.core
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [boefie-invest.db.schema :as schema]))

(defdb db schema/db-spec)

(defentity isins
  (pk :isin))

(defentity securities
  (has-one isins))
