(ns boefie-invest.morningstar-test
  (:require [clojure.java.io :as io]
            [clojure.data :refer :all]
            [clojure.test :refer :all]
            [clojure.string :as string]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.java.io :refer [resource]]
            [clj-time.core :refer [date-time]]
            [clojure-csv.core :refer [parse-csv]]
            [boefie-invest.morningstar :refer :all]
            [boefie-invest.bigmoney :refer [as-money]]))

(deftest test-as-double
  (is (= 1.23 (as-double "1.23")))
  (is (= 1234.56 (as-double "1,234.56")))
  (is (Double/isNaN (as-double ""))))

(deftest test-parse-income
  (let [sample-input
        [["RHI AG  (RAD) CashFlowFlag INCOME STATEMENT"]
         ["Fiscal year ends in December. EUR in millions except per share data."
          "2008-12" "2009-12" "2010-12" "2011-12" "2012-12" "TTM"]
         ["Revenue" "1597" "1237" "1523" "1759" "1836" "1761"]]
        result (parse-income sample-input)]
    (is (= 5 (count result)))
    (is (= result
           [{:name "revenue"
             :amount (as-money "1597000000" "EUR")
             :date (date-time 2008 12 01)
             :kind :amounts}
            {:name "revenue"
             :amount (as-money "1237000000" "EUR")
             :date (date-time 2009 12 01)
             :kind :amounts}
            {:name "revenue"
             :amount (as-money "1523000000" "EUR")
             :date (date-time 2010 12 01)
             :kind :amounts}
            {:name "revenue"
             :amount (as-money "1759000000" "EUR")
             :date (date-time 2011 12 01)
             :kind :amounts}
            {:name "revenue"
             :amount (as-money "1836000000" "EUR")
             :date (date-time 2012 12 01)
             :kind :amounts}]))))

(deftest test-parse-income-corner-cases
  (are [sample-input expected] (= expected (parse-income sample-input))
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
       [{:name "revenue"
         :amount (as-money "23000000" "GBP")
         :date (date-time 2009 12 01)
         :kind :amounts}]))

(deftest test-parse-balance
  (let [sample-input
        [["OMV AG  (OMV) CashFlowFlag BALANCE SHEET"]
         ["Fiscal year ends in December. EUR in millions except per share data."
          "2008-12" "2009-12"]
         ["Total current assets" "5884" "5622"]
         ["Intangible assets" "807" "812"]
         ["Total assets" "21376" "21415"]
         ["Total current liabilities" "5816" "4732"]
         ["Long-term debt" "2526" "3197"]
         ["Total liabilities" "12013" "11380"]]
        result (parse-balance sample-input)]
    (is (= 13 (count result)))
    (is (= [{:kind :securities :name "OMV AG"}
            {:amount (as-money "5884000000" "EUR")
             :date (date-time 2008 12 01)
             :name "total current assets"
             :kind :amounts}
            {:amount (as-money "5622000000" "EUR")
             :date (date-time 2009 12 01)
             :name "total current assets"
             :kind :amounts}
            {:amount (as-money "807000000" "EUR")
             :date (date-time 2008 12 01)
             :name "intangible assets"
             :kind :amounts}
            {:amount (as-money "812000000" "EUR")
             :date (date-time 2009 12 01)
             :name "intangible assets"
             :kind :amounts}
            {:amount (as-money "21376000000" "EUR")
             :date (date-time 2008 12 01)
             :name "total assets"
             :kind :amounts}
            {:amount (as-money "21415000000" "EUR")
             :date (date-time 2009 12 01)
             :name "total assets"
             :kind :amounts}
            {:amount (as-money "5816000000" "EUR")
             :date (date-time 2008 12 01)
             :name "total current liabilities"
             :kind :amounts}
            {:amount (as-money "4732000000" "EUR")
             :date (date-time 2009 12 01)
             :name "total current liabilities"
             :kind :amounts}
            {:amount (as-money "2526000000" "EUR")
             :date (date-time 2008 12 01)
             :name "long-term debt"
             :kind :amounts}
            {:amount (as-money "3197000000" "EUR")
             :date (date-time 2009 12 01)
             :name "long-term debt"
             :kind :amounts}
            {:amount (as-money "12013000000" "EUR")
             :date (date-time 2008 12 01)
             :name "total liabilities"
             :kind :amounts}
            {:amount (as-money "11380000000" "EUR")
             :date (date-time 2009 12 01)
             :name "total liabilities"
             :kind :amounts}]
           result))))


(deftest test-parse-keyratios
  (let [sample-input
        [["Growth Profitability and Financial Ratios for RHI AG"]
         ["Financials"]
         ["" "2008-12" "2009-12" "TTM"]
         ["Earnings Per Share EUR" "3.05" "2.85" "3.22"]
         ["Dividends EUR" "0.38" "0.56" ""]
         ["Shares Mil"  "40" "40" "40"]
         ["Book Value Per Share EUR" "11.02" "12.07" "13.20"]]
        result (parse-keyratios sample-input)]
    (is (= 8 (count result)))
    (is (= result
           [{:name "earnings per share"
             :amount (as-money "3.05" "EUR")
             :date (date-time 2008 12 01)
             :kind :per_share_amounts}
            {:name "earnings per share"
             :amount (as-money "2.85" "EUR")
             :date (date-time 2009 12 01)
             :kind :per_share_amounts}
            {:name "dividends"
             :amount (as-money "0.38" "EUR")
             :date (date-time 2008 12 01)
             :kind :per_share_amounts}
            {:name "dividends"
             :amount (as-money "0.56" "EUR")
             :date (date-time 2009 12 01)
             :kind :per_share_amounts}
            {:amount 4.0E7
             :date (date-time 2008 12 01)
             :kind :shares}
            {:amount 4.0E7
             :date (date-time 2009 12 01)
             :kind :shares}
            {:name "book value per share"
             :amount (as-money "11.02" "EUR")
             :date (date-time 2008 12 01)
             :kind :per_share_amounts}
            {:name "book value per share"
             :amount (as-money "12.07" "EUR")
             :date (date-time 2009 12 01)
             :kind :per_share_amounts}]))))

(deftest test-parse-dir
  (let [my-file-seq (fn [_]
                      (map io/file
                           ["/root/2012-04-04/ignore-this"
                            "/root/2012-04-04/anisin Income Statement.csv"
                            "/root/2012-04-04/anisin Balance Sheet.csv"
                            ;; inject a newer item here to test that
                            ;; sorting works as intended
                            "/root/2013-04-04/anisin Key Ratios.csv"
                            "/root/2012-04-04/anisin Key Ratios.csv"
                            "/root/2012-04-04/anotherisin Income Statement.csv"
                            "/root/2012-04-04/anotherisin Balance Sheet.csv"
                            "/root/ignore-this/anisin Key Ratios.csv"
                            "/root/ignore-this/foobar"]))]
    (is (= [{:isin "anisin"
             :type :incomestatement
             :file (io/file "/root/2012-04-04/anisin Income Statement.csv")
             :date_added (date-time 2012 04 04)}
            {:isin "anisin"
             :type :balancesheet
             :file (io/file "/root/2012-04-04/anisin Balance Sheet.csv")
             :date_added (date-time 2012 04 04)}
            {:isin "anisin"
             :type :keyratios
             :file (io/file "/root/2012-04-04/anisin Key Ratios.csv")
             :date_added (date-time 2012 04 04)}
            {:isin "anotherisin"
             :type :incomestatement
             :file (io/file "/root/2012-04-04/anotherisin Income Statement.csv")
             :date_added (date-time 2012 04 04)}
            {:isin "anotherisin"
             :type :balancesheet
             :file (io/file "/root/2012-04-04/anotherisin Balance Sheet.csv")
             :date_added (date-time 2012 04 04)}
            {:isin "anisin"
             :type :keyratios
             :file (io/file "/root/2013-04-04/anisin Key Ratios.csv")
             :date_added (date-time 2013 04 04)}]
           (parse-dir "/root" my-file-seq)))))

(deftest test-load-data
  (let [result-records (load-data (resource "resources/morningstar"))
        expected-types #{:isins :shares :securities
                         :amounts :per_share_amounts}]
    (doseq [result-record result-records]
      (is (= 2 (count result-record)) "All records are pairs")
      (is (contains? expected-types (first result-record))
          (format "Type is one of %s"  expected-types))
      (is (coll? (second result-record))
          "The second item of each record is a collection")
      (case (first result-record)
        :isins (doseq [rec (second result-record)]
                 (is (= [:isin] (keys rec))))
        :securities (doseq [rec (second result-record)]
                      (is (= [:name :isin :date_added] (keys rec))))
        :shares (doseq [rec (second result-record)]
                  (is (= [:amount :date :isin :date_added] (keys rec))))
        :amounts (doseq [rec (second result-record)]
                   (is (= [:name :amount :date :isin :date_added]
                          (keys rec))))
        :per_share_amounts (doseq [rec (second result-record)]
                             (is (= [:name :amount :date :isin :date_added]
                                    (keys rec))))))))

(deftest parse-csv-corner-cases
  ;; Document how parse-csv behaves in corner cases to show why the
  ;; tests below behave the way they do in these corner cases

  ;; empty string
  ;;
  ;; An empty string results in an empty seq
  (is (empty? (parse-csv "")))

  ;; empty first line
  ;;
  ;; An empty line results in a vector with an empty string as first
  ;; element.
  (is (= [[""]] (parse-csv "\n"))))

(defspec parse-csv-never-generates-empty-vecs
  ;; a probabilistic test to show that parse-csv never generates empty
  ;; vectors as rows
  100
  (prop/for-all [csv-data gen/string]
                (not-any? #{[]} (parse-csv csv-data))))

(def name-gen (gen/fmap #(str % " (abc)" ) gen/string))

(defspec check-parse-security
  100
  (prop/for-all [contains-name (gen/tuple
                                (gen/tuple name-gen gen/string gen/string)
                                (gen/vector gen/string)
                                (gen/vector gen/string)
                                (gen/vector gen/string))]
                (= {:name (-> contains-name first first
                              (string/replace #" *\(abc\)" ""))
                    :kind :securities}
                   (parse-security contains-name))))

