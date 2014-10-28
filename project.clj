(defproject boefie-invest "0.1.0-SNAPSHOT"
  :description "Tools for security analysis according to Benjamin Graham"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ring-server "0.3.1"]
                 [noir-exception "0.2.2"]
                 [environ "1.0.0"]
                 [com.taoensso/timbre "3.3.1"]
                 [markdown-clj "0.9.55" :exclusions [com.keminglabs/cljx]]
                 [org.clojure/clojure "1.6.0"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.joda/joda-money "0.8"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [clj-time "0.6.0"]
                 [selmer "0.7.2"]
                 [prone "0.6.0"]
                 [com.taoensso/tower "3.0.2"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [korma "0.4.0"]
                 [im.chit/cronj "1.4.2"]
                 [lib-noir "0.9.4"]
                 [com.h2database/h2 "1.4.181"]]
  :repl-options {:init-ns boefie-invest.repl}
  :jvm-opts ["-server"]
  :plugins [[lein-ring "0.8.13"]
            [lein-environ "1.0.0"]
            [lein-ancient "0.5.5"]
            [lein-marginalia "0.7.1"]]
  :ring {:handler boefie-invest.handler/app,
         :init boefie-invest.handler/init,
         :destroy boefie-invest.handler/destroy}
  :profiles {:uberjar {:omit-source true, :env {:production true}, :aot :all},
             :production {:ring
                          {:open-browser? false, :stacktraces? false, :auto-reload? false}}
             :dev {:dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.1"]
                                  [pjstadig/humane-test-output "0.6.0"]
                                  [org.clojure/tools.namespace "0.2.4"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :source-paths ["dev"]
                   :env {:dev true}}}
  :min-lein-version "2.0.0")
