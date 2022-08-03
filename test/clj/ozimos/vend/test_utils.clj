(ns ozimos.vend.test-utils
  (:require
   [ozimos.vend.core :as core]
   [ozimos.vend.web.controllers.auth :as auth]
   [migratus.core]
   [next.jdbc.sql :as sql]
   [jsonista.core :as json]
   [buddy.sign.jwt :as jwt]
   [clojure.set :as set]
   [integrant.repl.state :as state]))

(def default-user {:id 1 :username "Greg" :password "greg" :role "buyer" :deposit ""})
(def buyer-user {:id 2 :username "Roman" :password "roman" :role "buyer" :deposit "5,10,20,5,5,50,20"})
(def seller-user {:id 3 :username "Alice" :password "alice" :role "seller" :deposit ""})
(def deletion-user {:id 4 :username "Maddy" :password "maddy" :role "seller" :deposit ""})
(def update-user-self {:id 5 :username "Michael" :password "mike" :role "buyer" :deposit ""})
(def update-user-admin {:id 6 :username "Ada" :password "lovelace" :role "buyer" :deposit ""})

(def default-product {:sellerId 3 :id 1 :productName "soap" :amountAvailable 5 :cost 5})
(def deletion-product {:sellerId 3 :id 2 :productName "mouthwash" :amountAvailable 6 :cost 10})
(def update-product {:sellerId 3 :id 3 :productName "sneaker" :amountAvailable 7 :cost 15})

(def key-conversions {:sellerId :seller_id
                      :amountAvailable :amount_available
                      :productName :product_name})

(def users [default-user buyer-user seller-user deletion-user update-user-self update-user-admin])
(def products [default-product deletion-product update-product])


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

(defn populate-db-with-users [f]
  (when-let [s (system-state)]
    (let [db-conn (:db.sql/connection s)
          users-password-hashed (map (fn [user] (auth/update-password-with-hash user)) users)]
      (sql/insert-multi! db-conn :users users-password-hashed)))
  (f))

(defn populate-db-with-products [f]
  (when-let [s (system-state)]
    (sql/insert-multi!
     (:db.sql/connection s)
     :products (map #(set/rename-keys % key-conversions) products)))
  (f))

(defn get-handler []
  (:handler/ring (system-state)))

(defn create-token [payload]
  (jwt/sign payload (:auth/jwt-secret (system-state))))

(defn json-decode [body]
  (json/read-value body json/keyword-keys-object-mapper))
