(ns whiner.handler
  (:gen-class)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [ring.adapter.jetty :as jetty]
            [cheshire.core :as json]
            [kixi.log :as kixi-log]
            [kixi.metrics.reporters.json-console :as reporter]
            [metrics
             [core :refer [new-registry default-registry]]
             [histograms :refer [histogram update!]]
             [meters :refer [mark! meter]]]
            [metrics.jvm.core :as jvm])
  (:import [com.codahale.metrics ScheduledReporter MetricFilter]
           [java.util.concurrent TimeUnit]))

(def registry (atom nil))

(def log-config
  "own log config"
  {:level :info
   :ns-blacklist ["org.eclipse.jetty"]
   :middleware []

   ;; Clj only:
   :timestamp-opts kixi-log/default-timestamp-opts ; iso8601 timestamps

   :options {:stacktrace-fonts {}}
   :appenders {:direct-json (kixi-log/timbre-appender-logstash)}})

(defroutes app-routes
  (GET "/" []
       "OK")
  (GET "/info" []
       (log/info "informative message")
       "Info")
  (GET "/info-map" []
       (log/info {:key "informative message"})
       "Info Map")
  (GET "/event" []
       (log/info {:logtype "event"
                  :key "This is a map"})
       "Info Event")
  (GET "/warn" []
       (log/warn "warning")
       "Warning")
  (GET "/error" []
       (log/error "error")
       "Error")
  (GET "/exception" []
       (do
         (throw (Exception. "this is an exception"))
         "never reached"))
  (GET "/runtime" []
       (do
         (quot 1 0)
         "never reached"))
  (GET "/async" []
       (do
         (async/go (throw (Exception. "exception in a go block")))
         "asynced"))
  (GET "/async-run" []
       (do
         (async/go (quot 1 0))
         "asynced runtime error"))
  (route/not-found "Not Found"))

(defn wrap-catch-exceptions [handler]
  (fn [request]
    (try (handler request)
         (catch Throwable t (log/error t)))))

(def app
  (-> (wrap-defaults app-routes site-defaults)
      (wrap-catch-exceptions)))

(defn configure-metrics
  []
  (let [reg default-registry
        reporter (reporter/reporter reg {})]    
    (reporter/start reporter {:seconds 5})
    (jvm/instrument-jvm reg)))

(defn -main
  [& args]
  ;; set up log format
  (log/set-config! log-config)

  (configure-metrics)

  ;; https://stuartsierra.com/2015/05/27/clojure-uncaught-exceptions
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (log/error ex))))

  (jetty/run-jetty app {:port 3000}))
