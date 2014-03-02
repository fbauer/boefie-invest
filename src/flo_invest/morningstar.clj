(ns flo-invest.morningstar
  (:require [flo-invest.bigmoney :refer :all]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [from-string]])
  (:import [java.math RoundingMode]))

(defn annual_sales
  [millions]
  (multiply millions 1000000))

(defn date-vec [csv-record]
  (vec (map from-string (subvec csv-record 1 (- (count csv-record) 1)))))

(defn parse-income [income]
  (let [date-header (date-vec (income 1))
        currency (last (re-matches #"Fiscal year ends in \w+. (\w+) in millions except per share data." (first (income 1))))]
    (for [line income
          :when (= "Revenue" (first line))]
      {:name :annual_sales
       :amount (annual_sales
                (as-money (nth line (- (count line) 2) "") currency))
       :date (from-string "2012-12-12")}))) 

