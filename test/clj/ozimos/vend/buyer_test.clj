(ns ozimos.vend.buyer-test
  "Tests for the deposit, buy and reset routes"
  (:require
   [ozimos.vend.test-utils :as utils]
   [ring.mock.request :as mock]
   [clojure.test :refer :all]))

(use-fixtures :once
  (utils/system-fixture)
  utils/reset-db utils/populate-db-with-users
  utils/populate-db-with-products)

(defn deposit-request [method req-body]
  (-> (mock/request method "/deposit")
      (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 1 :role "buyer"})))
      (mock/json-body req-body)))

(deftest deposit-test
  (testing "Successfully deposits funds"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (deposit-request :post {:deposit 5}))
          first-payment (utils/json-decode body)

          _ (handler (deposit-request :post {:deposit 10}))
          resp (handler (deposit-request :post {:deposit 20}))
          final-payment (utils/json-decode (:body resp))]
      (is (= 200 status))
      (is (= {:deposit 5 :message "Deposit successful"} first-payment) "Deposit is correct")
      (is (= {:deposit 35 :message "Deposit successful"} final-payment) "Deposit is correct")))
  (testing "Does not deposit wrong amounts"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (deposit-request :put {:deposit 7}))]
      (is (= 400 status))
      (is (string? (get-in (utils/json-decode body) [:humanized :deposit 0])) "Returns error message about deposit"))))

(deftest reset-deposit-test
  (testing "Successfully resets deposits"
    (let [handler (utils/get-handler)
          {:keys [body status]}
          (handler (-> (mock/request :get "/reset")
                       (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 2 :role "buyer"})))))]
      (is (= 200 status))
      (is (= {:deposit 0 :message "Deposit reset successfully"} (utils/json-decode body)) "Deposit was reset"))))

(deftest buy-test
  (testing "Successfully buys product"
    (let [handler (utils/get-handler)
          {:keys [body status]}
          (handler (-> (mock/request :get "/buy")
                       (mock/json-body {:productId 3 :amount 3})
                       (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 2 :role "buyer"})))))]
      (is (= 200 status))
      (is (= {:totalSpend 45, :productName "sneaker", :deposit 70, :buyAmount 3, :change [50 10 5 5], :message "Buy successful"}
             (utils/json-decode body)) "Purchase successful"))))
