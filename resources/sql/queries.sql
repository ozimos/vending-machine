-- Place your queries here. Docs available https://www.hugsql.org/

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(username, password)
VALUES (:username, :password)

-- :name find-user! :? :1
-- :doc gets existing user record
SELECT * FROM users where
username = :username