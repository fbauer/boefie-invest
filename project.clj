(defproject flo-invest "0.1.0-SNAPSHOT"
  :description "Security analysis according to Graham"
  :url ""
  :license {:name ""
            :url ""}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [cheshire "5.3.0"]
                 [org.joda/joda-money "0.8"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [clj-time "0.6.0"]
                 [reiddraper/simple-check "0.5.3"]
                 ]
  :plugins [[lein-marginalia "0.7.1"]])
