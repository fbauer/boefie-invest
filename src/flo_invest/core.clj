(ns flo-invest.core (:require [clojure.java.io :as io]))

(defn parse-dir [datadir]
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
                                                 "Key" :keyratios} (parts 1))  :file f})))