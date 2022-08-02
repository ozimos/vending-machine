(ns ozimos.vend.web.middleware.auth
  (:require [buddy.auth.backends :as backends]
            [buddy.auth :as buddy]
            [buddy.auth.middleware :as b.auth.mw]
            [ring.util.http-response :as http-response]
            [clojure.spec.alpha :as s]))

(defn unauthorized-handler
  [_req metadata]
  (http-response/forbidden metadata))

(defn on-error
  [_req e]
  (buddy/throw-unauthorized {:message (.getMessage ^Exception e)}))

(defn wrap-jwt-auth
  [auth-mw {:keys [jwt-secret]} _opts]
  (let [backend (backends/jws {:secret jwt-secret :token-name "Bearer"
                               :on-error on-error
                                :unauthorized-handler unauthorized-handler})]
    (fn [handler]
      (auth-mw handler backend))))

(s/def ::jwt-secret string?)

(def wrap-jwt-authentication-middleware
  {:name ::wrap-jwt-authentication
   :spec (s/keys :req-un [::jwt-secret])
   :compile (partial wrap-jwt-auth b.auth.mw/wrap-authentication)})

(def wrap-jwt-authorization-middleware
  {:name ::wrap-jwt-authentication
   :spec (s/keys :req-un [::jwt-secret])
   :compile (partial wrap-jwt-auth b.auth.mw/wrap-authorization)})

(defn self-or-admin-middleware
  [handler]
  (fn
    [{{:keys [role id]} :identity {path :path} :parameters :as req}]
    (if (or (= "admin" role) (= id (:id path)))
      (handler req)
      (http-response/forbidden {:message "Only admins or self allowed"}))))

(defn admin-only-middleware
  [handler]
  (fn
    [{{:keys [role]} :identity :as req}]
    (if (= "admin" role)
      (handler req)
      (http-response/forbidden {:message "Only admins allowed"}))))

(def seller-only-middleware
  {:name ::sellers-only
   :compile (fn [{::keys [all-authorized]} _opts]
              (when-not all-authorized
                (fn [handler]
                  (fn
                    [req]
                    (if (= "seller" (get-in req [:identity :role]))
                      (handler req)
                      (http-response/forbidden {:message "Only sellers allowed"}))))))})

(def buyer-only-middleware
  {:name ::sellers-only
   :compile (fn [{::keys [all-authorized]} _opts]
              (when-not all-authorized
                (fn [handler]
                  (fn
                    [req]
                    (if (= "buyer" (get-in req [:identity :role]))
                      (handler req)
                      (http-response/forbidden {:message "Only buyers allowed"}))))))})

(def check-auth-middleware
  {:name ::check-auth
  :compile (fn [{::keys [auth-exempt]} _opts]
             (when-not auth-exempt
               (fn
                 [handler]
                 (fn [req]
                   (if (buddy/authenticated? req)
                     (handler req)
                     (buddy/throw-unauthorized {:message "Only authenticated users allowed"}))))))})
