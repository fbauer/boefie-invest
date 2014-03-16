(ns flo-invest.core-test
  (:require [clojure.data :refer :all]
            [clojure.test :refer :all]
            [flo-invest.core :refer :all]
            [flo-invest.bigmoney :refer [ as-money]]
            [clojure.java.io :as io]
            [cheshire.core :refer :all]
            [cheshire.factory :as factory])
  (:import [org.joda.money BigMoney CurrencyUnit]
           [java.math  RoundingMode]))

(def data-dir
  "/home/flo/geldanlage/aktienscreen_2013_10_05/data/morningstar/2013_10_05/")

(deftest test-data-available
  (let [testdata (parse-dir data-dir)]
    (testing "loading test data succeeds"
      (is (not= testdata []))
      (is (= 3 (count  (first (vals testdata))))))))

(deftest test-loading-of-hfc-stock
  (let [testdata data-dir
        my-file (fn [filename] (io/file (str testdata filename)))
        input (load-data [{:isin "US4361061082" :type :balancesheet
                           :file (my-file  "US4361061082 Balance Sheet.csv")}
                          {:isin "US4361061082" :type :incomestatement
                           :file (my-file "US4361061082 Income Statement.csv")}
                          {:isin "US4361061082" :type :keyratios
                           :file (my-file "US4361061082 Key Ratios.csv")}])

        expected {:isin "US4361061082"
                  :annual_sales (as-money "20091000000" "USD")
                  :currency "USD"
                  :current_assets (as-money "4470000000" "USD")
                  :current_liabilities (as-money "1654000000" "USD")
                  :dividends [(as-money "0.06" "USD")
                              (as-money "0.07" "USD")
                              (as-money "0.10" "USD")
                              (as-money "0.15" "USD")
                              (as-money "0.23" "USD")
                              (as-money "0.30" "USD")
                              (as-money "0.30" "USD")
                              (as-money "0.30" "USD")
                              (as-money "0.34" "USD")
                              (as-money "0.60" "USD")]
                  :eps [(as-money "0.36" "USD")
                        (as-money "0.65" "USD")
                        (as-money "1.33" "USD")
                        (as-money "2.29" "USD")
                        (as-money "2.99" "USD")
                        (as-money "1.19" "USD")
                        (as-money "0.20" "USD")
                        (as-money "0.97" "USD")
                        (as-money "6.42" "USD")
                        (as-money "8.38" "USD")]
                  :goodwill (as-money "2338000000" "USD")
                  :intangibles (as-money "169000000" "USD")
                  :long_term_debt (as-money "1336000000" "USD")
                  :non_redeemable_preferred_stock 0.0000000
                  :redeemable_preferred_stock 0.0000000
                  :reported_book_value (as-money "29.74" "USD")
                  :shares_outstanding 206000000.0
                  :split_bonus_factor 1.0
                  :tangible_book_value (as-money "17.5700970874" "USD")
                  :total_assets (as-money "10329000000" "USD")
                  :total_liabilities (as-money "4276000000" "USD")}]
    (testing "Result from calling load-data is as expected"
      (is (= input expected)))))

(deftest  test-data-loads-without-hickups
  (testing "test that all data files can be loaded"
    (doseq [testdata (parse-dir data-dir)]
      (is (not= {} (load-data (last testdata))) (first testdata)))))

(defn eq
  "Equality helper to test equality of data generated by Cheshire that
  includes NaNs."
  [a b]
  (cond
   (and (vector? a) (vector? b))
   (cons (= (count a) (count b)) (map eq a b))
   (and (number? a) (number? b) (Double/isNaN a) (Double/isNaN b))
   true
   (and (number? a) (number? b))
   (== a b)
   (and (= (class a) BigMoney) (= (class b) BigMoney))
   ;; compare BigMoney to 6 digits after decimal point
   (.isEqual (.withScale a 6 RoundingMode/HALF_UP)
             (.withScale b 6 RoundingMode/HALF_UP))
   :else (= a b)))

(defn bigmoney [currency amount]
  (if (Double/isNaN amount) nil 
      (BigMoney/of (CurrencyUnit/getInstance currency) (double amount))))

(deftest compare-with-python-results
  (let [json-path "/home/flo/geldanlage/aktienscreen_2013_10_05/morningstar.json"
        py-data (binding [factory/*json-factory*
                          (factory/make-json-factory {:allow-non-numeric-numbers true})]
                  (parse-stream (clojure.java.io/reader json-path)))
        clj-data (vec (for [input (parse-dir data-dir )] (load-data (last input))))

        py-convert (fn [py key] (case key
                                  :isin (py key)
                                  :currency (py key)
                                  :shares_outstanding (py key)
                                  :non_redeemable_preferred_stock (py key)
                                  :redeemable_preferred_stock (py key)
                                  :split_bonus_factor (py key)
                                  :eps (vec (map #(bigmoney (py :currency) %) (py key)))
                                  :dividends (vec (map #(bigmoney (py :currency) %) (py key)))
                                  (bigmoney (py :currency) (py key))))
        py-keys [:isin :currency :annual_sales :current_assets :current_liabilities
                 :dividends :eps :goodwill :intangibles :long_term_debt
                 :non_redeemable_preferred_stock :redeemable_preferred_stock
                 :reported_book_value :shares_outstanding :split_bonus_factor
                 :tangible_book_value :total_assets :total_liabilities]
        py-map (map #(zipmap py-keys %) py-data)
        ]
    (doseq [[py clj]
            (map list (sort-by :isin py-map) (sort-by :isin clj-data))
            key py-keys]
      (is (eq (py-convert py key) (clj key)) (str (py :isin) " " key " " (py key))))))


(deftest test-parse-keyratios
  (let [sample-input [["Growth Profitability and Financial Ratios for RHI AG"]
["Financials"]
["" "2003-12" "2004-12" "2005-12" "2006-12" "2007-12" "2008-12" "2009-12" "2010-12" "2011-12" "2012-12" "TTM"]
["Earnings Per Share EUR" "2.04" "13.58" "38.55" "157.01" "2.77" "2.48" "0.52" "2.66" "3.05" "2.85" "3.22"]
["Dividends EUR" "" "" "" "" "" "" "" "" "0.38" "0.56" ""]
["Shares Mil" "39" "7" "2" "1" "36" "38" "40" "40" "40" "40" "40"]
["Book Value Per Share EUR" "" "" "" "" "" "" "" "8.06" "11.02" "12.07" "13.20"]]
        result (parse-keyratios nil sample-input)]
    (is (= (count result) 7))
    (is (= result [nil nil nil
 {:eps
  [(as-money "2.04" "EUR")
   (as-money "13.58" "EUR")
   (as-money "38.55" "EUR")
   (as-money "157.01" "EUR")
   (as-money "2.77" "EUR")
   (as-money "2.48" "EUR")
   (as-money "0.52" "EUR")
   (as-money "2.66" "EUR")
   (as-money "3.05" "EUR")
   (as-money "2.85" "EUR")]}
 {:dividends
  [nil
   nil
   nil
   nil
   nil
   nil
   nil
   nil
   (as-money "0.38" "EUR")
   (as-money "0.56" "EUR")],
  :currency "EUR"}
 {:shares_outstanding 4.0E7}
 {:reported_book_value (as-money "12.07" "EUR")}
]))))
