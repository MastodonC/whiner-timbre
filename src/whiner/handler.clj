(ns whiner.handler
  (:gen-class)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [ring.adapter.jetty :as jetty]
            [clojure.data.json :as json]))

(def logback-timestamp-opts
  "Controls (:timestamp_ data)"
  {:pattern  "yyyy-MM-dd HH:mm:ss,SSS"
   :locale   :jvm-default
   :timezone :utc})


(defn output-fn
  "Default (fn [data]) -> string output fn.
  Use`(partial default-output-fn <opts-map>)` to modify default opts."
  ([     data] (output-fn nil data))
  ([opts data] ; For partials
   (let [{:keys [no-stacktrace? stack-fonts]} opts
         {:keys [level ?err #_vargs msg_ ?ns-str hostname_
                 timestamp_ ?line]} data]   
     (str
      (force timestamp_)  " "
      (str/upper-case (name level))  " "
      "[" (or ?ns-str "?") ":" (or ?line "?") "] - "
      (force msg_)
      (when-not no-stacktrace?
        (when-let [err ?err]
          (str "\n" (log/stacktrace err opts))))))))

(defn json-output
  [opts data]
  (json/write-str
  {:level (:level data)
   :namespace (:?ns-str data)
   :application "whiner-timbre"
   :file (:?file data)
   :line (:?line data)
   :stacktrace (some-> (:?err data) (stacktrace-str))
   :hostname (force (:hostname_ data))
   :message (force (:msg_ data))
   "@timestamp" (:instant data)}))

(def log-config
  "own log config"
  {:level :info  ; e/o #{:trace :debug :info :warn :error :fatal :report}

   ;; Control log filtering by namespaces/patterns. Useful for turning off
   ;; logging in noisy libraries, etc.:
   ;;:ns-whitelist  ["whiner.*"] #_["my-app.foo-ns"]
   :ns-blacklist ["org.eclipse.jetty"]


   ;; Clj only:
   :timestamp-opts logback-timestamp-opts ; iso8601 timestamps

   :output-fn (partial json-output {:stacktrace-fonts {}})
   })

(defroutes app-routes
  (GET "/" []
       "OK")
  (GET "/info" []
       (log/info "informative message")
       "Info")
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

(defn -main
  [& args]
  ;; set up log format
  (log/merge-config! log-config)

  ;; https://stuartsierra.com/2015/05/27/clojure-uncaught-exceptions
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (log/error ex))))

  (jetty/run-jetty app {:port 3000}))
