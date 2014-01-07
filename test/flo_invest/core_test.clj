(ns flo-invest.core-test
  
  (:require [clojure.data :refer :all]
            [clojure.test :refer :all]
            [flo-invest.core :refer :all]
            [clojure.java.io :as io]
            ))




(deftest test-data-available
  (let  [testdata (parse-dir "/home/flo/geldanlage/aktienscreen_2013_10_05/data/morningstar/2013_10_05/")]
    (testing "loading test data succeeds"
      (is (not= testdata []))
      (is (= 3 (count  (first (vals testdata)))))
      )))

(deftest test-loading-of-hfc-stock
  (let [testdata "/home/flo/geldanlage/aktienscreen_2013_10_05/data/morningstar/2013_10_05/"
        input (load-data [{:isin "US4361061082" :type :balancesheet :file (io/file (str testdata  "US4361061082 Balance Sheet.csv")) }
                     {:isin "US4361061082" :type :incomestatement :file (io/file (str testdata  "US4361061082 Income Statement.csv"))}
                     {:isin "US4361061082" :type :keyratios :file (io/file (str testdata  "US4361061082 Key Ratios.csv"))}])

        expected {:isin "US4361061082",
                    :annual_sales 20091e6,
                    :currency "USD",
                    :current_assets 4470e6,
                    :current_liabilities 1654e6,
                    :dividends [0.06, 0.07, 0.1, 0.15, 0.23, 0.3, 0.3, 0.3, 0.34, 0.6],
                    :eps [0.36, 0.65, 1.33, 2.29, 2.99, 1.19, 0.2, 0.97, 6.42, 8.38],
                    :goodwill 2338e6,
                    :intangibles 169e6,
                    :long_term_debt 1336e6,
                    :non_redeemable_preferred_stock 0.0e6,
                    :redeemable_preferred_stock 0.0e6,
                    :reported_book_value 29.74,
                    :shares_outstanding 206e6,
                    :split_bonus_factor 1.0,
                    :tangible_book_value 17.57009708737864,
                    :total_assets 10329.0e6,
                    :total_liabilities 4276e6}]
    (testing "Result from calling load-data is as expected"
      (is (= (diff input expected)  [nil nil expected]))
      )))

(deftest  test-data-loads-without-hickups
  (doseq [testdata (parse-dir "/home/flo/geldanlage/aktienscreen_2013_10_05/data/morningstar/2013_10_05/")]
    (is (not= {} (load-data (last  testdata))))))

(deftest test-as-float
  (is (= (as-float "1.23") 1.23))
  (is (= (as-float "1,234.56") 1234.56))
  (is (Double/isNaN (as-float ""))))

