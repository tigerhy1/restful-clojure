(ns restful_clojure.web
  (:gen-class)
  (:require [compojure.core :refer [defroutes GET POST OPTIONS]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body, wrap-json-response]]
            ;[ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [response]]
            [clojure.string :refer [join]]
            [clojure.data.json :as json]
            [pandect.algo.sha1 :refer :all]
            [restful_clojure.couchbase_con :refer [bucket]]
            [restful_clojure.couchbase :as c]
            [restful_clojure.movie :as m]
            [restful_clojure.share :as s]
            [restful_clojure.login :refer [receive-code, get-add-user-db]]
            [restful_clojure.login :refer [receive-code, get-add-user-db]]
            [restful_clojure.conf :refer [front-server-address]]
            )
            
  (:import [com.couchbase.client.java Cluster CouchbaseCluster]))


(def test-cb 
    (c/get-doc bucket "hello"))
    ;(prn "test-cb"))


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
        (assoc-in [:headers "Access-Control-Allow-Origin" ] front-server-address)
        (assoc-in [:headers "Access-Control-Allow-Methods"] "GET,PUT,POST,DELETE,OPTIONS")
        (assoc-in [:headers "Access-Control-Allow-Credentials"] "true")))



(defn fill-content 
    [item]
    (let [sid (get item "sid")
          uid (get item "uid")
          mid (get item "mid")
          desc (get item "desc")
          ti (get item "time")
          tii (if (= nil ti) "2010-10-01 12:00:00" ti)
          user_name (get-user-name uid)
          movie_name (m/get-movie-name mid)]
        {:id sid
         :user_name user_name
         :movie_name movie_name
         :share_comment desc
         :created_at tii}))

(defn add-share [req]
    (let [body (:body req)
          session (:session req)
          userid (:userid session)
          uid (if (= nil userid) (:uid body) userid)
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
                     wrap-cors)
          
          session (:session req)]
        (prn req)                
        (prn session)
        (prn "session userid  = "  (:userid session))
        (prn "res    = " res) 
        (prn "reslut = " result)
        result))

(defn composer
    [handler]
    (-> handler
        (wrap-json-body {:keywords? true :bigdecimals? true})
        wrap-json-response))

;(defn get-test
;    [req]
;    (let [a {:uid 1 :name "hu"}
;          b {:uid 2 :name "yuan"}
;          l (list a b)]
;        (prn l)
;        (response l))
;    )

(defn options-handler [request]
  (prn "ooo")
  (-> {:status 200
       :headers {"Access-Control-Allow-Headers" "Origin, X-Requested-With, Content-Type, Accept"}
       :body "hi"}
      wrap-cors))

(defn check-echo [request]
    (prn request)
    (prn (:params request))
    (let [params (:params request)
          signature (get params :signature)
          timestamp (:timestamp params)
          nonce (:nonce params)
          echostr (:echostr params)
          token "123456789"
          arr (sort [token timestamp nonce])
          s (sha1 (join arr))]
        (cond (= s signature) (response echostr)
            :else (response ""))))

(def check-echo-handler
    (wrap-defaults check-echo api-defaults))

(def receive-code-handler
    (-> receive-code
        (wrap-defaults api-defaults)))

;prove that session can work
(defn set-session [req]
    (let [session1 (:session req)
          count (:count session1 0)
          uri (:uri req)
          inc-cnt (if (= uri "/set-session-2") 2 1)
          session (assoc session1 :count (+ inc-cnt count))]
          (prn "uri = "  uri)
          (prn req)
          (prn session)
          (-> (response (str "You accessed this page " count " times."))
              (assoc :session session)
              (assoc :headers {"Content-Type" "text/html"})))
    )

(def set-session-handler
    (-> set-session
        wrap-session))

(defn one-session-store-fn [req]
    (let [uri (:uri req)]
        (cond (= uri "/receive-code") (receive-code-handler req)
              (= uri "/get-test") ((composer get-share) req)
              (= uri "/add-share") ((composer add-share) req)
              :else ((composer get-share) req))))

(def wrap-session-fn
    (wrap-session one-session-store-fn))

(def one-session-store-composer 
    (fn [req]
        (wrap-session-fn req)))

(defroutes routes
  (POST "/" {body :body} (slurp body))
  (POST "/get-add-user" req ((composer get-add-user) req))
  (POST "/add-share" req (one-session-store-composer req ))
  (OPTIONS "/add-share" req (options-handler req))
  ;(POST "/get-share" req ((composer get-share-handler) req))
  (POST "/get-test" req (one-session-store-composer req ))
  (OPTIONS "/get-test" req (options-handler req))
  (GET "/check-echo" req (check-echo-handler req))
  (GET "/receive-code" req (one-session-store-composer req ))
  (GET "/set-session" req (set-session-handler req))
  (GET "/set-session-2" req (set-session-handler req)))
  

(defn -main []
  (run-jetty #'routes {:port 8080 :join? false}))

