(ns flo-invest.core (:require [clojure.java.io :as io]
                              [clojure.set :only select])
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
  (parse-csv (slurp ((first (clojure.set/select #(= (:type %) type) struct)) :file))))

(defn as-float [a_string]
  (if (= a_string  "") Double/NaN
      (Double/parseDouble (clojure.string/replace a_string "," "" ))))

(defn- parse-income [income]
  (for [line income
        :when (= "Revenue" (first line))]
    {:annual_sales (* 1e6 (as-float (nth line (- (count line) 2) "")))}))

(defn- parse-balance [balance]
  (let [balance_dict  {"Total current assets" :current_assets
                       "Total current liabilities" :current_liabilities
                       "Long-term debt" :long_term_debt
                       "Total liabilities" :total_liabilities
                       "Total assets" :total_assets
                       "Goodwill" :goodwill
                       "Intangible assets" :intangibles
                       }]
    (for [line balance]
      (if (contains? balance_dict (line 0))
        (into {} { (balance_dict (line 0)) (* 1e6 (as-float (last line)))})))
    ))

(defn double-vec [line]
  (vec (map as-float (subvec line 1 (- (count line) 1))))
  )
(defn- parse-keyratios [keyratios]
  (for [line keyratios]
    (if-let [match (re-matches #"Dividends (\w+)" (first line))]
      (into {} {:dividends (double-vec line) :currency (last match)})
      (if-let [match (re-matches #"Earnings Per Share (\w+)" (first line))]
        (into {} {:eps (double-vec line) :currency (last match)})
        (if-let [match (re-matches #"Book Value Per Share (\w+)" (first line))]
          (into {} {:reported_book_value (last (double-vec line)) :currency (last match)})
          (if-let [match (re-matches #"Shares Mil" (first line))]
            (into {} {:shares_outstanding (* 1e6 (last (double-vec line)))})
            ))))))

(defn add-tangible-book-value [input-map]
  (into input-map {:tangible_book_value
                   (- (input-map :reported_book_value)
                      (/ (+ (input-map :goodwill) (input-map :intangibles))
                         (input-map :shares_outstanding)))
                   }))

(defn load-data [file-data]
  (let [
        inputs  {:non_redeemable_preferred_stock 0.0
                 :redeemable_preferred_stock 0.0
                 :split_bonus_factor 1.0
                 :goodwill (as-float "")
                 :intangibles (as-float "")
                 :annual_sales (as-float "")
                 :current_assets (as-float "")
                 :current_liabilities (as-float "")
                 :long_term_debt (as-float "")
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
