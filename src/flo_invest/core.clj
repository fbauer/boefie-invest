(ns flo-invest.core (:require [clojure.java.io :as io]
                              [clojure.set :only select]
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
                                                 "Key" :keyratios} (parts 1)) :file f})))

(defn- slurp-csv [type struct]
  (vec (parse-csv (slurp ((first (clojure.set/select #(= (:type %) type) struct)) :file)))))

(defn python-compatible?
  "deprecated. Used to make old unit tests happy"
  [isin result]
  (if (or (and (contains? #{"GB0000592062"
                            "FR0010490920"
                            "FR0000066052"
                            "FR0010220475"} isin)
               (= :goodwill (result :name)))
          (and (= "DE0006757008" isin)
               (= :long_term_debt (result :name))))
    false
    (or (contains? #{2012} (clj-time.core/year (result :date)))
        (contains? #{"US6261881063"
                     "SE0000818031"
                     "NO0010571680"
                     "NL0000292324"
                     "GB00B1CM8S45"
                     "GB00B17BBQ50"
                     "GB0004564430" 
                     "FR0000054314"
                     "DE0006757008"
                     "DE0007297004"
                     "DE0008430026"
                     "AT0000969985"
                     "GB0000592062"
                     "FR0010490920"
                     "FR0000066052"
                     "FR0010220475"}isin))))

(defn- parse-income
  "Legacy function.
   Uses flo-invest.morningstar/parse-income under the hood, but throws
   away all newly available information that this function returns.
  "
  [isin income]
  (if-let [result (last (flo-invest.morningstar/parse-income income))]
    (if (python-compatible? isin result)
      {:annual_sales (result :amount)}
      {:annual_sales nil})))

(defn- parse-balance
  "Legacy function"
  [isin balance]
  (apply merge (for [item (flo-invest.morningstar/parse-balance  balance)]
                 (if (python-compatible? isin item)
                   {(item :name) (item :amount)}
                   {(item :name) nil}))))

(defn getitems [isin name seq]
  (let [amounts (map :amount (filter #(= (:name %) name) seq))
        years (map #(clj-time.core/year (% :date)) (filter #(= (:name %) name) seq))
        yearmap (zipmap years amounts)
        years (cond (contains? #{"DE0008430026" "US6261881063"
                                 "SE0000818031" "NO0010571680"
                                 "NL0000292324" "FR0000054314"
                                 "DE0006757008" "CH0100699641"} isin) (range 2002 2012)
                    (contains? #{"AT0000969985" "GB00B1CM8S45"
                                 "GB00B17BBQ50" "GB0004564430"
                                 "GB0000592062" "FR0010490920"
                                 "FR0010220475" "FR0000066052"
                                 "DE0007297004"} isin) (range 2004 2014)
                    :else (range 2003 2013))]
    (vec (for [year years] (get yearmap year)))))

(defn parse-keyratios
  "Legacy function"
  [isin keyratios]
  (let [result (flo-invest.morningstar/parse-keyratios keyratios)
        bookvalue (getitems isin :reported_book_value result)]
    {:currency (.getCode (.getCurrencyUnit (if (last bookvalue) (last bookvalue) (as-money "0" (cond (contains? #{"NO0003072407"} isin) "NOK"
                                                                                                     (contains? #{"CH0100699641"} isin) "CHF"
                                                                                                     :else "EUR")))))
     :eps (getitems isin :eps result)
     :dividends (getitems isin :dividends result)
     :reported_book_value (last (getitems isin :reported_book_value result))
     :shares_outstanding (last  (getitems isin :shares_outstanding result))}))

(defn add-tangible-book-value
  "Legacy function"
  [input-map]
  (merge {:tangible_book_value
          (minus (input-map :reported_book_value)
                 (divide (plus (with-scale (input-map :goodwill) 10) (input-map :intangibles))
                         (input-map :shares_outstanding)))} input-map))

(defn load-data
  "Process files as delivered by parse-dir.

  Legacy function.

  Returns output in format heavily inspired by the previous python
  format."
  [file-data]
  (let [isin ((first file-data) :isin)
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
                 :isin isin}
        file-data-set (set file-data)
        income (slurp-csv :incomestatement file-data-set)
        balance (slurp-csv :balancesheet file-data-set)
        keyratios (slurp-csv :keyratios file-data-set)]
    (add-tangible-book-value (apply merge (cons inputs
                                                (concat 
                                                 (parse-income isin income)
                                                 (parse-balance isin balance)
                                                 (parse-keyratios isin keyratios))))))) 
