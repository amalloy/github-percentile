(ns github-percentile.core
  (:use [compojure.core :only [defroutes GET POST]]
        [compojure.route :only [resources]]
        [ring.adapter.jetty :only [run-jetty]]
        [ring.middleware.params :only [wrap-params]]
        [hiccup.page-helpers :only [html5 doctype include-css]]
        [hiccup.form-helpers :only [form-to label text-field submit-button]])
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [tentacles.orgs :as orgs]
            [tentacles.users :as users]
            [ring.util.response :as response]))

(defonce members (memoize orgs/members))

(defonce user-id (memoize (comp :id users/user)))

(defn percentile
  "If you were a Github employee, what percentile would your user ID be?"
  ([username org]
     (let [id (user-id username)
           hubbers (members org)
           older (filter (partial > id) (map :id hubbers))]
       (int (* 100 (/ (count older) (count hubbers))))))
  ([username] (percentile username "github")))

(defn message [number who & [org]]
  (format "%s%% of %s employees have had an account longer than <a href='http://github.com/%s'>%s</a> has."
          number (or org "Github") who who))

(defn request-form []
  (form-to [:post "/"]
           (label "who" "Github user name")
           (text-field "who")
           (submit-button "Calculate")))

(defn layout [body]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Github Percentile"]
    (include-css "/stylesheets/style.css"
                 "/stylesheets/base.css"
                 "/stylesheets/skeleton.css")
    (include-css "http://fonts.googleapis.com/css?family=Electrolize")]
   [:body
    [:div#header
     [:h1.container
      [:a {:href "/"} "Github Percentile"]]]
    [:div#content.container body]
    [:div#footer.container
     [:p "Get " [:a {:href "https://github.com/amalloy/github-percentile"}
                 "the source"] "."]]]))

(defroutes app
  (resources "/")
  (GET "/api/:who/:org" [who org]
    {:status 200 :headers {"Content-Type" "application/json"}
     :body (str "{"
                (s/join ", "
                        (for [[k v] {:user who :org org :percentile (percentile who org)}]
                          (format "\"%s\": %s" (name k) (pr-str v))))
                "}")})
  (GET "/:who" [who]
    (layout [:div {:id "result"}
             [:p (message (percentile who) who)]
             (request-form)]))
  (GET "/:who/:org" [who org]           ; easter egg!
    (layout [:div {:id "result"}
             [:p (message (percentile who org) who org)]
             (request-form)]))
  (POST "/" {params :params}
    (response/redirect (str "/" (params "who"))))
  (GET "/" [params]
    (layout [:div {:id "welcome"}
             (request-form)])))

(defn -main [& args]
  (run-jetty (wrap-params #'app)
             {:port (Integer. (or (System/getenv "PORT") "8080"))}))
