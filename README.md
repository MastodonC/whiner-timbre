# whiner-timbre

Model logging app for logging with timbre with our logstash configuration.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server-headless


## Important points


The log configuration should happen at the very start of the main- function, with

```
(:require [taoensso.timbre :as log])
...
  (log/merge-config! log-config)
```

log-config is a clojure map.  The defaults are fairly sensible but to get our format we use a custom one, described [here](https://github.com/MastodonC/whiner-timbre/blob/master/src/whiner/handler.clj#L36)

The main changes here are to get a format that we can easily multiline on, as well as parse out the log level of the logged line.
The format is the same as the one used for the (https://github.com/MastodonC/whiner-slf4j) application, which is:

```
2016-07-18 15:13:52,635 INFO ...
```

Unlike slf4j we cannot really decide to limit the log level of a library, it's blacklisting or white-listing or nothing.

```
   :ns-blacklist ["org.eclipse.jetty"]
```


Another, separate point that we also use is Stuart Sierra's trick to get all errors from all threads passed on:

```
  ;; https://stuartsierra.com/2015/05/27/clojure-uncaught-exceptions
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (log/error ex))))
```

We're also surrounding the main method with a try-catch block to make sure all runtime errors are caught

```
    (try <all the action happens here>
         (catch Throwable t (log/error t)))))
```


## License

Copyright © 2016 Mastodon C
