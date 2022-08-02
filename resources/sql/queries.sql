-- Place your queries here. Docs available https://www.hugsql.org/

-- User Queries

-- :name create-user! :<! :1
-- :doc creates a new user record
INSERT INTO users
(username, password, role)
VALUES (:username, :password, :role) RETURNING id

-- :name find-user! :? :1
-- :doc gets user record by username
SELECT * FROM users WHERE
username = :username

-- :name get-user-by-id! :? :1
-- :doc gets user record by user id
SELECT id, username, role, deposit FROM users WHERE
id = :id

-- :name get-all-users! :? :raw
-- :doc gets all user records
SELECT id, username, role, deposit FROM users

-- :name update-user-by-id! :<! :1
-- :doc updates user record
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE users SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
WHERE
id = :id
RETURNING id, username, role, deposit

-- :name delete-user-by-id! :! :n
DELETE FROM users WHERE id = :id


-- Product Queries

-- :name create-product! :<! :1
-- :doc creates a new product record
INSERT INTO products
(amount_available, product_name, cost, seller_id)
VALUES (:amount_available, :product_name, :cost, :seller_id) RETURNING id

-- :name get-product-by-id! :? :1
-- :doc gets product record by product id
SELECT id, amount_available, product_name, cost, seller_id FROM products WHERE
id = :id

-- :name get-all-products! :? :raw
-- :doc gets all product records
SELECT id, amount_available, product_name, cost, seller_id FROM products

-- :name update-product-by-id! :<! :1
-- :doc updates product record
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE products SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
WHERE
id = :id AND seller_id = :seller_id
RETURNING id, amount_available, product_name, cost, seller_id

-- :name delete-product-by-id! :! :n
DELETE FROM products WHERE id = :id AND seller_id = :seller_id