(ns flo-invest.core (:require [clojure.java.io :as io]
                              [clojure.set :only select]
                              [flo-invest.bigmoney :refer :all]
                              [flo-invest.morningstar])
    (:import [java.math RoundingMode])
    (:use clojure-csv.core)
    )

(defn parse-dir
  "Iterate through the directory datadir and find triples of files
  with stock information.

  Returns a seq containing triples of maps {:isin :type :file},
  where :isin is an isin string, :type is one
  of :balancesheet, :incomestatement or :keyratios and file is a
  handle to the file.
  "
  [datadir]
  (group-by :isin
            (for [f (file-seq (io/file datadir))
                  :let [filename (.getName f)
                        parts (clojure.string/split filename #" ")
                        isin (parts 0)]
                  :when (and
                         (.endsWith filename ".csv")
                         (= 3 (count parts))
                         (contains? #{["Balance" "Sheet.csv"]
                                      ["Income" "Statement.csv"]
                                      ["Key" "Ratios.csv"]} (subvec parts 1))
                         ) ] {:isin isin :type ({"Balance" :balancesheet
                                                 "Income" :incomestatement
                                                 "Key" :keyratios} (parts 1))  :file f})))

(defn- slurp-csv [type struct]
  (vec (parse-csv (slurp ((first (clojure.set/select #(= (:type %) type) struct)) :file)))))

(defn as-float [a_string]
  (if (= a_string  "") Double/NaN
      (Double/parseDouble (clojure.string/replace a_string "," "" ))))

(defn- parse-income
  "Legacy function.
   Uses flo-invest.morningstar/parse-income under the hood, but throws
   away all newly available information that this function returns.
  "
  [income]
  (if-let [result (last (flo-invest.morningstar/parse-income income))]
    {:annual_sales (result :amount)})) 

(defn- parse-balance [balance]
  (apply merge (for [item (flo-invest.morningstar/parse-balance  balance)]
                 {(item :name) (item :amount)})))

(defn double-vec [line]
  (vec (map as-float (subvec line 1 (- (count line) 1))))
  )

(defn money-vec [line currency]
  (vec (map #(as-money % currency) (subvec line 1 (- (count line) 1))))
  )

(defn- parse-keyratios [keyratios]
  (for [line keyratios]
    (if-let [match (re-matches #"Dividends (\w+)" (first line))]
      (into {} {:dividends (money-vec line (last match))
                :currency (last match)})
      (if-let [match (re-matches #"Earnings Per Share (\w+)" (first line))]
        (into {} {:eps (money-vec line (last match))})
        (if-let [match (re-matches #"Book Value Per Share (\w+)" (first line))]
          (into {} {:reported_book_value (last (money-vec line (last match)))})
          (if-let [match (re-matches #"Shares Mil" (first line))]
            (into {} {:shares_outstanding (* 1e6 (last (double-vec line)))})
            ))))))

(defn add-tangible-book-value [input-map]
  (merge {:tangible_book_value
          (minus (input-map :reported_book_value)
                 (divide (plus (with-scale (input-map :goodwill) 10) (input-map :intangibles))
                         (input-map :shares_outstanding)))
          } input-map))

(defn load-data
  "Process files as delivered by parse-dir.
  Returns output in format heavily inspired by the previous python format.
  "
  [file-data]
  (let [
        missing (as-money "" "EUR")
        inputs  {:non_redeemable_preferred_stock 0.0
                 :redeemable_preferred_stock 0.0
                 :split_bonus_factor 1.0
                 :goodwill missing
                 :intangibles missing
                 :annual_sales missing
                 :current_assets missing
                 :current_liabilities missing
                 :long_term_debt missing
                 :isin ((first file-data) :isin)
                 }
        file-data-set (set file-data)
        income (slurp-csv :incomestatement file-data-set)
        balance (slurp-csv :balancesheet file-data-set)
        keyratios (slurp-csv :keyratios file-data-set)
        ]
    (add-tangible-book-value (apply merge (cons inputs
                                                (concat 
                                                 (parse-income income)
                                                 (parse-balance balance)
                                                 (parse-keyratios keyratios))))))) 
