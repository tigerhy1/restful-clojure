(ns restful_clojure.couchbase_con
    (:require [restful_clojure.couchbase :as c])
    )

(def cluster (c/create "localhost"))

(def bucket (c/open-bucket cluster "default"))

