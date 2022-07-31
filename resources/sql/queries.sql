-- Place your queries here. Docs available https://www.hugsql.org/

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
-- :doc gets user record by username
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
RETURNING id, username, role

-- :name delete-user-by-id! :! :n
DELETE FROM users WHERE id = :id