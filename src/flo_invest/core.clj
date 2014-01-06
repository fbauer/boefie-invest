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

(defn- parse-income [income]
  (for [line income
        :when (= "Revenue" (first line))]
    {:annual_sales (* 1e6 (Double/parseDouble (nth line (- (count line) 2) "NaN")))}))

(defn- parse-balance [balance] {})
(defn- parse-keyratios [keyratios] {})

(defn load [file-data]
  (let [balance_dict  {"Total current assets" :current_assets
                       "Total current liabilities" :current_liabilities
                       "Long-term debt" :long_term_debt
                       "Total liabilities" :total_liabilities
                       "Total assets" :total_assets
                       "Goodwill" :goodwill
                       "Intangible assets" :intangibles
                       }
        inputs  {:non_redeemable_preferred_stock 0
                 :redeemable_preferred_stock 0
                 :split_bonus_factor 1
                 :goodwill Double/NaN
                 :intangibles Double/NaN
                 :annual_sales Double/NaN
                 :current_assets Double/NaN
                 :current_liabilities Double/NaN
                 :long_term_debt Double/NaN
                 :isin ((first file-data) :isin) 
                 }
        file-data-set (set file-data)
        income (slurp-csv :incomestatement file-data-set)
        balance (slurp-csv :balancesheet file-data-set)
        keyratios (slurp-csv :keyratios file-data-set)
        ]
    (apply merge (cons inputs
                       (concat 
                       (parse-income income)
                       (parse-balance balance)
                       (parse-keyratios keyratios)
                       )))
    )) 