(def slf4j-version "1.7.21")
(defproject whiner-timbre "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [org.clojure/core.async "0.2.385"]
                 [com.taoensso/timbre "4.5.1"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [com.fzakaria/slf4j-timbre "0.3.2"]
                 [org.slf4j/jul-to-slf4j         ~slf4j-version]
                 [org.slf4j/jcl-over-slf4j       ~slf4j-version]
                 [org.slf4j/log4j-over-slf4j     ~slf4j-version]]
  :plugins [[lein-ring "0.9.7"]]

  :uberjar-name "whiner-timbre.jar"
  :main whiner.handler
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
