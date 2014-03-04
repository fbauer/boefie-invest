(ns flo-invest.morningstar
  (:require [flo-invest.bigmoney :refer :all]
            [clj-time.core :refer [date-time]]
            [clj-time.format :refer [parse formatters]]
            [clj-time.coerce :refer [from-string]])
  (:import [java.math RoundingMode]))

(defn annual_sales
  [millions]
  (multiply millions 1000000))

(defn parse-date-yyyy-mm
  "Parse date in yyyy-mm format"
  [date-string]
  (parse (formatters :year-month) date-string))

(defn date-vec [csv-record]
  (vec (map parse-date-yyyy-mm (subvec csv-record 1 (- (count csv-record) 1)))))

(defn money-vec [csv-record currency]
  (vec (map #(as-money % currency) (subvec csv-record 1 (- (count csv-record) 1)))))

(defn find-first
  [predicate coll]
  (first (filter predicate coll)))

(defn parse-income [income]

  (let [date-header (date-vec (income 1))
        year-currency-pat #"Fiscal year ends in \w+. (\w+) in millions except per share data."
        currency (last (re-matches year-currency-pat (first (income 1))))
        revenue-record  (find-first #(= "Revenue" (first %)) income)]
    (if (< (count revenue-record) 2) []
        (map #(assoc {} :name :annual_sales
                     :amount (annual_sales %1)
                     :date %2) (money-vec revenue-record currency) date-header))))

