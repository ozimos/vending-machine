(ns ozimos.vend.web.controllers.product
  (:require
   [ring.util.http-response :as http-response]
   [clojure.string :as str]
   [ozimos.vend.web.routes.utils :as r.utils]))

(defn create-product
  [{{body :body} :parameters ident :identity :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)]
    (try
      (let [{id :id} (query-fn :create-product! (assoc body :seller_id (:id ident)))]
        (http-response/created (str "/product/" id) {:id id :message "Product created successfully"}))
      (catch clojure.lang.ExceptionInfo e
        (if (some-> (.getMessage ^Exception e)
                    (str/includes? "UNIQUE constraint failed: products.product_name, products.seller_id"))
          (http-response/conflict {:humanized {:product-name ["You already have a product by the same name"]}
                                   :message "Product creation failed"})
          (throw e))))))

(defn get-all-products
  [req]
  (let [query-fn (r.utils/route-data-key req :query-fn)
        res (query-fn :get-all-products! {})]
    (http-response/ok {:products res})))

(defn get-product
  [{{path :path} :parameters :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)
        res (query-fn :get-product-by-id! path)]
    (if res
      (http-response/ok res)
      (http-response/not-found {:message "No product found for the supplied id"}))))

(defn delete-product
  [{{path :path} :parameters ident :identity :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)]
    (query-fn :delete-product-by-id! (assoc path :seller_id (:id ident)))
    (http-response/no-content)))

(defn update-product
  [{{:keys [body path]} :parameters ident :identity :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)]
    (try
      (if-let [updated-product (query-fn :update-product-by-id!
                                         (assoc path
                                                :seller_id (:id ident)
                                                :updates body))]
        (http-response/ok updated-product)
        (http-response/no-content))
      (catch clojure.lang.ExceptionInfo e
        (if (some-> (.getMessage ^Exception e)
                    (str/includes? "UNIQUE constraint failed: products.product_name, products.seller_id"))
          (http-response/bad-request {:errors {:product-name ["You already have a product by the same name"]}})
          (throw e))))))


