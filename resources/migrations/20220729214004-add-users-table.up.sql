CREATE TABLE IF NOT EXISTS users
(id INTEGER PRIMARY KEY,
username varchar(25) UNIQUE NOT NULL,
password varchar(30) NOT NULL,
deposit TEXT DEFAULT '',
role varchar(25) DEFAULT "buyer");