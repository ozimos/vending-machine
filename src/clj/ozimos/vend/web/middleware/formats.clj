(ns ozimos.vend.web.middleware.formats
  (:require
   [luminus-transit.time :as time]
   [camel-snake-kebab.core :as csk]
   [muuntaja.core :as m]
   [clojure.set :as set]))

(def instance
  (m/create
   (-> m/default-options
       (assoc-in
        [:formats "application/json" :decoder-opts]
        {:decode-key-fn (comp keyword csk/->snake_case)})
       (assoc-in
        [:formats "application/json" :encoder-opts]
        {:encode-key-fn (comp name csk/->camelCase)})
       (update-in
        [:formats "application/transit+json" :decoder-opts]
        (partial merge time/time-deserialization-handlers))
       (update-in
        [:formats "application/transit+json" :encoder-opts]
        (partial merge time/time-serialization-handlers)))))
(comment


(require '[clojure.set :as set])
  (set/superset? (set {:a 1 :b 2}) (set {:a 1})))