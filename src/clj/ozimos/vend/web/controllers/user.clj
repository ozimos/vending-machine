(ns ozimos.vend.web.controllers.user
  (:require
   [ring.util.http-response :as http-response]
   [clojure.string :as str]
   [ozimos.vend.web.routes.utils :as r.utils]))

(defn get-all-users
  [req]
  (let [query-fn (r.utils/route-data-key req :query-fn)
        res (query-fn :get-all-users! {})]
    (http-response/ok {:users res})))

(defn get-user
  [{{path :path} :parameters :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)
        res (query-fn :get-user-by-id! path)]
    (if res
      (http-response/ok res)
      (http-response/not-found {:message "No user found for the supplied id"}))))

(defn delete-user
  [{{path :path} :parameters :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)]
    (query-fn :delete-user-by-id! path)
    (http-response/no-content)))

(defn update-user
  [{{:keys [body path]} :parameters :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)]
    (try
      (if-let [updated-user (query-fn :update-user-by-id! (assoc path :updates body))]
        (http-response/ok updated-user)
        (http-response/no-content))
      (catch clojure.lang.ExceptionInfo e
        (if (some-> (.getMessage ^Exception e)
                    (str/includes? "UNIQUE constraint failed: users.username"))
          (http-response/bad-request {:humanized {:username ["The username is already taken"]}
                                      :message "Failed to update user"})
          (throw e))))))


