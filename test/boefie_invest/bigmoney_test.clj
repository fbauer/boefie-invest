(ns boefie-invest.bigmoney-test
  (:require [clojure.test :refer :all]
            [boefie-invest.bigmoney :refer :all])
  (:import [org.joda.money BigMoney CurrencyMismatchException]
           [java.lang IllegalArgumentException]))

(deftest test-as-money
  (is (= (BigMoney/parse "USD 0.3") (as-money "0.3" "USD")))
  (is (= (BigMoney/parse "USD 0.33") (as-money "0.33" "USD")))
  (is (= (BigMoney/parse "EUR 1.23") (as-money "1.23" "EUR")))
  (is (= (BigMoney/parse "EUR 1234.56") (as-money "1,234.56" "EUR")))
  (is (= (BigMoney/parse "CNY 0.11") (as-money "0.11" "CNY")))
  (is (= nil (as-money "" "EUR")))
  (testing "Exceptional cases"
    (is (thrown? IllegalArgumentException  (as-money "0.3" "$")))))

(deftest money-and-nans
  (testing "My own numeric tower that handles nil and NaN as missing values"
    (doseq [func [plus minus multiply divide]]
      (let [msg (str "function: " func)]
        (is (= nil (func nil (BigMoney/parse "EUR 1.23"))) msg)
        (is (= nil (func Double/NaN (BigMoney/parse "EUR 1.23"))) msg)
        (is (= nil (func nil (BigMoney/parse "EUR 1.23"))) msg)
        (is (= nil (func Double/NaN (BigMoney/parse "EUR 1.23"))) msg)))))

(deftest test-plus
  (testing "Normal addition"
    (are [a b c] (= (plus a b) c)
         (as-money "1" "EUR") (as-money "1" "EUR") (as-money "2" "EUR")
         ;; plus adds arguments with the precision of the most precise argument
         (as-money "1" "EUR") (as-money "1.000" "EUR") (as-money "2.000" "EUR")))
  (testing "Exceptional cases"
    (are [exception a b] (thrown? exception (plus a b))
         IllegalArgumentException (as-money "1" "EUR") 2
         CurrencyMismatchException (as-money "1" "EUR") (as-money "1" "USD"))))

(deftest test-minus
  (testing "Normal subtraction"
    (are [a b c] (= (minus a b) c)
         (as-money "1" "EUR") (as-money "1" "EUR") (as-money "0" "EUR")
         ;; minus subtracts arguments with the precision of the most precise argument
         (as-money "1" "EUR") (as-money "1.000" "EUR") (as-money "0.000" "EUR")))
  (testing "Exceptional cases"
    (are [exception a b] (thrown? exception (minus a b))
         IllegalArgumentException (as-money "1" "EUR") 2
         CurrencyMismatchException (as-money "1" "EUR") (as-money "1" "USD"))))

(deftest test-multiply
  (testing "Normal multiplication"
    (are [a b c] (= (multiply a b) c)
         (as-money "1" "EUR") 2 (as-money "2" "EUR")
         ;; multiply multiplies arguments with the precision of the most
         ;; precise argument
         ;; Which can be surprising in the case of floats
         (as-money "1" "EUR") 3.00 (as-money "3.0" "EUR")
         (as-money "1" "EUR") 0.101 (as-money "0.101" "EUR")
         (as-money "1.0000" "EUR") 0.101 (as-money "0.1010000" "EUR")))
  (testing "Exceptional cases"
    (are [exception a b] (thrown? exception (multiply a b))
         IllegalArgumentException (as-money "1" "EUR") (as-money "1" "EUR"))))

(deftest test-divide
  (testing "Normal division"
    (are [a b c] (= (divide a b) c)
         (as-money "10" "EUR") 2 (as-money "5" "EUR")
         (as-money "1" "EUR") 2 (as-money "1" "EUR")
         (as-money "1" "EUR") 10 (as-money "0" "EUR")
         (as-money "1.0" "EUR") 10 (as-money "0.1" "EUR")
         ;; divide divides arguments with the precision of the most
         ;; precise argument.
         ;;
         ;; Floats can be surprising
         (as-money "1" "EUR") 3.00 (as-money "0" "EUR")
         (as-money "1.000" "EUR") 3.00 (as-money "0.333" "EUR")
         (as-money "1" "EUR") 0.101 (as-money "10" "EUR")
         (as-money "1.0000" "EUR") 0.101 (as-money "9.9010" "EUR")))
  (testing "Exceptional cases"
    (are [exception a b] (thrown? exception (divide a b))
         IllegalArgumentException (as-money "1" "EUR") (as-money "1" "EUR"))))


(deftest test-with-scale
  (testing "Test scaling the precision of money"
    (are [expected a scale] (= expected (with-scale a scale))
         nil nil 4
         (as-money "1" "EUR") (as-money "1" "EUR") 0
         (as-money "1" "EUR") (as-money "1.0" "EUR") 0
         (as-money "1.0" "EUR") (as-money "1" "EUR") 1)))
  (testing "Exceptional cases"
    (are [exception a scale] (thrown? exception (with-scale a scale))
         ArithmeticException (as-money "1.1" "EUR") 0))
