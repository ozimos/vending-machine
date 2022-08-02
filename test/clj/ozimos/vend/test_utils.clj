(ns ozimos.vend.test-utils
  (:require
   [ozimos.vend.core :as core]
   [ozimos.vend.web.controllers.auth :as auth]
   [migratus.core]
   [next.jdbc.sql :as sql]
   [buddy.sign.jwt :as jwt]
   [integrant.core :as ig]
   [integrant.repl.state :as state]))

(def default-user {:id 1 :username "Greg" :password "greg" :role "buyer"})
(def buyer-user {:id 2 :username "Roman" :password "roman" :role "buyer"})
(def seller-user {:id 3 :username "Alice" :password "alice" :role "seller"})
(def deletion-user {:id 4 :username "Maddy" :password "maddy" :role "seller"})
(def update-user-self {:id 5 :username "Michael" :password "mike" :role "buyer"})
(def update-user-admin {:id 6 :username "Ada" :password "lovelace" :role "buyer"})

(def users [default-user buyer-user seller-user deletion-user update-user-self update-user-admin])

(defmethod ig/init-key :auth/jwt-secret [_ k] k)

(defn system-state
  []
  (or @core/system state/system))

(defn system-fixture
  []
  (fn [f]
    (when (nil? (system-state))
      (core/start-app {:opts {:profile :test}} [:handler/ring :db.sql/migrations :auth/jwt-secret])
      (f))))

(defn reset-db [f]
  (when-let [s (system-state)]
    (migratus.core/reset (:db.sql/migrations s)))
  (f))

(defn populate-db [f]
  (when-let [s (system-state)]
    (let [db-conn (:db.sql/connection s)
          users-password-hashed (map (fn [user] (auth/update-password-with-hash user)) users)]
      (sql/insert-multi! db-conn :users users-password-hashed)))
  (f))

(defn get-handler []
  (:handler/ring (system-state)))

(defn create-token [payload]
  (jwt/sign payload (:auth/jwt-secret (system-state))))
