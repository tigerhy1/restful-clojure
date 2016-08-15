(ns restful_clojure.login
    (:require [clojure.data.json :as json]
              [clj-http.client :as client]
              [ring.util.response :refer [redirect]]
              [restful_clojure.couchbase_con :refer [bucket]]
              [restful_clojure.couchbase :as c]))

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
          openId (get json-body1 "openid")
          nickname (get json-body1 "nickname")
          unionId (get json-body1 "unionid")]
        (prn "userinfo = " json-body1)
        (get-add-user-db openId unionId nickname))
    )

(defn deal-code-session [request code]
    (let [session (:session request)
          userId (deal-code code)
          session (assoc session :userid userId)]
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