(ns flo-invest.core (:require [clojure.java.io :as io]
                              [clojure.set :only select]))

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
        income     (clojure.set/select #(= (:type %) :incomestatement) file-data-set)
        balance     (clojure.set/select #(= (:type %) :balancesheet) file-data-set)
        keyratios     (clojure.set/select #(= (:type %) :keyratios) file-data-set)
        ]

    inputs
  ))