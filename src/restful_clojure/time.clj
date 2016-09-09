(ns restful_clojure.time
    (:require [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.format :as f]))

(def custom-formatter (f/formatter "yyyy-MM-dd HH:mm"))

;TODO, time zone wrong
(def now
    (str (f/unparse custom-formatter (t/to-time-zone (t/now) (t/time-zone-for-offset +2)))))