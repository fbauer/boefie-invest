(ns flo-invest.morningstar-test
  (:require [clojure.data :refer :all]
            [clojure.test :refer :all]
            [clj-time.core :refer [date-time]]
            [flo-invest.morningstar :refer :all]
            [flo-invest.bigmoney :refer [ as-money]]))

(deftest test-as-float
  (is (= (as-float "1.23") 1.23))
  (is (= (as-float "1,234.56") 1234.56))
  (is (Double/isNaN (as-float ""))))

(deftest test-parse-income
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

(deftest test-parse-income-corner-cases
  (are [sample-input expected] (= (parse-income sample-input) expected)
       ;; Truncated "Revenue" record
       [["BARCLAYS PLC (BCY) CashFlowFlag INCOME STATEMENT"]
        ["Fiscal year ends in December. GBP in millions except per share data."
         "2008-12" "TTM"]
        ["Revenue"]]
       []
       ;; No "Revenue" record
       [["BARCLAYS PLC (BCY) CashFlowFlag INCOME STATEMENT"]
        ["Fiscal year ends in December. GBP in millions except per share data."
         "2008-12" "2009-12" "2010-12" "2011-12" "2012-12" "TTM"]]
       []
       ;; Empty "Revenue" items  
       [["BARCLAYS PLC (BCY) CashFlowFlag INCOME STATEMENT"]
        ["Fiscal year ends in December. GBP in millions except per share data."
         "2008-12" "2009-12" "2010-12" "2011-12" "2012-12" "TTM"]
        ["Revenue" "" "" "" "" "" ""]]
       []
       ;; Missing "Revenue" items  
       [["BARCLAYS PLC (BCY) CashFlowFlag INCOME STATEMENT"]
        ["Fiscal year ends in December. GBP in millions except per share data."
         "2008-12" "2009-12" "2010-12" "2011-12" "2012-12" "TTM"]
        ["Revenue" "" "23" "" "" "" ""]]
       [{:name :annual_sales
         :amount (as-money "23000000" "GBP")
         :date (date-time 2009 12 01)}]))

(deftest test-parse-balance
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


(deftest test-parse-keyratios
  (let [sample-input [["Growth Profitability and Financial Ratios for RHI AG"]
                      ["Financials"]
                      ["" "2008-12" "2009-12" "TTM"]
                      ["Earnings Per Share EUR" "3.05" "2.85" "3.22"]
                      ["Dividends EUR" "0.38" "0.56" ""]
                      ["Shares Mil"  "40" "40" "40"]
                      ["Book Value Per Share EUR" "11.02" "12.07" "13.20"]]
        result (parse-keyratios sample-input)]
    (is (= (count result) 8))
    (is (= result [{:name :eps
                    :amount (as-money "3.05" "EUR")
                    :date (date-time 2008 12 01)}
                   {:name :eps
                    :amount (as-money "2.85" "EUR")
                    :date (date-time 2009 12 01)}
                   {:name :dividends
                    :amount (as-money "0.38" "EUR")
                    :date (date-time 2008 12 01)}
                   {:name :dividends
                    :amount (as-money "0.56" "EUR")
                    :date (date-time 2009 12 01)}
                   {:name :shares_outstanding
                    :amount 4.0E7
                    :date (date-time 2008 12 01)}
                   {:name :shares_outstanding
                    :amount 4.0E7
                    :date (date-time 2009 12 01)}
                   {:name :reported_book_value
                    :amount (as-money "11.02" "EUR")
                    :date (date-time 2008 12 01)}
                   {:name :reported_book_value
                    :amount (as-money "12.07" "EUR")
                    :date (date-time 2009 12 01)}]))))


 

