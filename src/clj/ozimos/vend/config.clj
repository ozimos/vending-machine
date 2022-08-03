(ns ozimos.vend.config
  (:require
   [kit.config :as config]
   [integrant.core :as ig]))

(def ^:const system-filename "system.edn")

(defn system-config
  [options]
  (config/read-config system-filename options))

(defmethod ig/init-key :auth/jwt-secret [_ k] k)