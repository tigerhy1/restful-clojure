(ns restful_clojure.movie
    (:require [restful_clojure.couchbase_con :refer [bucket]]
              [restful_clojure.couchbase :as c]))

;logic like add user, consider to genelize it 
(defn get-movie
    [name]
    (let [k (str "i_m_" name)
          doc (c/get-doc bucket k)
          content (:content doc)]
        (prn (str "doc = " doc))
        (prn content)
        (prn (get content "mid"))
        (if (nil? doc) 0 (get content "mid"))))

(defn create-movie
    [name]
    (let [k "max_mid"
          mid (c/counter! bucket k)]
        (prn k)
        (prn mid)
          {:mid mid
           :name name}
        ))

(defn add-movie
    [name]
    (let [movie (create-movie name)
          mid (:mid movie)
          k (str "m_" mid)
          k2 (str "i_m_" name)]
        (c/replace! bucket k2 {:mid mid})
        (c/replace! bucket k movie)
        (prn (str "in add-movie mid = " mid))
        mid))

(defn get-add-movie
    [name]
    (let [mid (get-movie name)]
        (prn (str "in get-add-movie mid = " mid))
        (cond
            (= 0 mid) (add-movie name)
            :else mid)))

;From mid to movie_name
(defn get-movie-name [mid]
    (let [k (str "m_" mid)
          doc (c/get-doc bucket k)
          movie (:content doc)]
        ;(prn "doc " doc)
        ;(prn "movie " movie)
        (get movie "name")))
    





