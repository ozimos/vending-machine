(ns ozimos.vend.web.routes.api
  (:require
   [ozimos.vend.web.controllers.health :as health]
   [ozimos.vend.web.controllers.auth :as auth]
   [ozimos.vend.web.middleware.exception :as exception]
   [ozimos.vend.web.middleware.formats :as formats]
   [ozimos.vend.web.auth.utils :as auth-utils]
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.spec :as rrs]
   [reitit.spec :as rs]
   [expound.alpha :as e]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]))

(def User [:map
           [:username [:string {:min 4}]]
           [:password [:string {:min 4}]]])

;; Routes
(defn api-routes [opts]
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title "vend API"
                            :description "An API for creating and enjoying vends"
                            :version "1.0.0"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/api-docs/*"
    {:get (swagger-ui/create-swagger-ui-handler)}]
   ["/health"
    {:get health/healthcheck!}]
   ["/user"
    {:post {:parameters {:body User}
            :handler (auth/make-register-handler opts)}}]
   ["/deposit"
    {:middleware [auth-utils/wrap-jwt-authentication-middleware auth-utils/check-auth-middleware]}
    ["" {:get health/healthcheck!
         :post health/healthcheck!}]]
   ["/login"
    {:post {:parameters {:body User}
            :handler (auth/make-login-handler opts)}}]])

(defn route-data
  [opts]
  (merge
   opts
   {:coercion   malli/coercion
    :muuntaja   formats/instance
    :swagger    {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                  ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                  ;; encoding response body
                 muuntaja/format-response-middleware
                  ;; exception handling
                 coercion/coerce-exceptions-middleware
                  ;; decoding request body
                 muuntaja/format-request-middleware
                  ;; coercing response bodys
                 coercion/coerce-response-middleware
                  ;; coercing request parameters
                 coercion/coerce-request-middleware
                  ;; exception handling
                 exception/wrap-exception]}))

(defmethod ig/init-key :router/core
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (ring/router [base-path (route-data opts) (api-routes opts)]
               {:validate rrs/validate
                ::rs/explain e/expound-str}))



