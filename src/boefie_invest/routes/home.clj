(ns boefie-invest.routes.home
  (:require [compojure.core :refer :all]
            [boefie-invest.layout :as layout]
            [boefie-invest.util :as util]
            [boefie-invest.db.core :as db]))

(defn home-page []
  (layout/render
   "home.html"
   {:content (util/md->html "/md/docs.md")
    :securities (db/get-securities)}))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page)))
