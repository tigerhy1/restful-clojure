(ns restful_clojure.web
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body, wrap-json-response]]
            [ring.util.response :refer [response]]
            [restful_clojure.couchbase_con :refer [bucket]]
            [restful_clojure.couchbase :as c])
            
  (:import [com.couchbase.client.java Cluster CouchbaseCluster]))

(defn- str-to [num]
  (apply str (interpose ", " (range 1 (inc num)))))

(defn- str-from [num]
  (apply str (interpose ", " (reverse (range 1 (inc num))))))

;Cluster cluster = CouchbaseCluster.create();

(def responseString "Hello World2")


(def test-cb 
    (c/get-doc bucket "hello"))
    ;(prn "test-cb"))

(defn get-uid
    [unionId]
    (let [k (str "unionid2uid" "_" unionId)
          content (c/get-doc bucket k)]
      (prn content)
      (if (nil? content) 0 (:uid content))
    ))

(defn add-user
    []
    2)

(defn get-add-user-db 
    [openId]
    (let [uid (get-uid openId)]
    ;(prn "in get-add-user-db")
    ;add-user add () around, then mean apply the function, 
    ;otherwise just function object.
    (cond (= 0 uid) (add-user)
          :else uid)))


(defn get-add-user [req]
    (let [body (:body req)
          openId (:openId body)
          unionId (:unionId body)
          weixinName (:weixinName body)
          uid (get-add-user-db openId)]
    ;(prn (get-add-user-db openId unionId weixinName))
    (prn openId)
    (response {:openId uid}))
    )

(defn composer
    [handler]
    (-> handler
        (wrap-json-body {:keywords? true :bigdecimals? true})
        wrap-json-response))

(defroutes routes
  (POST "/" {body :body} (slurp body))
  (GET "/count-up/:to" [to] (str-to (Integer. to)))
  (GET "/count-down/:from" [from] (str-from (Integer. from)))
  (POST "/get-add-user" req ((composer get-add-user) req)))

(defn -main []
  (run-jetty #'routes {:port 8080 :join? false}))