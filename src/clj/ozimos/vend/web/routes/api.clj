(ns ozimos.vend.web.routes.api
  (:require
   [ozimos.vend.web.controllers.health :as health]
   [ozimos.vend.web.controllers.auth :as auth]
   [ozimos.vend.web.controllers.user :as user]
   [ozimos.vend.web.controllers.product :as product]
   [ozimos.vend.web.middleware.exception :as exception]
   [ozimos.vend.web.middleware.formats :as formats]
   [ozimos.vend.web.middleware.auth :as auth-mw]
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.spec :as rrs]
   [reitit.spec :as rs]
   [expound.alpha :as e]
   [malli.util :as mu]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]))

;; Route parameter validation
(def Role [:role {:optional true} [:enum {:default "buyer"} "buyer" "seller"]])

(def BaseUser [:map
               [:username [:string {:min 2}]]])

(def LoginUser (conj BaseUser [:password [:string {:min 4}]]))

(def CreateUser (conj LoginUser Role))

(def UpdateUser (mu/optional-keys BaseUser))

(def Product [:map
              [:amount-available int?]
              [:cost [:and :int [:fn (fn [v] (zero? (mod v 5)))]]]
              [:product-name [:string {:min 2}]]])

(def UpdateProduct (mu/optional-keys Product))

(def auth-mw [auth-mw/wrap-jwt-authorization-middleware
              auth-mw/wrap-jwt-authentication-middleware
              auth-mw/check-auth-middleware])

;; Routes
(defn api-routes [_opts]
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title "Vend API"
                            :description "An API for buying and selling"
                            :version "1.0.0"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/api-docs/*"
    {:get (swagger-ui/create-swagger-ui-handler)}]
   ["/health"
    {:get health/healthcheck!}]
   ["/user" {:middleware auth-mw}
    ["" {:post {:parameters {:body CreateUser}
                ::auth-mw/auth-exempt true
                :handler auth/register}
         :get {:handler user/get-all-users
               :middleware [auth-mw/admin-only-middleware]}}]
    ["/:id" {:get {:parameters {:path {:id int?}}
                   :handler user/get-user}
             :put {:parameters {:body UpdateUser
                                :path {:id int?}}
                   :handler user/update-user}
             :delete {:parameters {:path {:id int?}}
                      :handler user/delete-user}
             :middleware [auth-mw/self-or-admin-middleware]}]]
   ["/product"
    {:middleware (conj auth-mw auth-mw/seller-only-middleware)}
    ["" {:post {:parameters {:body Product}
                :handler auth/register}
         :get {:handler product/get-all-products
               ::auth-mw/all-authorized true}}]
    ["/:id" {:get {:parameters {:path {:id int?}}
                   ::auth-mw/all-authorized true
                   :handler product/get-product}
             :put {:parameters {:body UpdateProduct
                                :path {:id int?}}
                   :handler product/update-product}
             :delete {:parameters {:path {:id int?}}
                      :handler product/delete-product}}]]
   ["" {:middleware (conj auth-mw auth-mw/buyer-only-middleware)}
    ["/deposit"
     {:get health/healthcheck!}]
    ["/buy"
     {:get health/healthcheck!}]
    ["/reset"
     {:get health/healthcheck!}]]
   ["/login"
    {:post {:parameters {:body LoginUser}
            :handler auth/login}}]])

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



