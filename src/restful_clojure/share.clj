(ns restful_clojure.share
    (:require [restful_clojure.couchbase_con :refer [bucket]]
              [restful_clojure.couchbase :as c]
              [restful_clojure.movie :as m]))

(defn create-share
    [uid mid desc]
    {:uid uid
     :mid mid
     :desc desc})

(defn add-share
    [uid, mid, desc]
    (let [shareId (c/counter! bucket "max_sid")
          k (str "share_" shareId)]
        (c/replace! bucket k (create-share uid mid desc))))