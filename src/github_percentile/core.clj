(ns github-percentile.core
  (:use [compojure.core :only [defroutes GET]]
        [ring.adapter.jetty :only [run-jetty]])
  (:require [tentacles.orgs :as orgs]
            [tentacles.users :as users]))

(def members (memoize (fn members [org] (orgs/members org))))

(defn percentile
  "If you were a Github employee, what percentile would your user ID be?"
  ([username org]
     (let [id (:id (users/user username))
           hubbers (members org)
           older (filter (partial > id) (map :id hubbers))]
       (int (* 100 (/ (count older) (count hubbers))))))
  ([username] (percentile username "github")))

(defroutes main-routes
  (GET "/:who" [who]
    (str (percentile who) "% of Github employees have had a Github account longer than " who " has.")))

(defn -main [& args]
  (run-jetty #'main-routes {:port (Integer/parseInt (System/getenv "PORT"))}))
