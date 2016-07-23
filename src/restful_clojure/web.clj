(ns restful_clojure.web
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body, wrap-json-response]]
            [ring.util.response :refer [response]]
            [restful_clojure.couchbase_con :refer [bucket]]
            [restful_clojure.couchbase :as c]
            [restful_clojure.movie :as m]
            [restful_clojure.share :as s])
            
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
          doc (c/get-doc bucket k)
          content (:content doc)]
      (prn (str "in get-uid " content))
      (if (nil? content) 0 (get content "uid"))
    ))

(defn create-user
    [openId unionId weixinName]
    (let [k "max_uid"
          uid (c/counter! bucket k)]
        (prn k)
        (prn uid)
          {:uid uid
          :openId openId
          :unionId unionId
          :weixinName weixinName
          }
        ))

(defn add-user
    [openId unionId weixinName]
    (let [user (create-user openId unionId weixinName)
          uid (:uid user)
          k (str "user_" uid)
          k2 (str "unionid2uid_" unionId)]
        (c/replace! bucket k2 {:uid uid})
        (c/replace! bucket k user)))

(defn get-add-user-db 
    [openId unionId weixinName]
    (let [uid (get-uid unionId)]
    ;(prn "in get-add-user-db")
    ;add-user add () around, then mean apply the function, 
    ;otherwise just function object.
    (cond (= 0 uid) (add-user openId unionId weixinName)
          :else uid)))

(defn get-add-user [req]
    (let [body (:body req)
          openId (:openId body)
          unionId (:unionId body)
          weixinName (:weixinName body)
          uid (get-add-user-db openId unionId weixinName)]
    ;(prn (get-add-user-db openId unionId weixinName))
    (prn openId)
    (response {:uid uid}))
    )

(defn add-share [req]
    (let [body (:body req)
          uid (:uid body)
          movieName (:movieName body)
          desc (:desc body)
          mid (m/get-add-movie movieName)]
        (s/add-share uid mid desc)))

(defn get-share [req]
    (let [body (:body req)
          offset (:offset body)
          size (:size body)]
        (response(s/get-share offset size))))

(defn composer
    [handler]
    (-> handler
        (wrap-json-body {:keywords? true :bigdecimals? true})
        wrap-json-response))

(defn get-test
    [req]
    (let [a {:uid 1 :name "hu"}
          b {:uid 2 :name "yuan"}
          l (list a b)]
        (response l))
    )

(defroutes routes
  (POST "/" {body :body} (slurp body))
  (GET "/count-up/:to" [to] (str-to (Integer. to)))
  (GET "/count-down/:from" [from] (str-from (Integer. from)))
  (POST "/get-add-user" req ((composer get-add-user) req))
  (POST "/add-share" req ((composer add-share) req))
  (POST "/get-share" req ((composer get-share) req))
  (POST "/get-test" req ((composer get-test) req)))
  

(defn -main []
  (run-jetty #'routes {:port 8080 :join? false}))

