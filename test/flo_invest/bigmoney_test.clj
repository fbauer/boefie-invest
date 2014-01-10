(ns flo-invest.bigmoney-test
  (:require [clojure.test :refer :all]
            [flo-invest.bigmoney :refer :all])
  (:import [org.joda.money BigMoney]))

(deftest test-as-money
  (is (= (as-money "0.3" "USD") (BigMoney/parse "USD 0.3")))
  (is (= (as-money "0.33" "USD") (BigMoney/parse "USD 0.33")))
  (is (= (as-money "1.23" "EUR") (BigMoney/parse "EUR 1.23")))
  (is (= (as-money "1,234.56" "EUR") (BigMoney/parse "EUR 1234.56")))
  (is (= (as-money "0.11" "CNY") (BigMoney/parse "CNY 0.11")))
  (is (= (as-money "" "EUR") nil)))

(deftest money-and-nans
  (testing "My own numeric tower that handles nil and NaN as missing values"
    (doseq [func [plus minus multiply divide]]
      (let [msg (str "function: " func)]
        (is (= (func (BigMoney/parse "EUR 1.23") nil) nil) msg)
        (is (= (func (BigMoney/parse "EUR 1.23") Double/NaN) nil) msg)
        (is (= (func nil (BigMoney/parse "EUR 1.23")) nil) msg)
        (is (= (func Double/NaN (BigMoney/parse "EUR 1.23")) nil) msg)
        ))))

