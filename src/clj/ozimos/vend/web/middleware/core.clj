(ns ozimos.vend.web.middleware.core
  (:require
   [ozimos.vend.env :as env]))

(defn wrap-base
  [opts]
  (fn [handler]
    ((:middleware env/defaults) handler opts)))
