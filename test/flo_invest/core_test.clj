(ns flo-invest.core-test
  (:require [clojure.test :refer :all]
            [flo-invest.core :refer :all]
            [clojure.java.io :as io]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(defn load-testdata []
  (let [datadir "/home/flo/geldanlage/aktienscreen_2013_10_05/data/morningstar/2013_10_05/"]
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
                                                   "Key" :keyratios} (parts 1))  :file f} ))))


(deftest test-data-available
  (let  [testdata (load-testdata)]
    (testing "loading test data succeeds"
      (is (not= testdata []))
      (is (= 3 (count  (first (vals testdata)))))
      )))