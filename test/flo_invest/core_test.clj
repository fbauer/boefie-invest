(ns flo-invest.core-test
  (:require [clojure.test :refer :all]
            [flo-invest.core :refer :all]
            ))




(deftest test-data-available
  (let  [testdata (parse-dir "/home/flo/geldanlage/aktienscreen_2013_10_05/data/morningstar/2013_10_05/")]
    (testing "loading test data succeeds"
      (is (not= testdata []))
      (is (= 3 (count  (first (vals testdata)))))
      )))