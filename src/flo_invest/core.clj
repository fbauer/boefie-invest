(ns flo-invest.core (:require [clojure.set :only select]
                              [clj-time.core :only year]
                              [flo-invest.bigmoney :refer :all]
                              [flo-invest.morningstar])
    (:import [java.math RoundingMode]                              
             [org.joda.money BigMoney])
    (:use clojure-csv.core))

(defn parse-dir
  "Iterate through the directory datadir and find triples of files
  with stock information.

  Returns a seq containing triples of maps {:isin :type :file},
  where :isin is an isin string, :type is one
  of :balancesheet, :incomestatement or :keyratios and file is a
  handle to the file.
  "
  [datadir]
  (group-by :isin (flo-invest.morningstar/parse-dir datadir)))







