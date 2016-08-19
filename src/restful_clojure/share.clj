(ns restful_clojure.share
    (:require [restful_clojure.couchbase_con :refer [bucket]]
              [restful_clojure.couchbase :as c]
              [restful_clojure.movie :as m]))

(defn create-share
    [uid mid desc]
    {:uid uid
     :mid mid
     :desc desc})

;TODO aynchroized API
(defn get-share
    ([shareId]
    (let [k (str "share_" shareId)
          doc (c/get-doc bucket k)
          content (:content doc)
          ret (assoc content "sid" shareId)]
        (prn "k " k " content " ret)
        ret))
    ([offset size]
    (let [lastIdx (c/counter bucket "max_sid")
          sIdx (if (= offset 0) lastIdx offset)
          eIdx (max 0 (- sIdx size))
          r (range sIdx eIdx -1)]
        (prn (str "sIdx " sIdx " eIdx " eIdx))
        (prn (str "offset " offset " size " size))
        (prn r)
        (map get-share r))))

(defn add-share
    [uid, mid, desc]
    (let [shareId (c/counter! bucket "max_sid")
          k (str "share_" shareId)]
        (c/replace! bucket k (create-share uid mid desc))
        (get-share shareId)))

(defn get-test
    [x]
    x)

