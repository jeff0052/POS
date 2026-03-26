-- Real user table for authentication
CREATE TABLE auth_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    username VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'STORE_ADMIN',
    merchant_id BIGINT NULL,
    store_id BIGINT NULL,
    user_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_user_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_auth_user_store FOREIGN KEY (store_id) REFERENCES stores(id)
);

-- Seed a default admin user (password: admin123, BCrypt hash)
INSERT INTO auth_users (user_id, username, password_hash, display_name, role, merchant_id, store_id)
SELECT 'USR_DEFAULT_ADMIN', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       'Default Admin', 'PLATFORM_ADMIN', NULL, NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM auth_users WHERE username = 'admin');
