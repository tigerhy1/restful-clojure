(ns restful_clojure.couchbase
    (:import [com.couchbase.client.java CouchbaseCluster]
             [com.couchbase.client.java Bucket]
             [com.couchbase.client.java.document JsonDocument]
             [com.couchbase.client.java.document.json JsonObject])
    (:require [clojure.data.json :as json]))

(defn create
    [string]
    (let [urls (if (string? string) (vector string) string)]
        (CouchbaseCluster/create urls)))

(defn open-bucket
    ([cluster bucket-name]
     (.openBucket cluster bucket-name)))

(defn get-cnt
    ([jsondocument]
     (if jsondocument
        (-> jsondocument
            .content))))

(defn counter!
    ([bucket k]
     (counter! bucket k 0 1))
    ([bucket k delta initial]
     (get-cnt (.counter bucket k delta initial))))

(defn read-json
    [data]
    (when-not (nil? data) (json/read-str data)))

(def write-json json/json-str)

(defn content->map
    [json-document]
    (if json-document
        (-> json-document
            .content
            .toString
            read-json)))

(defn document->map 
    [jsondocument]
    {:id (.id jsondocument)
     :cas (.cas jsondocument)
     :expiry (.expiry jsondocument)
     :content (content->map jsondocument)})

(defn create-json-document
    [id json-map]
    (let [json-object (JsonObject/fromJson (write-json json-map))]
        (JsonDocument/create id json-object)))

(defn replace!
    [bucket id json-map]
    (let [json (if (string? json-map) (read-json json-map) json-map)
          doc (create-json-document id json)]
        (document->map (.upsert bucket doc))))

(defn get-doc
    ([bucket id]
     (get-doc bucket id :json))
    ([bucket id format]
     (let [doc (.get bucket id)]
         (if doc
             (if (= :raw format)
                 (-> doc
                     .content
                     .toString)
                 (document->map doc))))))






