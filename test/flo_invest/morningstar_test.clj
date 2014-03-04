(ns flo-invest.morningstar-test
  (:require [clojure.data :refer :all]
            [clojure.test :refer :all]
            [clj-time.core :refer [date-time]]
            [flo-invest.morningstar :refer :all]
            [flo-invest.bigmoney :refer [ as-money]]))

(deftest test-parse-income-good-doc
  (let [sample-input [["RHI AG  (RAD) CashFlowFlag INCOME STATEMENT"]
                      ["Fiscal year ends in December. EUR in millions except per share data."
                       "2008-12" "2009-12" "2010-12" "2011-12" "2012-12" "TTM"]
                      ["Revenue" "1597" "1237" "1523" "1759" "1836" "1761"]]]
    (is (= (parse-income sample-input)
           [{:name :annual_sales
             :amount (as-money "1597000000" "EUR")
             :date (date-time 2008 12 01)}
            {:name :annual_sales
             :amount (as-money "1237000000" "EUR")
             :date (date-time 2009 12 01)}
            {:name :annual_sales :amount
             (as-money "1523000000" "EUR")
             :date (date-time 2010 12 01)}
            {:name :annual_sales
             :amount (as-money "1759000000" "EUR")
             :date (date-time 2011 12 01)}
            {:name :annual_sales
             :amount (as-money "1836000000" "EUR")
             :date (date-time 2012 12 01)}
            ]))))


(deftest test-parse-income-good-doc
  (let [sample-input [["BARCLAYS PLC (BCY) CashFlowFlag INCOME STATEMENT"]
                      ["Fiscal year ends in December. GBP in millions except per share data."
                       "2008-12" "2009-12" "2010-12" "2011-12" "2012-12" "TTM"]
                      ["Revenue"]]]
    (is (= (parse-income sample-input) []))))

