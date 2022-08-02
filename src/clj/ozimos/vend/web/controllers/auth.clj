(ns ozimos.vend.web.controllers.auth
  (:require
   [ring.util.http-response :as http-response]
   [ozimos.vend.web.routes.utils :as r.utils]
   [java-time :as jt]
   [buddy.hashers :as hashers]
   [buddy.sign.jwt :as jwt]))

(defn update-password-with-hash [user]
  (update user :password hashers/derive))

(defn register
  [{{body :body} :parameters :as req}]
  (let [{:keys [jwt-secret query-fn token-exp]} (r.utils/route-data req)
        user (query-fn :find-user! body)]
    (if user
      (http-response/conflict {:humanized {:username ["The username is already taken"]}
                               :message "User registration failed"})
      (let  [body (if (:role body) body (assoc body :role "buyer"))
             {id :id} (query-fn :create-user! (update-password-with-hash body))
             exp (.toEpochSecond ^java.time.ZonedDateTime
                                 (jt/plus (jt/zoned-date-time)
                                          (jt/minutes token-exp)))]
        (http-response/created (str "/user/" id)
                               {:message "Your account has been created"
                                :id id
                                :token (jwt/sign (-> body
                                                     (assoc :id id)
                                                     (dissoc :password))
                                                 jwt-secret {:exp exp})})))))

(defn login
  [{{{:keys [password] :as body} :body} :parameters :as req}]
  (let [{:keys [jwt-secret query-fn]} (r.utils/route-data req)
        {hashed-password :password :as user} (query-fn :find-user! body)]
    (if (and user (:valid (hashers/verify password hashed-password)))
      (http-response/ok {:message "Login Success"
                         :token (jwt/sign (dissoc user password) jwt-secret)})
      (http-response/bad-request! {:humanized {:password ["Incorrect username or password"]
                                               :username ["Incorrect username or password"]}
                                   :message "Login failed"}))))
