(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test]
   [difftest.core :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [boefie-invest.morningstar]))

(def system
  "A Var containing an object representing the application under
  development."
  {:tests ['boefie-invest.bigmoney-test
           'boefie-invest.core-test
           'boefie-invest.test.handler
           'boefie-invest.database-test
           'boefie-invest.morningstar-test]})

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  ;; TODO
  )

(defn start
  "Starts the system running, updates the Var #'system."
  []
  ;; TODO
  )

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  ;; TODO
  )

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))

(defn run-tests
  "Run unit tests"
  []
  (apply test/run-tests (:tests system)))

(defn run-difftests
  "Run unit tests"
  []
  (test/activate)
  (apply test/test-ns (:tests system)))
