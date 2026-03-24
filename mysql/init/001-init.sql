CREATE TABLE IF NOT EXISTS stores (
  id BIGINT PRIMARY KEY,
  store_name VARCHAR(100) NOT NULL,
  store_code VARCHAR(64) NOT NULL,
  address VARCHAR(255),
  phone VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS store_settings (
  id BIGINT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  receipt_title VARCHAR(128),
  receipt_footer VARCHAR(255),
  printer_config_json TEXT,
  payment_config_json TEXT
);

CREATE TABLE IF NOT EXISTS product_categories (
  id BIGINT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS products (
  id BIGINT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  category_id BIGINT,
  name VARCHAR(128) NOT NULL,
  barcode VARCHAR(64),
  price_cents BIGINT NOT NULL,
  stock_qty INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL,
  store_id BIGINT NOT NULL,
  cashier_id BIGINT NOT NULL,
  paid_amount_cents BIGINT,
  order_status VARCHAR(32),
  payment_status VARCHAR(32),
  print_status VARCHAR(32),
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS qr_table_orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL,
  queue_no VARCHAR(64) NOT NULL,
  store_code VARCHAR(64) NOT NULL,
  store_name VARCHAR(100) NOT NULL,
  table_code VARCHAR(64) NOT NULL,
  settlement_status VARCHAR(32) NOT NULL,
  member_name VARCHAR(100),
  member_tier VARCHAR(64),
  original_amount_cents BIGINT NOT NULL,
  member_discount_cents BIGINT NOT NULL DEFAULT 0,
  promotion_discount_cents BIGINT NOT NULL DEFAULT 0,
  payable_amount_cents BIGINT NOT NULL,
  items_json TEXT,
  created_at DATETIME NOT NULL
);

INSERT INTO stores (id, store_name, store_code, address, phone, status, deleted)
VALUES (1001, 'Demo Store', 'STORE1001', 'Shanghai', '13800000000', 'ENABLED', 0)
ON DUPLICATE KEY UPDATE store_name = VALUES(store_name);

INSERT INTO store_settings (id, store_id, receipt_title, receipt_footer, printer_config_json, payment_config_json)
VALUES (1, 1001, 'Demo Store', 'Thanks for visiting', '{}', '{}')
ON DUPLICATE KEY UPDATE receipt_title = VALUES(receipt_title);

INSERT INTO product_categories (id, store_id, name, sort_order, status, deleted)
VALUES
  (1, 1001, 'Drinks', 1, 'ENABLED', 0),
  (2, 1001, 'Meals', 2, 'ENABLED', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO products (id, store_id, category_id, name, barcode, price_cents, stock_qty, status, deleted)
VALUES
  (1, 1001, 1, 'Coke', '692000000001', 500, 100, 'ENABLED', 0),
  (2, 1001, 2, 'Fried Rice', '692000000002', 1800, 40, 'ENABLED', 0),
  (3, 1001, 1, 'Milk Tea', '692000000003', 1200, 60, 'ENABLED', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO orders (id, order_no, store_id, cashier_id, paid_amount_cents, order_status, payment_status, print_status, created_at)
VALUES
  (1, 'POS202603200001', 1001, 1, 2800, 'PAID', 'SDK_PAY', 'PRINT_SUCCESS', '2026-03-20 09:21:00'),
  (2, 'POS202603200002', 1001, 2, 1200, 'PENDING', 'CASH', 'NOT_PRINTED', '2026-03-20 09:34:00')
ON DUPLICATE KEY UPDATE order_status = VALUES(order_status);
