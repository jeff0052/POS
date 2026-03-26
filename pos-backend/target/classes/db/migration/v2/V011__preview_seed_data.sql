INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10002, 101, 'T1', 'T1', 2, 'AVAILABLE', 1
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T1');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10003, 101, 'T2', 'T2', 4, 'AVAILABLE', 2
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T2');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10004, 101, 'T3', 'T3', 4, 'AVAILABLE', 3
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T3');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10005, 101, 'T4', 'T4', 2, 'AVAILABLE', 4
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T4');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10006, 101, 'T5', 'T5', 6, 'AVAILABLE', 5
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T5');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10007, 101, 'T7', 'T7', 3, 'AVAILABLE', 7
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T7');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10008, 101, 'T8', 'T8', 5, 'AVAILABLE', 8
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T8');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10009, 101, 'T9', 'T9', 4, 'AVAILABLE', 9
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T9');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10010, 101, 'T10', 'T10', 2, 'AVAILABLE', 10
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T10');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10011, 101, 'T11', 'T11', 2, 'AVAILABLE', 11
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T11');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10012, 101, 'T12', 'T12', 4, 'AVAILABLE', 12
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T12');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10013, 101, 'T13', 'T13', 5, 'AVAILABLE', 13
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T13');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10014, 101, 'T14', 'T14', 2, 'AVAILABLE', 14
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T14');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10015, 101, 'T15', 'T15', 4, 'AVAILABLE', 15
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T15');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10016, 101, 'T16', 'T16', 4, 'AVAILABLE', 16
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T16');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10017, 101, 'T17', 'T17', 2, 'AVAILABLE', 17
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T17');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10018, 101, 'T18', 'T18', 2, 'AVAILABLE', 18
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T18');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10019, 101, 'T19', 'T19', 3, 'AVAILABLE', 19
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T19');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10020, 101, 'T20', 'T20', 4, 'AVAILABLE', 20
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T20');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10021, 101, 'T21', 'T21', 4, 'AVAILABLE', 21
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T21');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10022, 101, 'T22', 'T22', 6, 'AVAILABLE', 22
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T22');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10023, 101, 'T23', 'T23', 2, 'AVAILABLE', 23
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T23');

INSERT INTO store_tables (id, store_id, table_code, table_name, table_capacity, table_status, sort_order)
SELECT 10024, 101, 'T24', 'T24', 4, 'AVAILABLE', 24
WHERE NOT EXISTS (SELECT 1 FROM store_tables WHERE store_id = 101 AND table_code = 'T24');

INSERT INTO product_categories (id, store_id, category_code, category_name, is_active, sort_order)
SELECT 202, 101, 'snacks', 'Snacks', TRUE, 2
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE store_id = 101 AND category_code = 'snacks');

INSERT INTO product_categories (id, store_id, category_code, category_name, is_active, sort_order)
SELECT 203, 101, 'drinks', 'Drinks', TRUE, 3
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE store_id = 101 AND category_code = 'drinks');

INSERT INTO product_categories (id, store_id, category_code, category_name, is_active, sort_order)
SELECT 204, 101, 'desserts', 'Desserts', TRUE, 4
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE store_id = 101 AND category_code = 'desserts');

INSERT INTO product_categories (id, store_id, category_code, category_name, is_active, sort_order)
SELECT 205, 101, 'popular', 'Popular', TRUE, 5
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE store_id = 101 AND category_code = 'popular');

INSERT INTO products (id, store_id, category_id, product_code, product_name, product_status)
SELECT 302, 101, 201, 'black-pepper-beef-rice', 'Black Pepper Beef Rice', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = 101 AND product_code = 'black-pepper-beef-rice');

INSERT INTO products (id, store_id, category_id, product_code, product_name, product_status)
SELECT 303, 101, 202, 'crispy-chicken-bites', 'Crispy Chicken Bites', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = 101 AND product_code = 'crispy-chicken-bites');

INSERT INTO products (id, store_id, category_id, product_code, product_name, product_status)
SELECT 304, 101, 203, 'white-peach-soda', 'White Peach Soda', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = 101 AND product_code = 'white-peach-soda');

INSERT INTO products (id, store_id, category_id, product_code, product_name, product_status)
SELECT 305, 101, 203, 'brown-sugar-milk-tea', 'Brown Sugar Milk Tea', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = 101 AND product_code = 'brown-sugar-milk-tea');

INSERT INTO products (id, store_id, category_id, product_code, product_name, product_status)
SELECT 306, 101, 204, 'mango-pudding', 'Mango Pudding', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = 101 AND product_code = 'mango-pudding');

INSERT INTO products (id, store_id, category_id, product_code, product_name, product_status)
SELECT 307, 101, 205, 'chef-combo', 'Chef Combo', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = 101 AND product_code = 'chef-combo');

INSERT INTO products (id, store_id, category_id, product_code, product_name, product_status)
SELECT 308, 101, 202, 'truffle-fries', 'Truffle Fries', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE store_id = 101 AND product_code = 'truffle-fries');

INSERT INTO skus (id, product_id, sku_code, sku_name, base_price_cents, sku_status)
SELECT 402, 302, 'black-pepper-beef-rice-default', 'Black Pepper Beef Rice', 3400, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skus WHERE product_id = 302 AND sku_code = 'black-pepper-beef-rice-default');

INSERT INTO skus (id, product_id, sku_code, sku_name, base_price_cents, sku_status)
SELECT 403, 303, 'crispy-chicken-bites-default', 'Crispy Chicken Bites', 1600, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skus WHERE product_id = 303 AND sku_code = 'crispy-chicken-bites-default');

INSERT INTO skus (id, product_id, sku_code, sku_name, base_price_cents, sku_status)
SELECT 404, 304, 'white-peach-soda-default', 'White Peach Soda', 1200, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skus WHERE product_id = 304 AND sku_code = 'white-peach-soda-default');

INSERT INTO skus (id, product_id, sku_code, sku_name, base_price_cents, sku_status)
SELECT 405, 305, 'brown-sugar-milk-tea-default', 'Brown Sugar Milk Tea', 1400, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skus WHERE product_id = 305 AND sku_code = 'brown-sugar-milk-tea-default');

INSERT INTO skus (id, product_id, sku_code, sku_name, base_price_cents, sku_status)
SELECT 406, 306, 'mango-pudding-default', 'Mango Pudding', 1500, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skus WHERE product_id = 306 AND sku_code = 'mango-pudding-default');

INSERT INTO skus (id, product_id, sku_code, sku_name, base_price_cents, sku_status)
SELECT 407, 307, 'chef-combo-default', 'Chef Combo', 4600, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skus WHERE product_id = 307 AND sku_code = 'chef-combo-default');

INSERT INTO skus (id, product_id, sku_code, sku_name, base_price_cents, sku_status)
SELECT 408, 308, 'truffle-fries-default', 'Truffle Fries', 1900, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skus WHERE product_id = 308 AND sku_code = 'truffle-fries-default');

INSERT INTO store_sku_availability (store_id, sku_id, is_available)
SELECT 101, 401, TRUE
WHERE NOT EXISTS (SELECT 1 FROM store_sku_availability WHERE store_id = 101 AND sku_id = 401);

INSERT INTO store_sku_availability (store_id, sku_id, is_available)
SELECT 101, 402, TRUE
WHERE NOT EXISTS (SELECT 1 FROM store_sku_availability WHERE store_id = 101 AND sku_id = 402);

INSERT INTO store_sku_availability (store_id, sku_id, is_available)
SELECT 101, 403, TRUE
WHERE NOT EXISTS (SELECT 1 FROM store_sku_availability WHERE store_id = 101 AND sku_id = 403);

INSERT INTO store_sku_availability (store_id, sku_id, is_available)
SELECT 101, 404, TRUE
WHERE NOT EXISTS (SELECT 1 FROM store_sku_availability WHERE store_id = 101 AND sku_id = 404);

INSERT INTO store_sku_availability (store_id, sku_id, is_available)
SELECT 101, 405, TRUE
WHERE NOT EXISTS (SELECT 1 FROM store_sku_availability WHERE store_id = 101 AND sku_id = 405);

INSERT INTO store_sku_availability (store_id, sku_id, is_available)
SELECT 101, 406, TRUE
WHERE NOT EXISTS (SELECT 1 FROM store_sku_availability WHERE store_id = 101 AND sku_id = 406);

INSERT INTO store_sku_availability (store_id, sku_id, is_available)
SELECT 101, 407, TRUE
WHERE NOT EXISTS (SELECT 1 FROM store_sku_availability WHERE store_id = 101 AND sku_id = 407);

INSERT INTO store_sku_availability (store_id, sku_id, is_available)
SELECT 101, 408, TRUE
WHERE NOT EXISTS (SELECT 1 FROM store_sku_availability WHERE store_id = 101 AND sku_id = 408);
