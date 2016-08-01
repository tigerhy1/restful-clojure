(defproject restful-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.2.1"]
                 [pandect "0.6.0"]
                 [ring-cors "0.1.8"]
                 [compojure "1.5.1"]
                 [com.couchbase.client/java-client "2.2.4"]]
  :main restful_clojure.web
  :aot [restful_clojure.web]
  )
