-- Staff, Role, and Permission tables for POS access control

CREATE TABLE staff (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    staff_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    staff_name VARCHAR(128) NOT NULL,
    staff_code VARCHAR(32) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    role_code VARCHAR(32) NOT NULL DEFAULT 'CASHIER',
    staff_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    phone VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_staff_store_code UNIQUE (store_id, staff_code)
);

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL
);

CREATE TABLE role_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    CONSTRAINT uk_role_permission UNIQUE (role_code, permission_code),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_code) REFERENCES roles(role_code)
);

-- Seed default roles
INSERT INTO roles (role_code, role_name, description) VALUES
('OWNER', 'Store Owner', 'Full access to all features'),
('MANAGER', 'Store Manager', 'Manage staff, menu, promotions, reports'),
('CASHIER', 'Cashier', 'Process orders and settlements'),
('WAITER', 'Waiter', 'Take orders and manage tables'),
('KITCHEN', 'Kitchen Staff', 'View and manage kitchen orders');

-- Seed default role permissions
INSERT INTO role_permissions (role_code, permission_code) VALUES
('OWNER', 'ALL'),
('MANAGER', 'MENU_MANAGE'), ('MANAGER', 'STAFF_MANAGE'), ('MANAGER', 'PROMOTION_MANAGE'),
('MANAGER', 'REPORT_VIEW'), ('MANAGER', 'ORDER_MANAGE'), ('MANAGER', 'SETTLEMENT_MANAGE'),
('CASHIER', 'ORDER_MANAGE'), ('CASHIER', 'SETTLEMENT_MANAGE'), ('CASHIER', 'MEMBER_VIEW'),
('WAITER', 'ORDER_MANAGE'), ('WAITER', 'TABLE_MANAGE'),
('KITCHEN', 'KITCHEN_VIEW'), ('KITCHEN', 'ORDER_VIEW');
