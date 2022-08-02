CREATE TABLE IF NOT EXISTS products
(id INTEGER PRIMARY KEY,
amount_available int NOT NULL,
product_name TEXT NOT NULL,
cost int NOT NULL CHECK(cost%5 = 0),
seller_id int REFERENCES users(id) ON DELETE CASCADE,
UNIQUE(product_name, seller_id)
);        