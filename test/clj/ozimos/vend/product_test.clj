(ns ozimos.vend.product-test
  "Tests for the product routes"
  (:require
   [ozimos.vend.test-utils :as utils]
   [ring.mock.request :as mock]
   [jsonista.core :as json]
   [next.jdbc :as jdbc]
   [clojure.test :refer :all]))


(use-fixtures :once
  (utils/system-fixture)
  utils/reset-db utils/populate-db-with-users
  utils/populate-db-with-products)

(deftest create-product-test
  (testing "Successfully creates a new product"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/product")
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:role "seller"})))
                                             (mock/json-body {:product_name "roomba"
                                                              :amount_available 5
                                                              :cost 5})))
          {:keys [message id] :as decoded} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 201 status))
      (is (= #{:message :id} (-> decoded keys set)) "Response body has required keys")
      (is (int? id) "Returns the id of the created product")
      (is (string? message) "Returns product creation message")))

  (testing "Does not add an existing product for the seller"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/product")
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 3 :role "seller"})))
                                             (mock/json-body utils/default-product)))
          {:keys [message humanized] :as decoded} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 409 status))
      (is (= #{:message :humanized} (-> decoded keys set)) "Response body has required keys")
      (is (string? (get-in humanized [:productName 0])) "Returns error message about product_name")
      (is (string? message) "Returns general error message")))

  (testing "Does not add products with non conforming costs"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/product")
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 3 :role "seller"})))
                                             (mock/json-body {:product_name "razor" :cost 7 :amount_available 3})))
          {:keys [humanized]} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 400 status))
      (is (string? (get-in humanized [:cost 0])) "Returns error message about cost"))))

(deftest get-product-test
  (testing "get product succeeds for self when authorized"
    (let [handler (utils/get-handler)
          {:keys [status body]} (handler (-> (mock/request :get "/product/1")
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 1 :role "seller"})))))]
      (is (= utils/default-product (json/read-value body json/keyword-keys-object-mapper)) "returns product details")
      (is (= 200 status) "returns 20X status")))
  (testing "get product succeeds for admin"
    (let [handler (utils/get-handler)
          {:keys [status body]} (handler (-> (mock/request :get "/product/1")
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:role "admin"})))))]
      (is (= utils/default-product (json/read-value body json/keyword-keys-object-mapper)) "returns product details")
      (is (= 200 status) "returns 20X status")))
  (testing "get product requires authentication"
    (let [handler (utils/get-handler)
          {:keys [status]} (handler (mock/request :get "/product/1"))]
      (is (= 403 status) "get product is not allowed for unauthenticated products"))))

(deftest update-product-test
  (testing "update product succeeds for self when authorized"
    (let [handler (utils/get-handler)
          {:keys [status body]} (handler (-> (mock/request :put "/product/3")
                                             (mock/json-body {:productName "carpet"})
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 3 :role "seller"})))))
          expected-product (assoc utils/update-product :productName "carpet")]
      (is (= expected-product (json/read-value body json/keyword-keys-object-mapper)) "returns product details")
      (is (= 200 status) "returns 20X status")))
  (testing "update product fails for non owner"
    (let [handler (utils/get-handler)
          {:keys [status]} (handler (-> (mock/request :put "/product/4")
                                             (mock/json-body {:productName "atlas"})
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:role "admin"})))))]
      (is (= 204 status) "returns 20X status")))
  (testing "update product requires authentication"
    (let [handler (utils/get-handler)
          {:keys [status]} (handler (-> (mock/request :put "/product/6")
                                        (mock/json-body {:product_name "Adam"})))]
      (is (= 403 status) "update product is not allowed for unauthenticated products"))))

(deftest delete-product-test
  (testing "delete product succeeds when authorized"
    (let [db-conn (:db.sql/connection (utils/system-state))
          record-before (jdbc/execute-one! db-conn ["select product_name from products where id = ?" 2])
          handler (utils/get-handler)
          {:keys [status]} (handler (-> (mock/request :delete "/product/2")
                                        (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 3 :role "seller"})))))
          record-after (jdbc/execute-one! db-conn ["select product_name from products where id = ?" 2])]
      (is (= 204 status) "returns 20X status")
      (is (not= record-before record-after) "product was deleted")))
  (testing "delete product requires authentication"
    (let [handler (utils/get-handler)
          {:keys [status]} (handler (mock/request :delete "/product/4"))]
      (is (= 403 status) "delete product is not allowed for unauthenticated products"))))