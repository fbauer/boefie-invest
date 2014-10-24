(ns boefie-invest.bigmoney
  (:import [org.joda.money BigMoney]
           [java.math RoundingMode])
  )

(defn as-money [a_string currency_symbol]
  (if (= a_string  "") nil
      (BigMoney/parse (str currency_symbol " " (clojure.string/replace  a_string "," "")))))

(defn plus [a b]
  (if (or (and (number? a) (Double/isNaN a))
          (and (number? b) (Double/isNaN b))
          (nil? a)
          (nil? b)
          ) nil
            (.plus a b)))

(defn minus [a b]
  (if (or (and (number? a) (Double/isNaN a))
          (and (number? b) (Double/isNaN b))
          (nil? a)
          (nil? b)
          ) nil
            (.minus a b)))

(defn multiply [a b]
  (if (or (and (number? a) (Double/isNaN a))
          (and (number? b) (Double/isNaN b))
          (nil? a)
          (nil? b)
          ) nil
            (.multipliedBy a b)))

(defn divide
  ([a b]
      (if (or (and (number? a) (Double/isNaN a))
              (and (number? b) (Double/isNaN b))
              (nil? a)
              (nil? b)
              ) nil
                (.dividedBy a b RoundingMode/HALF_UP))))

(defn with-scale [bigmoney-or-nil scale]
  (if (nil? bigmoney-or-nil) nil (.withScale bigmoney-or-nil scale)))
