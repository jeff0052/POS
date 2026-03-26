-- Real auth_users table
CREATE TABLE IF NOT EXISTS auth_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128),
    role VARCHAR(32) NOT NULL DEFAULT 'CASHIER',
    merchant_id BIGINT NULL,
    store_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_auth_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed: platform admin (password: admin123)
INSERT INTO auth_users (username, password_hash, display_name, role, merchant_id, store_id, status)
SELECT 'admin', '$2b$12$x9Q2FhlaQDprOupevlqxGeHaqb9/IbkjZI.MLvGfuREcKQimB3oqK', 'Platform Admin', 'PLATFORM_ADMIN', NULL, NULL, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM auth_users WHERE username = 'admin');

-- Seed: store admin (password: admin123)
INSERT INTO auth_users (username, password_hash, display_name, role, merchant_id, store_id, status)
SELECT 'store_admin', '$2b$12$x9Q2FhlaQDprOupevlqxGeHaqb9/IbkjZI.MLvGfuREcKQimB3oqK', 'Store Admin', 'ADMIN', 1, 1, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM auth_users WHERE username = 'store_admin');

-- Seed: cashier (password: admin123)
INSERT INTO auth_users (username, password_hash, display_name, role, merchant_id, store_id, status)
SELECT 'cashier', '$2b$12$x9Q2FhlaQDprOupevlqxGeHaqb9/IbkjZI.MLvGfuREcKQimB3oqK', 'Cashier', 'CASHIER', 1, 1, 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM auth_users WHERE username = 'cashier');
