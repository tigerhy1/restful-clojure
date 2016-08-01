(ns restful_clojure.web
  (:require [compojure.core :refer [defroutes GET POST OPTIONS]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body, wrap-json-response]]
            ;[ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer [response]]
            [clojure.string :refer [join]]
            [pandect.algo.sha1 :refer :all]
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

(defn get-user-name [uid]
    (let [k (str "user_" uid)
          doc (c/get-doc bucket k)
          user (:content doc)]
        (get user "weixinName")))

(defn wrap-cors [res]
    (-> res
        (assoc-in [:headers "Access-Control-Allow-Origin"] "http://localhost:3000")
        (assoc-in [:headers "Access-Control-Allow-Methods"] "GET,PUT,POST,DELETE,OPTIONS")))



(defn fill-content 
    [item]
    (let [sid (get item "sid")
          uid (get item "uid")
          mid (get item "mid")
          desc (get item "desc")
          user_name (get-user-name uid)
          movie_name (m/get-movie-name mid)]
        {:id sid
         :user_name user_name
         :movie_name movie_name
         :share_comment desc}))

(defn get-share-test [req]
    (let [body (:body req)]
        (prn "body" body)
        {:status 200
        :headers {"Content-Type" "text/html" "Access-Control-Allow-Origin" "http://localhost:3000"
             "Access-Control-Allow-Methods" "GET,PUT,POST,DELETE,OPTIONS"}
        :body "Hello"}))

(defn add-share [req]
    (let [body (:body req)
          uid (:uid body)
          movieName (:movie_name body)
          desc (:share_comment body)
          mid (m/get-add-movie movieName)
          res (s/add-share uid mid desc)
          result (fill-content res)]
        (prn "uid " uid " movie_name " movieName "share_comment" desc)
        (prn "add-share result "  result)
        (-> result
            response
            wrap-cors)))


(defn get-share [req]
    (let [body (:body req)
          offset (:offset body)
          size (:size body)
          raw (s/get-share offset size)
          res (response (map fill-content raw))
          result (-> res
                     (assoc-in [:headers "Access-Control-Allow-Origin"] "http://localhost:3000")
                     (assoc-in [:headers "Access-Control-Allow-Methods"] "GET,PUT,POST,DELETE,OPTIONS"))]
                        
        
        (prn "res    = " res) 
        (prn "reslut = " result)
        result))

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
        (prn l)
        (response l))
    )

(defn options-handler [request]
  (prn "ooo")
  {:status 200
   :headers {"Content-Type" "application/json" "Access-Control-Allow-Origin" "http://localhost:3000"
             "Access-Control-Allow-Methods" "GET,PUT,POST,DELETE,OPTIONS"
             "Access-Control-Allow-Headers" "Origin, X-Requested-With, Content-Type, Accept"}
   :body "hi"})

(defn check-echo [request]
    (prn request)
    (prn (:params request))
    (let [params (:params request)
          signature (get params :signature)
          timestamp (:timestamp params)
          nonce (:nonce params)
          arr (sort [signature timestamp nonce])
          s (sha1 (join arr))]
        (cond (= s signature) (response signature)
            :else (response ""))))

(def check-echo-handler
    (wrap-defaults check-echo api-defaults))

(defroutes routes
  (POST "/" {body :body} (slurp body))
  (GET "/count-up/:to" [to] (str-to (Integer. to)))
  (GET "/count-down/:from" [from] (str-from (Integer. from)))
  (POST "/get-add-user" req ((composer get-add-user) req))
  (POST "/add-share" req ((composer add-share) req))
  (OPTIONS "/add-share" req (options-handler req))
  (POST "/get-share" req ((composer get-share) req))
  (POST "/get-test" req ((composer get-share) req))
  (OPTIONS "/get-test" req (options-handler req))
  (GET "/check-echo" req (check-echo-handler req))
  (GET "/foo/:foo" [foo id]                    ; You can always destructure and use query parameter in the same way
    (str "Foo = " foo " / Id = " id)))
  

(defn -main []
  (run-jetty #'routes {:port 8080 :join? false}))

