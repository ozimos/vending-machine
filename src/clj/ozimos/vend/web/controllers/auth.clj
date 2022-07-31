(ns ozimos.vend.web.controllers.auth
  (:require
   [ring.util.http-response :as http-response]
   [buddy.hashers :as hashers]
   [buddy.sign.jwt :as jwt]))

(defn make-register-handler [{:keys [query-fn]}]
  (fn [{{body :body} :parameters}]
    (let [user (query-fn :find-user! body)]
      (if user
        (http-response/conflict {:errors {:username ["The username is already taken"]}})
        (do (query-fn :create-user! (update body :password hashers/derive))
            (http-response/created {:message "Your account has been created"}))))))

(defn make-login-handler [{:keys [jwt-secret query-fn]}]
  (fn [{{{:keys [username password] :as body} :body} :parameters}]
    (let [{hashed-password :password :as user} (query-fn :find-user! body)]
      (if (and user (:valid (hashers/verify password hashed-password)))
        (http-response/ok {:message "Login Success"
                           :token (jwt/sign {:username username} jwt-secret)})
        (http-response/bad-request! {:errors {:password ["Incorrect username or password"]
                                              :username ["Incorrect username or password"]}})))))
