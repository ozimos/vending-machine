CREATE TABLE IF NOT EXISTS users
(id BIGINT PRIMARY KEY,
  username varchar(25) NOT NULL,
  password varchar(30) NOT NULL,
  deposit int,
  role varchar);