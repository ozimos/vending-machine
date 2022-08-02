(ns ozimos.vend.core-test
  (:require
   [ozimos.vend.test-utils :as utils]
   [ring.mock.request :as mock]
   [jsonista.core :as json]
   [next.jdbc :as jdbc]
   [clojure.test :refer :all]))


(use-fixtures :once (utils/system-fixture) utils/reset-db utils/populate-db)

(deftest register-user-test
  (testing "Successfully registers a new user"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/user")
                                             (mock/json-body {:username "rajesh"
                                                              :password "tbbt"})))
          {:keys [message id token] :as decoded} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 201 status))
      (is (= #{:message :id :token} (-> decoded keys set)) "Response body has required keys")
      (is (int? id) "Returns the id of the created user")
      (is (string? message) "Returns user creation message")
      (is (string? token) "Returns jwt token")))

  (testing "Does not register an existing user"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/user")
                                             (mock/json-body utils/default-user)))
          {:keys [message humanized] :as decoded} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 409 status))
      (is (= #{:message :humanized} (-> decoded keys set)) "Response body has required keys")
      (is (string? (get-in humanized [:username 0])) "Returns error message about username")
      (is (string? message) "Returns general error message")))

  (testing "Does not register for non conforming usernames"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/user")
                                             (mock/json-body {:username "t" :password "short"})))
          {:keys [humanized]} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 400 status))
      (is (string? (get-in humanized [:username 0])) "Returns error message about username")))

  (testing "Does not register for non conforming passwords"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/user")
                                             (mock/json-body {:username "okay" :password "bad"})))
          {:keys [humanized]} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 400 status))
      (is (string? (get-in humanized [:password 0])) "Returns error message about username"))))

(deftest login-user-test
  (testing "Logs in user"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/login")
                                             (mock/json-body utils/default-user)))
          {:keys [message token] :as decoded} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 200 status))
      (is (= #{:message  :token} (-> decoded keys set)) "Response body has required keys")
      (is (string? message) "Returns login success message")
      (is (string? token) "Returns jwt token")))

  (testing "Does not login when wrong username"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/login")
                                             (mock/json-body (assoc utils/default-user :username "wrong_"))))
          {:keys [message humanized] :as decoded} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 400 status))
      (is (= #{:message :humanized} (-> decoded keys set)) "Response body has required keys")
      (is (string? (get-in humanized [:username 0])) "Returns error message about username")
      (is (string? (get-in humanized [:password 0])) "Returns error message about password")
      (is (string? message) "Returns general error message")))

  (testing "Does not login when wrong password"
    (let [handler (utils/get-handler)
          {:keys [body status]} (handler (-> (mock/request :post "/login")
                                             (mock/json-body (assoc utils/default-user :password "wrong_"))))
          {:keys [message humanized] :as decoded} (json/read-value body json/keyword-keys-object-mapper)]
      (is (= 400 status))
      (is (= #{:message :humanized} (-> decoded keys set)) "Response body has required keys")
      (is (string? (get-in humanized [:username 0])) "Returns error message about username")
      (is (string? (get-in humanized [:password 0])) "Returns error message about password")
      (is (string? message) "Returns general error message"))))

(deftest get-user-test
  (testing "get user succeeds for self when authorized"
    (let [handler (utils/get-handler)
          {:keys [status body]} (handler (-> (mock/request :get "/user/1")
                                        (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 1})))))
          expected-user (-> utils/default-user (assoc :deposit 0) (dissoc :password)) ]
      (is (= expected-user (json/read-value body json/keyword-keys-object-mapper)) "returns user details")
      (is (= 200 status) "returns 20X status")))
  (testing "get user succeeds for admin"
    (let [handler (utils/get-handler)
          {:keys [status body]} (handler (-> (mock/request :get "/user/1")
                                        (mock/header "Authorization" (str "Bearer " (utils/create-token {:role "admin"})))))
          expected-user (-> utils/default-user (assoc :deposit 0) (dissoc :password)) ]
      (is (= expected-user (json/read-value body json/keyword-keys-object-mapper)) "returns user details")
      (is (= 200 status) "returns 20X status")))
  (testing "get user requires authentication"
    (let [handler (utils/get-handler)
          {:keys [status]} (handler (mock/request :get "/user/1"))]
      (is (= 403 status) "get user is not allowed for unauthenticated users"))))

(deftest update-user-test
  (testing "update user succeeds for self when authorized"
    (let [handler (utils/get-handler)
          {:keys [status body]} (handler (-> (mock/request :put "/user/5")
                                             (mock/json-body {:username "Mishael"})
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 5})))))
          expected-user (-> utils/update-user-self (assoc :deposit 0 :username "Mishael") (dissoc :password))]
      (is (= expected-user (json/read-value body json/keyword-keys-object-mapper)) "returns user details")
      (is (= 200 status) "returns 20X status")))
  (testing "update user succeeds for admin"
    (let [handler (utils/get-handler)
          {:keys [status body]} (handler (-> (mock/request :put "/user/6")
                                             (mock/json-body {:username "Adam"})
                                             (mock/header "Authorization" (str "Bearer " (utils/create-token {:role "admin"})))))
          expected-user (-> utils/update-user-admin (assoc :deposit 0 :username "Adam") (dissoc :password))]
      (is (= expected-user (json/read-value body json/keyword-keys-object-mapper)) "returns user details")
      (is (= 200 status) "returns 20X status")))
  (testing "update user requires authentication"
    (let [handler (utils/get-handler)
          {:keys [status]} (handler (-> (mock/request :put "/user/6")
                                        (mock/json-body {:username "Adam"})))]
      (is (= 403 status) "update user is not allowed for unauthenticated users"))))

(deftest delete-user-test
  (testing "delete user succeeds when authorized"
    (let [db-conn (:db.sql/connection (utils/system-state))
          record-before (jdbc/execute-one! db-conn ["select username from users where id = ?" 4])
          handler (utils/get-handler)
          {:keys [status]} (handler (-> (mock/request :delete "/user/4")
                                        (mock/header "Authorization" (str "Bearer " (utils/create-token {:id 4})))))
          record-after (jdbc/execute-one! db-conn ["select username from users where id = ?" 4])]
      (is (= 204 status) "returns 20X status")
      (is (not= record-before record-after) "user was deleted")))
  (testing "delete user requires authentication"
    (let [handler (utils/get-handler)
          {:keys [status]} (handler (mock/request :delete "/user/4"))]
      (is (= 403 status) "delete user is not allowed for unauthenticated users"))))