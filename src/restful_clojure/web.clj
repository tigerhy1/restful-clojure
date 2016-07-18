(ns restful_clojure.web
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :as middleware]
            [ring.util.response :refer [response]])
  (:import [com.couchbase.client.java Cluster CouchbaseCluster]))

(defn- str-to [num]
  (apply str (interpose ", " (range 1 (inc num)))))

(defn- str-from [num]
  (apply str (interpose ", " (reverse (range 1 (inc num))))))

;Cluster cluster = CouchbaseCluster.create();

(def responseString "Hello World2")
             
(defn userIdHandler [req]
    (let [userId (get-in req [:body :userIdExternal])]
    (prn userId)
    (response {:userId userId}))
    )

(def exHandler 
    (middleware/wrap-json-body userIdHandler {:keywords? true}))

(def finalHandler
    (middleware/wrap-json-response exHandler))

(defroutes routes
  (POST "/" {body :body} (slurp body))
  (GET "/count-up/:to" [to] (str-to (Integer. to)))
  (GET "/count-down/:from" [from] (str-from (Integer. from)))
  (POST "/get_add_user" req (finalHandler req)))

(defn -main []
  (run-jetty #'routes {:port 8080 :join? false}))