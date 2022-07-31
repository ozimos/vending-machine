(ns ozimos.vend.web.middleware.auth
  (:require [buddy.auth.backends :as backends]
            [buddy.auth :as buddy]
            [buddy.auth.middleware :as b.auth.mw]
            [ring.util.http-response :as http-response]
            [clojure.spec.alpha :as s]))

(defn wrap-jwt-authentication
  [{:keys [jwt-secret]} _opts]
  (let [backend (backends/jws {:secret jwt-secret :token-name "Bearer"})]
    (fn [handler]
      (b.auth.mw/wrap-authentication handler backend))))

(s/def ::jwt-secret string?)

(def wrap-jwt-authentication-middleware
  {:name ::wrap-jwt-authentication
   :spec (s/keys :req-un [::jwt-secret])
   :compile wrap-jwt-authentication})

(defn check-auth-middleware
  [handler]
  (fn [req]
    (if (buddy/authenticated? req)
      (handler req)
      (http-response/forbidden {:error "Unauthorized"}))))
