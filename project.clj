(defproject boefie-invest "0.1.0-SNAPSHOT"
  :description "Tools for security analysis according to Benjamin Graham"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.joda/joda-money "0.8"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [clj-time "0.6.0"]
                 [korma "0.3.0-RC6"]]
  :plugins [[lein-marginalia "0.7.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [difftest "1.3.8"]]
                   :source-paths ["dev"]}})
