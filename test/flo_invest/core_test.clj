(ns flo-invest.core-test
  (:require [clojure.data :refer :all]
            [clojure.test :refer :all]
            [flo-invest.core :refer :all]
            [flo-invest.bigmoney :refer [ as-money]]
            [clojure.java.io :as io]
            [cheshire.core :refer :all]
            [cheshire.factory :as factory])
  (:import [org.joda.money BigMoney CurrencyUnit]
           [java.math  RoundingMode]))

