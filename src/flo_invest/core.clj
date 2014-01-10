(ns flo-invest.core (:require [clojure.java.io :as io]
                              [clojure.set :only select])
    (:import [org.joda.money BigMoney]
             [java.math RoundingMode])
                              
    (:use clojure-csv.core)
    )

(defn parse-dir [datadir]
  (group-by :isin
            (for [f (file-seq (io/file datadir))
                  :let [filename (.getName f)
                        parts (clojure.string/split filename #" ")
                        isin (parts 0)
                        ]
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

(defn as-money [a_string currency_symbol]
  (if (= a_string  "") nil
      (BigMoney/parse (str currency_symbol " " (clojure.string/replace  a_string "," "")))))

(defn plus [a b]
  (if (or (and (number? a) (Double/isNaN a))
          (and (number? b) (Double/isNaN b))
          (nil? a)
          (nil? b)
          ) nil
            (.plus a b)))

(defn minus [a b]
  (if (or (and (number? a) (Double/isNaN a))
          (and (number? b) (Double/isNaN b))
          (nil? a)
          (nil? b)
          ) nil
            (.minus a b)))

(defn multiply [a b]
  (if (or (and (number? a) (Double/isNaN a))
          (and (number? b) (Double/isNaN b))
          (nil? a)
          (nil? b)
          ) nil
            (.multipliedBy a b)))

(defn divide
  ([a b]
      (if (or (and (number? a) (Double/isNaN a))
              (and (number? b) (Double/isNaN b))
              (nil? a)
              (nil? b)
              ) nil
                (.dividedBy a b RoundingMode/HALF_UP))))

(defn- parse-income [income]
  (let [currency (last (re-matches #"Fiscal year ends in \w+. (\w+) in millions except per share data." (first (income 1))))]
    (for [line income
          :when (= "Revenue" (first line))]
      {:annual_sales (multiply
                      (as-money (nth line (- (count line) 2) "") currency)
                      1000000)}))) 

(defn- parse-balance [balance]
  (let [balance_dict  {"Total current assets" :current_assets
                       "Total current liabilities" :current_liabilities
                       "Long-term debt" :long_term_debt
                       "Total liabilities" :total_liabilities
                       "Total assets" :total_assets
                       "Goodwill" :goodwill
                       "Intangible assets" :intangibles
                       }
        currency (last (re-matches #"Fiscal year ends in \w+. (\w+) in millions except per share data." (first (balance 1))))]
    (for [line balance]

      (if (contains? balance_dict (line 0))
        (into {} { (balance_dict (line 0)) (multiply
                                            (as-money (last line) currency)
                                            1000000)})))
    ))

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

(defn with-scale [bigmoney-or-nil scale]
  (if (nil? bigmoney-or-nil) nil (.withScale bigmoney-or-nil scale)))

(defn add-tangible-book-value [input-map]
  (merge {:tangible_book_value
          (minus (input-map :reported_book_value)
                 (divide (plus (with-scale (input-map :goodwill) 10) (input-map :intangibles))
                         (input-map :shares_outstanding)
                         ))
          } input-map))

(defn load-data [file-data]
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
                                                  (parse-keyratios keyratios)
                                                  )))))) 
