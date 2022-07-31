(ns ozimos.vend.web.controllers.auth
  (:require
   [ring.util.http-response :as http-response]
   [ozimos.vend.web.routes.utils :as r.utils]
   [buddy.hashers :as hashers]
   [buddy.sign.jwt :as jwt]))

(defn register
  [{{body :body} :parameters :as req}]
  (let [{:keys [jwt-secret query-fn]} (r.utils/route-data req)
        user (query-fn :find-user! body)]
    (if user
      (http-response/conflict {:errors {:username ["The username is already taken"]}})
      (let  [body (if (:role body) body (assoc body :role "buyer"))
             {id :id} (query-fn :create-user! (update body :password hashers/derive))]
        (http-response/created (str "/user/" id) {:message "Your account has been created"
                                                  :id id
                                                  :token (jwt/sign (dissoc body :password) jwt-secret)})))))

(defn login
  [{{{:keys [password] :as body} :body} :parameters :as req}]
  (let [{:keys [jwt-secret query-fn]} (r.utils/route-data req)
        {hashed-password :password :as user} (query-fn :find-user! body)]
    (if (and user (:valid (hashers/verify password hashed-password)))
      (http-response/ok {:message "Login Success"
                         :token (jwt/sign (dissoc user password) jwt-secret)})
      (http-response/bad-request! {:errors {:password ["Incorrect username or password"]
                                            :username ["Incorrect username or password"]}}))))
