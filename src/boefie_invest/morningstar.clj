(ns boefie-invest.morningstar
  (:require [clojure.java.io :as io]
            [boefie-invest.bigmoney :refer :all]
            [clj-time.core :refer [date-time]]
            [clj-time.format :refer [parse formatters formatter]]
            [clj-time.coerce :refer [from-string]]
            [clojure-csv.core :refer [parse-csv]])
  (:import [java.math RoundingMode]))

(defn parse-date-yyyy-mm
  "Parse date in yyyy-mm format"
  [date-string]
  (parse (formatters :year-month) date-string))

(defn as-double
  "Create double from string.

  Empty strings are interpreted as NaN. The number representation is
  expected in american format, using a comma as thousands separator."
  [a_string]
  (if (= a_string  "")
    Double/NaN
    (Double/parseDouble (clojure.string/replace a_string "," "" ))))

(defn double-vec
  "Convert a vector of strings to a vector of doubles."
  [line]
  (vec (map as-double line)))

(defn date-vec [csv-record]
  (vec (map parse-date-yyyy-mm csv-record)))

(defn money-vec [csv-record currency]
  (vec (map #(as-money % currency) csv-record)))

(defn parse-morningstar
  [rows extract keys-to-symbol]
  (let [date-header (date-vec (extract (rows 1)))
        year-currency-pat #"Fiscal year ends in \w+. (\w+) in millions except per share data."
        currency (last (re-matches year-currency-pat (first (rows 1))))]
    (flatten (for [record rows
                   :when (and (> (count record) 2)
                              (contains? keys-to-symbol (record 0)))]
               (filter #(not (nil? (:amount %)))
                       (map #(assoc {}
                               :name (keys-to-symbol (record 0))
                               :amount (multiply %1 1000000)
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

(defn parse-keyratios
  "Parse a key ratios csv file as issued by Morningstar.

  Return a vector of maps {:name :amount :date}, where :name is one of
  the symbols :dividends, :eps, :reported_book_value, or
  :shares_outstanding. :amount and :date give the revenue for a given
  year."
  [rows]
  (let [extract #(subvec % 1 (- (count %) 1))
        regex #"(Earnings Per Share|Book Value Per Share|Dividends|Shares Mil) ?(\w+)?"
        date-header (date-vec (extract (rows 2)))
        keys-to-symbol {"Earnings Per Share" :eps
                        "Book Value Per Share" :reported_book_value
                        "Dividends" :dividends
                        "Shares Mil" :shares_outstanding}]
    (flatten (for [record rows
                     :let [match (re-matches regex (record 0))
                           currency (get match 2)
                           name (get match 1)]
                     
                     :when (and (> (count record) 2) match)]
               (filter #(not (nil? (:amount %)))
                       (map #(assoc {}
                               :name (keys-to-symbol name)
                               :amount %1
                               :date %2)
                            (if currency
                              (money-vec (extract record) currency)
                              (map #(* 1e6 %) (double-vec (extract record))))
                            date-header))))))

(defn parse-dir
  "Find files with stock information.

  Recursively iterate through datadir. parse-dir expects to find
  subdirectories whose name is a datestring in yyyy_MM_dd format,
  which contain csv files downloaded from morningstar.com.

  Returns a seq of maps {:isin :type :file :date_added}, where :isin
  is an isin string, :type is one of :balancesheet, :incomestatement
  or :keyratios, file is a handle to the file and :date_added is a
  datetime representation."
  ([datadir]
     (parse-dir datadir file-seq))
  ;; second entry point is for testing purposes
  ([datadir iterator]
     (for [f (iterator (io/file datadir))
           :let [filename (.getName f)
                 dirname (.getName (.getParentFile f))
                 parts (clojure.string/split filename #" ")
                 isin (first parts)
                 date-added (try
                              (parse (formatter "yyyy-MM-dd") dirname)
                              (catch Exception e))]
           :when (and (.endsWith filename ".csv")
                      (= 3 (count parts))
                      (contains? #{["Balance" "Sheet.csv"]
                                   ["Income" "Statement.csv"]
                                   ["Key" "Ratios.csv"]} (subvec parts 1))
                      date-added)]
       {:isin isin :type ({"Balance" :balancesheet
                           "Income" :incomestatement
                           "Key" :keyratios} (parts 1))
        :file f :date_added date-added})))


(defn load-data
  "Load all stock information from data-dir."
  [data-dir]
  (apply concat
   (for [file-info (parse-dir data-dir)
         :let [csv-data (vec (parse-csv (slurp (:file file-info))))
               isin-date (select-keys file-info [:date_added :isin])]]
     (map #(merge isin-date %)
          (case (:type file-info)
            :balancesheet (parse-balance csv-data)
            :incomestatement (parse-income csv-data)
            :keyratios (parse-keyratios csv-data))))))



