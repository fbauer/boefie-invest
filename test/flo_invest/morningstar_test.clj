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
                      ["Revenue" "1597" "1237" "1523" "1759" "1836" "1761"]]
        result (parse-income sample-input)]
    (is (= (count result) 5))
    (is (= result
           [{:name :annual_sales
             :amount (as-money "1597000000" "EUR")
             :date (date-time 2008 12 01)}
            {:name :annual_sales
             :amount (as-money "1237000000" "EUR")
             :date (date-time 2009 12 01)}
            {:name :annual_sales
             :amount (as-money "1523000000" "EUR")
             :date (date-time 2010 12 01)}
            {:name :annual_sales
             :amount (as-money "1759000000" "EUR")
             :date (date-time 2011 12 01)}
            {:name :annual_sales
             :amount (as-money "1836000000" "EUR")
             :date (date-time 2012 12 01)}]))))


(deftest test-parse-income-bad-doc
  (let [sample-input [["BARCLAYS PLC (BCY) CashFlowFlag INCOME STATEMENT"]
                      ["Fiscal year ends in December. GBP in millions except per share data."
                       "2008-12" "2009-12" "2010-12" "2011-12" "2012-12" "TTM"]
                      ["Revenue"]]]
    (is (= (parse-income sample-input) []))))

(deftest test-parse-balance-good-doc
  (let [sample-input [["OMV AG  (OMV) CashFlowFlag BALANCE SHEET"]
                      ["Fiscal year ends in December. EUR in millions except per share data."
                       "2008-12" "2009-12"]
                      ["Total current assets" "5884" "5622"]
                      ["Intangible assets" "807" "812"]
                      ["Total assets" "21376" "21415"]
                      ["Total current liabilities" "5816" "4732"]
                      ["Long-term debt" "2526" "3197"]
                      ["Total liabilities" "12013" "11380"]]
        result (parse-balance sample-input)]
    (is (= (count result) 12))
    (is (= result
           [{:amount (as-money "5884000000" "EUR")
             :date (date-time 2008 12 01)
             :name :current_assets}
            {:amount (as-money "5622000000" "EUR")
             :date (date-time 2009 12 01)
             :name :current_assets}
            {:amount (as-money "807000000" "EUR")
             :date (date-time 2008 12 01)
             :name :intangibles}
            {:amount (as-money "812000000" "EUR")
             :date (date-time 2009 12 01)
             :name :intangibles}
            {:amount (as-money "21376000000" "EUR")
             :date (date-time 2008 12 01)
             :name :total_assets}
            {:amount (as-money "21415000000" "EUR")
             :date (date-time 2009 12 01)
             :name :total_assets}
            {:amount (as-money "5816000000" "EUR")
             :date (date-time 2008 12 01)
             :name :current_liabilities}
            {:amount (as-money "4732000000" "EUR")
             :date (date-time 2009 12 01)
             :name :current_liabilities}
            {:amount (as-money "2526000000" "EUR")
             :date (date-time 2008 12 01)
             :name :long_term_debt}
            {:amount (as-money "3197000000" "EUR")
             :date (date-time 2009 12 01)
             :name :long_term_debt}
            {:amount (as-money "12013000000" "EUR")
             :date (date-time 2008 12 01)
             :name :total_liabilities}
            {:amount (as-money "11380000000" "EUR")
             :date (date-time 2009 12 01)
             :name :total_liabilities}]))))


