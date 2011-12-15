(ns github-percentile.core
  (:use [compojure.core :only [defroutes GET]]
        [ring.adapter.jetty :only [run-jetty]])
  (:require [tentacles.orgs :as orgs]
            [tentacles.users :as users]))

(defn percentile
  "If you were a Github employee, what percentile would your user ID be?"
  [username]
  (let [id (:id (users/user username))
        hubbers (orgs/members "github")
        older (filter (partial > id) (map :id hubbers))]
    (int (* 100 (/ (count older) (count hubbers))))))

(defroutes main-routes
  (GET "/:who" [who]
    (str "Github percentile for " who ": " (percentile who))))

(defn -main [& args]
  (run-jetty #'main-routes {:port (Integer/parseInt (System/getenv "PORT"))}))
