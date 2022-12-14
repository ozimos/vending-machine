(ns ozimos.vend.core
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [ozimos.vend.config :as config]
   [ozimos.vend.env :refer [defaults]]

    ;; Edges
   [kit.edge.server.undertow]
   [ozimos.vend.web.handler]

    ;; Routes
   [ozimos.vend.web.routes.api]

   [kit.edge.db.sql.conman]
   [kit.edge.db.sql.migratus])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(defonce system (atom nil))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!))
  (shutdown-agents))

(defn start-app [& [params start-keys]]
  ((or (:start params) (:start defaults) (fn [])))
  (let [config (->> (config/system-config (or (:opts params) (:opts defaults) {}))
                    (ig/prep))
        sys (if start-keys (ig/init config start-keys) (ig/init config))]
    (reset! system sys))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& _]
  (start-app))
