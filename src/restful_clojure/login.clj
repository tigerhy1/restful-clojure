(ns restful_clojure.login
    (:require [clojure.data.json :as json]
              [clj-http.client :as client]
              [ring.util.response :refer [redirect]]))

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
        (prn "userinfo = " json-body1)
        nickname)
    )

(defn deal-code-session [request code]
    (let [session (:session request)
          nickname (deal-code code)
          session (assoc session :username nickname)]
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