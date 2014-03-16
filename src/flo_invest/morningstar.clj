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
  (vec (map parse-date-yyyy-mm csv-record)))

(defn money-vec [csv-record currency]
  (vec (map #(as-money % currency) csv-record)))

(defn find-first
  [predicate coll]
  (first (filter predicate coll)))

(defn parse-morningstar
  [balance extract keys-to-symbol]
  (let [date-header (date-vec (extract (balance 1)))
        year-currency-pat #"Fiscal year ends in \w+. (\w+) in millions except per share data."
        currency (last (re-matches year-currency-pat (first (balance 1))))]
    (flatten (for [record balance
                   :when (and (> (count record) 2)
                              (contains? keys-to-symbol (record 0)))]
               (filter #(not (nil? (:amount %)))
                       (map #(assoc {}
                               :name (keys-to-symbol (record 0))
                               :amount (annual_sales %1)
                               :date %2)
                            (money-vec (extract record) currency) date-header))))))

(defn parse-balance
  [balance]
  (parse-morningstar balance #(subvec % 1 (count %))
                     {"Total current assets" :current_assets
                      "Total current liabilities" :current_liabilities
                      "Long-term debt" :long_term_debt
                      "Total liabilities" :total_liabilities
                      "Total assets" :total_assets
                      "Goodwill" :goodwill
                      "Intangible assets" :intangibles}))

(defn parse-income
  "Parse an income statement as issued by Morningstar.

  Look for the header line which specifies the currency and the end of
  the fiscal years (usually 5 years). Then go and find the Revenue line.
  Return a vector of maps {:name :amount :date}, where :name is always
  :annual_sales, and :amount and :date give the revenue for a given
  year."
  [income]
  (parse-morningstar income #(subvec % 1 (- (count %) 1))
                     {"Revenue" :annual_sales}))

(defn as-float
  "Create double from string.

  Empty strings are interpreted as NaN. The number representation is
  expected in american format, using a comma as thousands separator."
  [a_string]
  (if (= a_string  "")
    Double/NaN
    (Double/parseDouble (clojure.string/replace a_string "," "" ))))

(defn double-vec
  "Legacy function"
  [line]
  (vec (map as-float (subvec line 1 (- (count line) 1)))))

(defn legacy-money-vec
  "Legacy function"
  [line currency]
  (vec (map #(as-money % currency) (subvec line 1 (- (count line) 1)))))

(defn parse-keyratios
  "Legacy function"
  [keyratios]
  (for [line keyratios]
    (if-let [match (re-matches #"Dividends (\w+)" (first line))]
      (into {} {:dividends (legacy-money-vec line (last match))
                :currency (last match)})
      (if-let [match (re-matches #"Earnings Per Share (\w+)" (first line))]
        (into {} {:eps (legacy-money-vec line (last match))})
        (if-let [match (re-matches #"Book Value Per Share (\w+)" (first line))]
          (into {} {:reported_book_value (last (legacy-money-vec line (last match)))})
          (if-let [match (re-matches #"Shares Mil" (first line))]
            (into {} {:shares_outstanding (* 1e6 (last (double-vec line)))})))))))
