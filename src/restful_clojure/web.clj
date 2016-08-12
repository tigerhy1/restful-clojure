(ns restful_clojure.web
  (:gen-class)
  (:require [compojure.core :refer [defroutes GET POST OPTIONS]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body, wrap-json-response]]
            ;[ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [response, redirect]]
            [clojure.string :refer [join]]
            [clojure.data.json :as json]
            [pandect.algo.sha1 :refer :all]
            [clj-http.client :as client]
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
        (assoc-in [:headers "Access-Control-Allow-Origin"] "http://114.215.112.211:3000")
        (assoc-in [:headers "Access-Control-Allow-Methods"] "GET,PUT,POST,DELETE,OPTIONS")
        (assoc-in [:headers "Access-Control-Allow-Credentials"] "true")))



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
        :headers {"Content-Type" "text/html" "Access-Control-Allow-Origin" "http://114.215.112.211:3000"
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
                     (assoc-in [:headers "Access-Control-Allow-Origin"] "http://114.215.112.211:3000")
                     (assoc-in [:headers "Access-Control-Allow-Methods"] "GET,PUT,POST,DELETE,OPTIONS")
                     (assoc-in [:headers "Access-Control-Allow-Credentials"] "true"))
          
          session (:session req)]
        (prn req)                
        (prn session)
        (prn "session username  = "  (:username session))
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
   :headers {"Content-Type" "application/json" "Access-Control-Allow-Origin" "http://114.215.112.211:3000"
             "Access-Control-Allow-Methods" "GET,PUT,POST,DELETE,OPTIONS"
             "Access-Control-Allow-Headers" "Origin, X-Requested-With, Content-Type, Accept"
             "Access-Control-Allow-Credentials" "true"}
   :body "hi"})

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

(defn deal-code [code]
    (let [appid "wx05c3938f1f56e04b"
          secret "5f31e0bf0384fc0471d01b594c33165a"
          grant_type "authorization_code"
          res (client/get "https://api.weixin.qq.com/sns/oauth2/access_token?"
                    {:query-params {:appid appid :secret secret :code code :grant_type grant_type}})
          body (:body res)
          json-body (json/read-str body)
          access_token (get json-body "access_token")
          openid (get json-body "openid")
          res1 (client/get "https://api.weixin.qq.com/sns/userinfo?"
                    {:query-params {:access_token access_token :openid openid :lang "zh_CN"}})
          body1 (:body res1)
          json-body1 (json/read-str body1)
          nickname (get json-body1 "nickname")]
        (prn nickname)
        nickname)
    )

;(defn receive-code-test [request]
;    (response "yes"))

(defn deal-code-session [request code]
    (let [session (:session request)
          nickname (deal-code code)
          session (assoc session :useranme nickname)]
        (prn "in deal-code-session, session" session)
        session))

(defn receive-code [request]
    (let [params (:params request)
         code (:code params)]
        (prn "print sth")
        (prn params)
        (prn "receive-code, req = " + request)
        (cond (= nil code) nil
            :else (->(redirect "http://114.215.112.211:3000")
                     (assoc :session (deal-code-session request code))
                     (assoc-in [:headers "Access-Control-Allow-Credentials"] "true"))))
    )

(def receive-code-handler
    (-> receive-code
        (wrap-defaults api-defaults)))

;prove that session can work
(defn set-session [req]
    (let [session1 (:session req)
          count (:count session1 0)
          session (assoc session1 :count (inc count))]
          (prn req)
          (prn session)
          (-> (response (str "You accessed this page " count " times."))
              (assoc :session session)
              (assoc :headers {"Content-Type" "text/html"})))
    )

(def set-session-handler
    (-> set-session
        wrap-session))

(def get-share-handler
    (-> get-share
        wrap-session))

(defn one-session-store-fn [req path]
    (cond (= path "receive-code") receive-code-handler
          :else (composer get-share)))

(def one-session-store-handler
    (-> one-session-store-fn
        wrap-session))

(defroutes routes
  (POST "/" {body :body} (slurp body))
  (GET "/count-up/:to" [to] (str-to (Integer. to)))
  (GET "/count-down/:from" [from] (str-from (Integer. from)))
  (POST "/get-add-user" req ((composer get-add-user) req))
  (POST "/add-share" req ((composer add-share) req))
  (OPTIONS "/add-share" req (options-handler req))
  (POST "/get-share" req ((composer get-share-handler) req))
  (POST "/get-test" req (one-session-store-handler req "get-share"))
  (OPTIONS "/get-test" req (options-handler req))
  (GET "/check-echo" req (check-echo-handler req))
  (GET "/foo/:foo" [foo id]                    ; You can always destructure and use query parameter in the same way
    (str "Foo = " foo " / Id = " id))
  (GET "/receive-code" req (one-session-store-handler req "receive-code"))
  (GET "/set-session" req (set-session-handler req)))
  

(defn -main []
  (run-jetty #'routes {:port 8080 :join? false}))

