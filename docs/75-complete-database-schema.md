# FounderPOS Complete Database Schema — 119 Tables

**Version:** V20260328010
**Date:** 2026-03-28
**Status:** REFERENCE
**Sources:** Flyway V001–V065 (48 original tables) + docs 66–73 (71 new tables)

---

## 1. Summary Table — All 119 Tables by Module

| # | Module | Table | Columns | Source |
|---|--------|-------|---------|--------|
| **Core** | | | | |
| 1 | Core | merchants | 8 | Flyway V001 |
| 2 | Core | merchant_configs | 5 | Flyway V001 |
| 3 | Core | brands | 7 | Flyway V055 |
| 4 | Core | brand_countries | 8 | Flyway V055 |
| 5 | Core | stores | 12 | Flyway V002 + V055 |
| 6 | Core | store_tables | 10 | Flyway V002 + doc/66 ALTER |
| 7 | Core | store_terminals | 8 | Flyway V002 |
| **Catalog** | | | | |
| 8 | Catalog | product_categories | 7 | Flyway V003 |
| 9 | Catalog | products | 14 | Flyway V003 + V016 + V065 + doc/66 ALTER |
| 10 | Catalog | skus | 30 | Flyway V003 + V065 + V030 + doc/66 + doc/67 ALTER |
| 11 | Catalog | store_sku_availability | 8 | Flyway V003 + V030 |
| 12 | Catalog | sku_price_overrides | 9 | doc/66 |
| 13 | Catalog | sku_channel_configs | 10 | doc/67 |
| 14 | Catalog | sku_faq | 5 | doc/67 |
| 15 | Catalog | modifier_groups | 10 | doc/67 |
| 16 | Catalog | modifier_options | 8 | doc/67 |
| 17 | Catalog | sku_modifier_group_bindings | 4 | doc/67 |
| **Time-slot Menu** | | | | |
| 18 | Time-slot Menu | menu_time_slots | 11 | doc/66 |
| 19 | Time-slot Menu | menu_time_slot_products | 4 | doc/66 |
| **Buffet** | | | | |
| 20 | Buffet | buffet_packages | 10 | doc/66 |
| 21 | Buffet | buffet_package_items | 5 | doc/66 |
| **Orders** | | | | |
| 22 | Orders | active_table_orders | 22 | Flyway V004 + V030 |
| 23 | Orders | active_table_order_items | 11 | Flyway V004 |
| 24 | Orders | order_events | 8 | Flyway V004 |
| 25 | Orders | table_sessions | 15 | Flyway V012 + doc/66 + doc/73 ALTER |
| 26 | Orders | submitted_orders | 24 | Flyway V012 + V030 + doc/66 + doc/73 ALTER |
| 27 | Orders | submitted_order_items | 10 | Flyway V012 |
| **Kitchen KDS** | | | | |
| 28 | Kitchen KDS | kitchen_stations | 10 | doc/66 |
| 29 | Kitchen KDS | kitchen_tickets | 14 | doc/66 |
| 30 | Kitchen KDS | kitchen_ticket_items | 6 | doc/66 |
| **Settlement** | | | | |
| 31 | Settlement | settlement_records | 16 | Flyway V008 + V019 + V030 + V050 |
| 32 | Settlement | payment_attempts | 18 | Flyway V015 |
| 33 | Settlement | refund_records | 13 | Flyway V050 |
| **Inventory** | | | | |
| 34 | Inventory | inventory_items | 19 | doc/66 + doc/70 ALTER |
| 35 | Inventory | inventory_batches | 16 | doc/70 |
| 36 | Inventory | recipes | 6 | doc/66 |
| 37 | Inventory | purchase_invoices | 11 | doc/66 |
| 38 | Inventory | purchase_invoice_items | 6 | doc/66 |
| 39 | Inventory | purchase_orders | 16 | doc/70 |
| 40 | Inventory | purchase_order_items | 8 | doc/70 |
| 41 | Inventory | inventory_movements | 10 | doc/66 + doc/70 ALTER |
| 42 | Inventory | suppliers | 12 | doc/66 + doc/70 ALTER |
| 43 | Inventory | supplier_price_history | 8 | doc/70 |
| 44 | Inventory | order_suggestions | 12 | doc/66 + doc/70 ALTER |
| 45 | Inventory | stocktake_tasks | 13 | doc/70 |
| 46 | Inventory | stocktake_items | 12 | doc/70 |
| 47 | Inventory | waste_records | 14 | doc/70 |
| 48 | Inventory | inventory_transfers | 10 | doc/70 |
| 49 | Inventory | inventory_transfer_items | 6 | doc/70 |
| **Members / CRM** | | | | |
| 50 | Members/CRM | members | 19 | Flyway V009 + V030 + doc/71 ALTER |
| 51 | Members/CRM | member_accounts | 16 | Flyway V009 + doc/71 + doc/72 ALTER |
| 52 | Members/CRM | member_recharge_orders | 8 | Flyway V013 |
| 53 | Members/CRM | member_points_ledger | 12 | Flyway V013 + doc/72 ALTER |
| 54 | Members/CRM | member_cash_ledger | 10 | doc/72 |
| 55 | Members/CRM | member_tier_rules | 16 | doc/71 |
| 56 | Members/CRM | coupon_templates | 17 | doc/71 |
| 57 | Members/CRM | member_coupons | 11 | doc/71 |
| 58 | Members/CRM | recharge_campaigns | 13 | doc/71 |
| 59 | Members/CRM | points_rules | 18 | doc/72 |
| 60 | Members/CRM | points_expiry_rules | 9 | doc/72 |
| 61 | Members/CRM | points_batches | 12 | doc/72 |
| 62 | Members/CRM | points_deduction_rules | 10 | doc/72 |
| 63 | Members/CRM | cash_balance_rules | 9 | doc/72 |
| 64 | Members/CRM | points_redemption_items | 13 | doc/71 |
| 65 | Members/CRM | points_redemption_records | 7 | doc/71 |
| 66 | Members/CRM | referral_rewards_config | 13 | doc/71 |
| 67 | Members/CRM | referral_records | 12 | doc/71 |
| 68 | Members/CRM | member_tags | 8 | doc/71 |
| 69 | Members/CRM | member_tag_assignments | 5 | doc/71 |
| 70 | Members/CRM | member_consumption_profiles | 17 | doc/71 |
| 71 | Members/CRM | marketing_campaigns | 17 | doc/71 |
| 72 | Members/CRM | marketing_send_records | 12 | doc/71 |
| **Auth / RBAC** | | | | |
| 73 | Auth/RBAC | users | 14 | doc/68 (replaces auth_users + staff) |
| 74 | Auth/RBAC | permissions | 5 | doc/68 |
| 75 | Auth/RBAC | custom_roles | 10 | doc/68 |
| 76 | Auth/RBAC | custom_role_permissions | 3 | doc/68 |
| 77 | Auth/RBAC | user_roles | 5 | doc/68 |
| 78 | Auth/RBAC | user_store_access | 6 | doc/68 |
| **Employees** | | | | |
| 79 | Employees | employees | 24 | doc/69 |
| 80 | Employees | shift_templates | 8 | doc/69 |
| 81 | Employees | employee_schedules | 13 | doc/69 |
| 82 | Employees | attendance_records | 17 | doc/69 |
| 83 | Employees | leave_requests | 10 | doc/69 |
| 84 | Employees | leave_balances | 6 | doc/69 |
| 85 | Employees | payroll_periods | 7 | doc/69 |
| 86 | Employees | payroll_records | 19 | doc/69 |
| 87 | Employees | employee_performance_log | 7 | doc/69 |
| **Shifts** | | | | |
| 88 | Shifts | cashier_shifts | 17 | Flyway V026 + doc/69 ALTER |
| 89 | Shifts | cashier_shift_settlements | 6 | Flyway V026 |
| **Reservations** | | | | |
| 90 | Reservations | reservations | 10 | Flyway V018 |
| **Queue** | | | | |
| 91 | Queue | queue_tickets | 13 | doc/66 |
| **GTO** | | | | |
| 92 | GTO | gto_export_batches | 11 | Flyway V020 |
| 93 | GTO | gto_export_items | 9 | Flyway V020 |
| **Promotions** | | | | |
| 94 | Promotions | promotion_rules | 14 | Flyway V010 + V030 + V035 |
| 95 | Promotions | promotion_rule_conditions | 4 | Flyway V010 |
| 96 | Promotions | promotion_rule_rewards | 6 | Flyway V010 + V035 |
| 97 | Promotions | promotion_hits | 7 | Flyway V010 |
| **Recommendations** | | | | |
| 98 | Recommendations | recommendation_slots | 10 | doc/66 |
| 99 | Recommendations | recommendation_slot_items | 6 | doc/66 |
| **Delivery** | | | | |
| 100 | Delivery | delivery_platform_configs | 10 | doc/66 |
| 101 | Delivery | delivery_orders | 15 | doc/66 |
| **Channels** | | | | |
| 102 | Channels | channels | 10 | doc/73 |
| 103 | Channels | channel_commission_rules | 15 | doc/73 |
| 104 | Channels | order_channel_attribution | 7 | doc/73 |
| 105 | Channels | channel_commission_records | 13 | doc/73 |
| 106 | Channels | channel_settlement_batches | 15 | doc/73 |
| 107 | Channels | channel_performance_daily | 15 | doc/73 |
| **AI** | | | | |
| 108 | AI | action_log | 12 | Flyway V031 |
| 109 | AI | ai_proposal | 13 | Flyway V032 |
| 110 | AI | ai_recommendations | 16 | Flyway V040 |
| 111 | AI | ai_scheduled_checks | 6 | Flyway V040 |
| 112 | AI | restaurant_agents | 12 | Flyway V045 |
| 113 | AI | agent_wallets | 8 | Flyway V045 |
| 114 | AI | wallet_transactions | 9 | Flyway V045 |
| 115 | AI | agent_interactions | 15 | Flyway V045 |
| **Assets** | | | | |
| 116 | Assets | image_assets | 8 | Flyway V065 |
| **Legacy (to be replaced by doc/68 users)** | | | | |
| 117 | Legacy | auth_users | 9 | Flyway V060 |
| 118 | Legacy | staff | 10 | Flyway V023 |
| 119 | Legacy | roles / role_permissions | 3+3 | Flyway V023 |

> **Note:** Tables 117-119 (auth_users, staff, roles, role_permissions) are legacy tables that will be replaced by the new `users` + RBAC tables (73-78) during migration. They are counted toward the 119 total as they exist in the current schema.

---

## 2. Module-by-Module DDL

### 2.1 Core Module

#### merchants (Flyway V001)

```sql
CREATE TABLE merchants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_code VARCHAR(64) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Singapore',
    currency_code VARCHAR(16) NOT NULL DEFAULT 'SGD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_merchants_code UNIQUE (merchant_code)
);
```

#### merchant_configs (Flyway V001)

```sql
CREATE TABLE merchant_configs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    config_key VARCHAR(128) NOT NULL,
    config_value JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_merchant_configs_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_merchant_configs_key UNIQUE (merchant_id, config_key)
);
```

#### brands (Flyway V055)

```sql
CREATE TABLE brands (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    brand_code VARCHAR(64) NOT NULL,
    brand_name VARCHAR(255) NOT NULL,
    brand_logo_url VARCHAR(512) NULL,
    brand_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_brand_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_brand_code UNIQUE (merchant_id, brand_code)
);
```

#### brand_countries (Flyway V055)

```sql
CREATE TABLE brand_countries (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    brand_id BIGINT NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    country_name VARCHAR(128) NOT NULL,
    currency_code VARCHAR(16) NOT NULL DEFAULT 'SGD',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Singapore',
    tax_rate_percent DECIMAL(5,2) NOT NULL DEFAULT 9.00,
    country_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bc_brand FOREIGN KEY (brand_id) REFERENCES brands(id),
    CONSTRAINT uk_brand_country UNIQUE (brand_id, country_code)
);
```

#### stores (Flyway V002 + V055)

```sql
CREATE TABLE stores (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    brand_id BIGINT NULL,
    country_id BIGINT NULL,
    store_code VARCHAR(64) NOT NULL,
    store_name VARCHAR(255) NOT NULL,
    store_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    phone VARCHAR(64) NULL,
    address_line VARCHAR(255) NULL,
    city VARCHAR(128) NULL,
    country_code VARCHAR(16) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stores_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_stores_code UNIQUE (store_code),
    CONSTRAINT uk_stores_merchant_store UNIQUE (merchant_id, store_code),
    INDEX idx_stores_brand (brand_id),
    INDEX idx_stores_country (country_id)
);
```

#### store_tables (Flyway V002 + doc/66 ALTER)

```sql
CREATE TABLE store_tables (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    table_code VARCHAR(64) NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    area VARCHAR(64) NULL,
    table_capacity INT NOT NULL DEFAULT 4,
    table_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    sort_order INT NOT NULL DEFAULT 0,
    qr_code_url VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_tables_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_store_tables_code UNIQUE (store_id, table_code)
);
```

#### store_terminals (Flyway V002)

```sql
CREATE TABLE store_terminals (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    terminal_code VARCHAR(64) NOT NULL,
    terminal_name VARCHAR(128) NOT NULL,
    terminal_type VARCHAR(32) NOT NULL DEFAULT 'POS',
    device_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_terminals_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_store_terminals_code UNIQUE (store_id, terminal_code)
);
```

---

### 2.2 Catalog Module

#### product_categories (Flyway V003)

```sql
CREATE TABLE product_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_categories_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_product_categories_code UNIQUE (store_id, category_code)
);
```

#### products (Flyway V003 + V016 + V065 + doc/66 ALTER)

```sql
CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    image_id VARCHAR(64) NULL,
    menu_modes JSON NULL,
    is_featured BOOLEAN DEFAULT FALSE,
    is_recommended BOOLEAN DEFAULT FALSE,
    allergen_info VARCHAR(512) NULL,
    sort_order INT DEFAULT 0,
    product_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    attribute_config_json TEXT NULL,
    modifier_config_json TEXT NULL,
    combo_slot_config_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES product_categories(id),
    CONSTRAINT uk_products_code UNIQUE (store_id, product_code)
);
```

#### skus (Flyway V003 + V065 + V030 + doc/66 + doc/67 ALTER)

```sql
CREATE TABLE skus (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    merchant_id BIGINT NULL,
    sku_code VARCHAR(64) NOT NULL,
    sku_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    allergen_tags JSON NULL,
    spice_level INT DEFAULT 0,
    is_featured BOOLEAN DEFAULT FALSE,
    is_new BOOLEAN DEFAULT FALSE,
    tags JSON NULL,
    min_order_qty INT DEFAULT 1,
    max_order_qty INT DEFAULT 0,
    calories INT NULL,
    sort_order INT DEFAULT 0,
    base_price_cents BIGINT NOT NULL,
    cost_price_cents BIGINT NULL,
    image_id VARCHAR(64) NULL,
    station_id BIGINT NULL,
    print_route VARCHAR(64) NULL,
    prep_time_minutes INT NULL,
    kitchen_note_template VARCHAR(512) NULL,
    kitchen_priority INT DEFAULT 0,
    recipe_id BIGINT NULL,
    requires_stock_deduct BOOLEAN DEFAULT TRUE,
    sku_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    actor_type VARCHAR(32) DEFAULT 'HUMAN',
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) DEFAULT 'MANUAL',
    change_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_skus_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_skus_code UNIQUE (product_id, sku_code)
);
```

#### store_sku_availability (Flyway V003 + V030)

```sql
CREATE TABLE store_sku_availability (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    actor_type VARCHAR(32) DEFAULT 'HUMAN',
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) DEFAULT 'MANUAL',
    change_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_sku_availability_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_store_sku_availability_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_store_sku_availability UNIQUE (store_id, sku_id)
);
```

#### sku_price_overrides (doc/66)

```sql
CREATE TABLE sku_price_overrides (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    price_context VARCHAR(64) NOT NULL,
    price_context_ref VARCHAR(128) NULL,
    override_price_cents BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_spo_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT fk_spo_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_spo UNIQUE (sku_id, store_id, price_context, price_context_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### sku_channel_configs (doc/67)

```sql
CREATE TABLE sku_channel_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    channel_ref VARCHAR(128) NULL,
    channel_sku_name VARCHAR(255) NULL,
    channel_description TEXT NULL,
    channel_image_id VARCHAR(64) NULL,
    is_available BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_scc_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_scc UNIQUE (sku_id, channel_type, channel_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### sku_faq (doc/67)

```sql
CREATE TABLE sku_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    question VARCHAR(255) NOT NULL,
    answer TEXT NOT NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sf_sku FOREIGN KEY (sku_id) REFERENCES skus(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### modifier_groups (doc/67)

```sql
CREATE TABLE modifier_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    group_code VARCHAR(64) NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    selection_type VARCHAR(16) NOT NULL DEFAULT 'SINGLE',
    is_required BOOLEAN DEFAULT FALSE,
    min_select INT DEFAULT 0,
    max_select INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mg_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_mg UNIQUE (merchant_id, group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### modifier_options (doc/67)

```sql
CREATE TABLE modifier_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    option_code VARCHAR(64) NOT NULL,
    option_name VARCHAR(128) NOT NULL,
    price_adjustment_cents BIGINT DEFAULT 0,
    is_default BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mo_group FOREIGN KEY (group_id) REFERENCES modifier_groups(id) ON DELETE CASCADE,
    CONSTRAINT uk_mo UNIQUE (group_id, option_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### sku_modifier_group_bindings (doc/67)

```sql
CREATE TABLE sku_modifier_group_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    modifier_group_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    CONSTRAINT fk_smgb_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT fk_smgb_group FOREIGN KEY (modifier_group_id) REFERENCES modifier_groups(id),
    CONSTRAINT uk_smgb UNIQUE (sku_id, modifier_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.3 Time-slot Menu Module

#### menu_time_slots (doc/66)

```sql
CREATE TABLE menu_time_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    slot_code VARCHAR(64) NOT NULL,
    slot_name VARCHAR(128) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    applicable_days JSON DEFAULT '["MON","TUE","WED","THU","FRI","SAT","SUN"]',
    dining_modes JSON DEFAULT '["A_LA_CARTE"]',
    is_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mts_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_mts UNIQUE (store_id, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### menu_time_slot_products (doc/66)

```sql
CREATE TABLE menu_time_slot_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_slot_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_mtsp_slot FOREIGN KEY (time_slot_id) REFERENCES menu_time_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_mtsp_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_mtsp UNIQUE (time_slot_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.4 Buffet Module

#### buffet_packages (doc/66)

```sql
CREATE TABLE buffet_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    package_code VARCHAR(64) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    price_cents BIGINT NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 90,
    warning_before_minutes INT NOT NULL DEFAULT 10,
    overtime_fee_per_minute_cents BIGINT DEFAULT 0,
    package_status VARCHAR(32) DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_buffet_pkg_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_buffet_pkg UNIQUE (store_id, package_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### buffet_package_items (doc/66)

```sql
CREATE TABLE buffet_package_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    is_included BOOLEAN DEFAULT TRUE,
    surcharge_cents BIGINT DEFAULT 0,
    CONSTRAINT fk_bpi_package FOREIGN KEY (package_id) REFERENCES buffet_packages(id) ON DELETE CASCADE,
    CONSTRAINT fk_bpi_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_bpi UNIQUE (package_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.5 Orders Module

#### active_table_orders (Flyway V004 + V030)

```sql
CREATE TABLE active_table_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    active_order_id VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    order_source VARCHAR(32) NOT NULL,
    dining_type VARCHAR(32) NOT NULL DEFAULT 'DINE_IN',
    status VARCHAR(32) NOT NULL,
    member_id BIGINT NULL,
    cashier_id BIGINT NULL,
    current_shift_id BIGINT NULL,
    original_amount_cents BIGINT NOT NULL DEFAULT 0,
    member_discount_cents BIGINT NOT NULL DEFAULT 0,
    promotion_discount_cents BIGINT NOT NULL DEFAULT 0,
    payable_amount_cents BIGINT NOT NULL DEFAULT 0,
    pricing_snapshot_json JSON NULL,
    promotion_snapshot_json JSON NULL,
    settlement_snapshot_json JSON NULL,
    actor_type VARCHAR(32) DEFAULT 'HUMAN',
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) DEFAULT 'MANUAL',
    change_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_active_table_orders_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_active_table_orders_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_active_table_orders_table FOREIGN KEY (table_id) REFERENCES store_tables(id),
    CONSTRAINT uk_active_table_orders_id UNIQUE (active_order_id),
    CONSTRAINT uk_active_table_orders_no UNIQUE (order_no),
    CONSTRAINT uk_active_table_orders_table UNIQUE (store_id, table_id)
);
```

#### active_table_order_items (Flyway V004)

```sql
CREATE TABLE active_table_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    active_order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_name_snapshot VARCHAR(255) NOT NULL,
    sku_code_snapshot VARCHAR(64) NOT NULL,
    unit_price_snapshot_cents BIGINT NOT NULL,
    member_price_snapshot_cents BIGINT NULL,
    quantity INT NOT NULL,
    item_remark VARCHAR(255) NULL,
    option_snapshot_json JSON NULL,
    line_total_cents BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_active_table_order_items_order
        FOREIGN KEY (active_order_id) REFERENCES active_table_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_active_table_order_items_sku
        FOREIGN KEY (sku_id) REFERENCES skus(id)
);
```

#### order_events (Flyway V004)

```sql
CREATE TABLE order_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    active_order_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64) NULL,
    decision_source VARCHAR(32) NOT NULL DEFAULT 'HUMAN',
    event_payload_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_events_active_order
        FOREIGN KEY (active_order_id) REFERENCES active_table_orders(id) ON DELETE CASCADE
);
```

#### table_sessions (Flyway V012 + doc/66 + doc/73 ALTER)

```sql
CREATE TABLE table_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    session_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE',
    guest_count INT DEFAULT 1,
    buffet_package_id BIGINT NULL,
    buffet_started_at TIMESTAMP NULL,
    buffet_ends_at TIMESTAMP NULL,
    buffet_status VARCHAR(32) NULL,
    channel_code VARCHAR(64) NULL,
    tracking_value VARCHAR(255) NULL,
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_table_sessions_session_id UNIQUE (session_id),
    CONSTRAINT fk_table_sessions_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_table_sessions_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_table_sessions_table FOREIGN KEY (table_id) REFERENCES store_tables(id),
    INDEX idx_table_sessions_open (store_id, table_id, session_status)
);
```

#### submitted_orders (Flyway V012 + V030 + doc/66 + doc/73 ALTER)

```sql
CREATE TABLE submitted_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    submitted_order_id VARCHAR(64) NOT NULL,
    table_session_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    source_order_type VARCHAR(32) NOT NULL,
    dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE',
    external_platform VARCHAR(64) NULL,
    external_order_no VARCHAR(128) NULL,
    delivery_status VARCHAR(32) NULL,
    delivery_address TEXT NULL,
    delivery_contact_name VARCHAR(128) NULL,
    delivery_contact_phone VARCHAR(64) NULL,
    channel_code VARCHAR(64) NULL,
    source_active_order_id VARCHAR(64) NULL,
    order_no VARCHAR(64) NOT NULL,
    fulfillment_status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    settlement_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
    member_id BIGINT NULL,
    original_amount_cents BIGINT NOT NULL DEFAULT 0,
    member_discount_cents BIGINT NOT NULL DEFAULT 0,
    promotion_discount_cents BIGINT NOT NULL DEFAULT 0,
    payable_amount_cents BIGINT NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMP NULL,
    actor_type VARCHAR(32) DEFAULT 'HUMAN',
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) DEFAULT 'MANUAL',
    change_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_submitted_orders_id UNIQUE (submitted_order_id),
    CONSTRAINT uk_submitted_orders_no UNIQUE (order_no),
    CONSTRAINT fk_submitted_orders_session FOREIGN KEY (table_session_id) REFERENCES table_sessions(id),
    CONSTRAINT fk_submitted_orders_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_submitted_orders_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_submitted_orders_table FOREIGN KEY (table_id) REFERENCES store_tables(id),
    INDEX idx_submitted_orders_lookup (store_id, table_id, settlement_status)
);
```

#### submitted_order_items (Flyway V012)

```sql
CREATE TABLE submitted_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    submitted_order_db_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_name_snapshot VARCHAR(255) NOT NULL,
    sku_code_snapshot VARCHAR(64) NOT NULL,
    unit_price_snapshot_cents BIGINT NOT NULL,
    quantity INT NOT NULL,
    item_remark VARCHAR(255) NULL,
    option_snapshot_json JSON NULL,
    line_total_cents BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_submitted_order_items_order FOREIGN KEY (submitted_order_db_id) REFERENCES submitted_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_submitted_order_items_sku FOREIGN KEY (sku_id) REFERENCES skus(id)
);
```

---

### 2.6 Kitchen KDS Module

#### kitchen_stations (doc/66)

```sql
CREATE TABLE kitchen_stations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    station_code VARCHAR(64) NOT NULL,
    station_name VARCHAR(128) NOT NULL,
    station_type VARCHAR(32) DEFAULT 'KITCHEN',
    printer_ip VARCHAR(64) NULL,
    kds_display_id VARCHAR(64) NULL,
    station_status VARCHAR(32) DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_station_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_station UNIQUE (store_id, station_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### kitchen_tickets (doc/66)

```sql
CREATE TABLE kitchen_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    table_code VARCHAR(64) NOT NULL,
    station_id BIGINT NOT NULL,
    submitted_order_id BIGINT NOT NULL,
    round_number INT DEFAULT 1,
    ticket_status VARCHAR(32) DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    ready_at TIMESTAMP NULL,
    served_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kt_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_kt_station FOREIGN KEY (station_id) REFERENCES kitchen_stations(id),
    CONSTRAINT fk_kt_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    CONSTRAINT uk_kt UNIQUE (ticket_no),
    INDEX idx_kt_station_status (station_id, ticket_status),
    INDEX idx_kt_store_status (store_id, ticket_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### kitchen_ticket_items (doc/66)

```sql
CREATE TABLE kitchen_ticket_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_name_snapshot VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    item_remark VARCHAR(255) NULL,
    option_snapshot_json JSON NULL,
    CONSTRAINT fk_kti_ticket FOREIGN KEY (ticket_id) REFERENCES kitchen_tickets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.7 Settlement Module

#### settlement_records (Flyway V008 + V019 + V030 + V050)

```sql
CREATE TABLE settlement_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    settlement_no VARCHAR(64) NOT NULL,
    active_order_id VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    cashier_id BIGINT NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    final_status VARCHAR(32) NOT NULL,
    refund_status VARCHAR(32) NULL,
    payable_amount_cents BIGINT NOT NULL,
    collected_amount_cents BIGINT NOT NULL,
    refunded_amount_cents BIGINT NOT NULL DEFAULT 0,
    pricing_snapshot_json JSON NULL,
    settlement_snapshot_json JSON NULL,
    actor_type VARCHAR(32) DEFAULT 'HUMAN',
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) DEFAULT 'MANUAL',
    change_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_settlement_records_no UNIQUE (settlement_no),
    CONSTRAINT uk_settlement_records_active_order UNIQUE (active_order_id)
);
```

#### payment_attempts (Flyway V015)

```sql
CREATE TABLE payment_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_attempt_id VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    payment_scheme VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    table_session_id BIGINT NOT NULL,
    session_ref VARCHAR(64) NOT NULL,
    settlement_amount_cents BIGINT NOT NULL,
    currency_code VARCHAR(8) NOT NULL,
    provider_payment_id VARCHAR(128) NULL,
    provider_checkout_url VARCHAR(512) NULL,
    provider_status VARCHAR(64) NULL,
    attempt_status VARCHAR(32) NOT NULL,
    webhook_event_type VARCHAR(128) NULL,
    last_webhook_payload_json LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    UNIQUE KEY uk_payment_attempts_payment_attempt_id (payment_attempt_id),
    KEY idx_payment_attempts_provider_payment_id (provider, provider_payment_id),
    KEY idx_payment_attempts_table (store_id, table_id, attempt_status)
);
```

#### refund_records (Flyway V050)

```sql
CREATE TABLE refund_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    settlement_id BIGINT NOT NULL,
    settlement_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    refund_amount_cents BIGINT NOT NULL,
    refund_type VARCHAR(32) NOT NULL DEFAULT 'FULL',
    refund_reason TEXT,
    refund_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    payment_method VARCHAR(32) NOT NULL,
    operated_by BIGINT NULL,
    approved_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refund_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_records(id),
    INDEX idx_refund_store (store_id, created_at),
    INDEX idx_refund_settlement (settlement_id)
);
```

---

### 2.8 Inventory Module

#### inventory_items (doc/66 + doc/70 ALTER)

```sql
CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NULL,
    unit VARCHAR(32) NOT NULL,
    purchase_unit VARCHAR(32) NULL,
    purchase_to_stock_ratio DECIMAL(10,4) DEFAULT 1.0,
    usage_unit VARCHAR(32) NULL,
    stock_to_usage_ratio DECIMAL(10,4) DEFAULT 1.0,
    stock_unit VARCHAR(32) NULL,
    unit_conversion_factor DECIMAL(10,4) DEFAULT 1.0,
    current_stock DECIMAL(14,4) DEFAULT 0,
    safety_stock DECIMAL(14,4) DEFAULT 0,
    expiry_warning_days INT DEFAULT 3,
    shelf_life_days INT NULL,
    requires_batch_tracking BOOLEAN DEFAULT FALSE,
    item_status VARCHAR(32) DEFAULT 'ACTIVE',
    default_supplier_id BIGINT NULL,
    last_purchase_price_cents BIGINT NULL,
    avg_daily_usage DECIMAL(14,4) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_inv UNIQUE (store_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### inventory_batches (doc/70)

```sql
CREATE TABLE inventory_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_ref VARCHAR(128) NULL,
    supplier_id BIGINT NULL,
    received_qty DECIMAL(14,4) NOT NULL,
    remaining_qty DECIMAL(14,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    unit_cost_cents BIGINT NULL,
    total_cost_cents BIGINT NULL,
    production_date DATE NULL,
    expiry_date DATE NULL,
    received_date DATE NOT NULL,
    batch_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ib_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ib_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_ib_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT uk_ib UNIQUE (store_id, batch_no),
    INDEX idx_ib_item_expiry (inventory_item_id, expiry_date),
    INDEX idx_ib_status (store_id, batch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### recipes (doc/66)

```sql
CREATE TABLE recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    consumption_qty DECIMAL(10,4) NOT NULL,
    consumption_unit VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipe_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT fk_recipe_inv FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT uk_recipe UNIQUE (sku_id, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### purchase_invoices (doc/66)

```sql
CREATE TABLE purchase_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    invoice_no VARCHAR(128) NOT NULL,
    supplier_id BIGINT NULL,
    supplier_name VARCHAR(255) NULL,
    invoice_date DATE NOT NULL,
    total_amount_cents BIGINT DEFAULT 0,
    invoice_status VARCHAR(32) DEFAULT 'PENDING',
    scan_image_url VARCHAR(512) NULL,
    ocr_status VARCHAR(32) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pi_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_pi UNIQUE (store_id, invoice_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### purchase_invoice_items (doc/66)

```sql
CREATE TABLE purchase_invoice_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    quantity DECIMAL(10,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    line_total_cents BIGINT NOT NULL,
    CONSTRAINT fk_pii_invoice FOREIGN KEY (invoice_id) REFERENCES purchase_invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_pii_inv FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### purchase_orders (doc/70)

```sql
CREATE TABLE purchase_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    po_no VARCHAR(64) NOT NULL,
    supplier_id BIGINT NOT NULL,
    total_amount_cents BIGINT DEFAULT 0,
    currency_code VARCHAR(16) DEFAULT 'SGD',
    po_status VARCHAR(32) DEFAULT 'DRAFT',
    submitted_at TIMESTAMP NULL,
    confirmed_at TIMESTAMP NULL,
    received_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    created_by BIGINT NULL,
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    expected_delivery_date DATE NULL,
    actual_delivery_date DATE NULL,
    delivery_notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_po_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT uk_po UNIQUE (store_id, po_no),
    INDEX idx_po_status (store_id, po_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### purchase_order_items (doc/70)

```sql
CREATE TABLE purchase_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    ordered_qty DECIMAL(10,4) NOT NULL,
    ordered_unit VARCHAR(32) NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    line_total_cents BIGINT NOT NULL,
    received_qty DECIMAL(10,4) DEFAULT 0,
    item_status VARCHAR(32) DEFAULT 'PENDING',
    CONSTRAINT fk_poi_po FOREIGN KEY (po_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_poi_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### inventory_movements (doc/66 + doc/70 ALTER)

```sql
CREATE TABLE inventory_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    batch_id BIGINT NULL,
    movement_type VARCHAR(32) NOT NULL,
    quantity_change DECIMAL(14,4) NOT NULL,
    unit_cost_cents BIGINT NULL,
    balance_after DECIMAL(14,4) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_ref VARCHAR(128) NULL,
    notes VARCHAR(512) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_im_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_im_inv FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    INDEX idx_im_item (inventory_item_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### suppliers (doc/66 + doc/70 ALTER)

```sql
CREATE TABLE suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    supplier_code VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(128) NULL,
    contact_phone VARCHAR(64) NULL,
    email VARCHAR(255) NULL,
    address TEXT NULL,
    payment_terms VARCHAR(64) NULL,
    lead_time_days INT DEFAULT 1,
    rating DECIMAL(2,1) NULL,
    notes TEXT NULL,
    supplier_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_supplier_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_supplier UNIQUE (merchant_id, supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### supplier_price_history (doc/70)

```sql
CREATE TABLE supplier_price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    unit VARCHAR(32) NOT NULL,
    price_cents BIGINT NOT NULL,
    effective_date DATE NOT NULL,
    source_type VARCHAR(32) DEFAULT 'INVOICE',
    source_ref VARCHAR(128) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sph_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_sph_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    INDEX idx_sph_item_date (inventory_item_id, effective_date DESC),
    INDEX idx_sph_supplier_item (supplier_id, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### order_suggestions (doc/66 + doc/70 ALTER)

```sql
CREATE TABLE order_suggestions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    suggestion_date DATE NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    supplier_id BIGINT NULL,
    current_stock DECIMAL(14,4) NOT NULL,
    avg_daily_usage DECIMAL(14,4) NOT NULL,
    suggested_qty DECIMAL(14,4) NOT NULL,
    estimated_cost_cents BIGINT NULL,
    suggestion_status VARCHAR(32) DEFAULT 'PENDING',
    approved_qty DECIMAL(14,4) NULL,
    approved_by VARCHAR(128) NULL,
    purchase_order_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_os_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_os_inv FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT uk_os UNIQUE (store_id, suggestion_date, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### stocktake_tasks (doc/70)

```sql
CREATE TABLE stocktake_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    stocktake_type VARCHAR(32) NOT NULL DEFAULT 'FULL',
    stocktake_date DATE NOT NULL,
    task_status VARCHAR(32) DEFAULT 'PENDING',
    total_items INT DEFAULT 0,
    counted_items INT DEFAULT 0,
    variance_items INT DEFAULT 0,
    total_variance_cost_cents BIGINT DEFAULT 0,
    created_by BIGINT NULL,
    completed_by BIGINT NULL,
    approved_by BIGINT NULL,
    completed_at TIMESTAMP NULL,
    approved_at TIMESTAMP NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stt_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_stt UNIQUE (store_id, task_no),
    INDEX idx_stt_date (store_id, stocktake_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### stocktake_items (doc/70)

```sql
CREATE TABLE stocktake_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    system_qty DECIMAL(14,4) NOT NULL,
    counted_qty DECIMAL(14,4) NULL,
    variance_qty DECIMAL(14,4) NULL,
    unit VARCHAR(32) NOT NULL,
    unit_cost_cents BIGINT NULL,
    variance_cost_cents BIGINT NULL,
    variance_reason VARCHAR(32) NULL,
    notes VARCHAR(255) NULL,
    counted_at TIMESTAMP NULL,
    counted_by BIGINT NULL,
    CONSTRAINT fk_sti_task FOREIGN KEY (task_id) REFERENCES stocktake_tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_sti_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT uk_sti UNIQUE (task_id, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### waste_records (doc/70)

```sql
CREATE TABLE waste_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    waste_no VARCHAR(64) NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    batch_id BIGINT NULL,
    waste_qty DECIMAL(14,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    unit_cost_cents BIGINT NULL,
    total_cost_cents BIGINT NULL,
    waste_reason VARCHAR(32) NOT NULL,
    waste_detail VARCHAR(255) NULL,
    recorded_by BIGINT NULL,
    approved_by BIGINT NULL,
    waste_status VARCHAR(32) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_wr_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_wr_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches(id),
    CONSTRAINT uk_wr UNIQUE (store_id, waste_no),
    INDEX idx_wr_date (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### inventory_transfers (doc/70)

```sql
CREATE TABLE inventory_transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_no VARCHAR(64) NOT NULL,
    from_store_id BIGINT NOT NULL,
    to_store_id BIGINT NOT NULL,
    transfer_status VARCHAR(32) DEFAULT 'PENDING',
    requested_by BIGINT NULL,
    approved_by BIGINT NULL,
    shipped_at TIMESTAMP NULL,
    received_at TIMESTAMP NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_it_from FOREIGN KEY (from_store_id) REFERENCES stores(id),
    CONSTRAINT fk_it_to FOREIGN KEY (to_store_id) REFERENCES stores(id),
    CONSTRAINT uk_it UNIQUE (transfer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### inventory_transfer_items (doc/70)

```sql
CREATE TABLE inventory_transfer_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    batch_id BIGINT NULL,
    transfer_qty DECIMAL(14,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    received_qty DECIMAL(14,4) NULL,
    CONSTRAINT fk_iti_transfer FOREIGN KEY (transfer_id) REFERENCES inventory_transfers(id) ON DELETE CASCADE,
    CONSTRAINT fk_iti_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_iti_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.9 Members / CRM Module

#### members (Flyway V009 + V030 + doc/71 ALTER)

```sql
CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    member_no VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    phone VARCHAR(64) NOT NULL,
    tier_code VARCHAR(64) NOT NULL DEFAULT 'STANDARD',
    member_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    referral_code VARCHAR(32) NULL,
    referred_by_member_id BIGINT NULL,
    referral_count INT DEFAULT 0,
    last_visit_at TIMESTAMP NULL,
    total_visit_count INT DEFAULT 0,
    avg_spend_cents BIGINT DEFAULT 0,
    preferred_dining_mode VARCHAR(32) NULL,
    birthday DATE NULL,
    anniversary DATE NULL,
    language VARCHAR(16) DEFAULT 'zh',
    communication_preference VARCHAR(32) DEFAULT 'WHATSAPP',
    actor_type VARCHAR(32) DEFAULT 'HUMAN',
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) DEFAULT 'MANUAL',
    change_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_members_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_members_no UNIQUE (member_no),
    CONSTRAINT uk_members_phone UNIQUE (phone)
);
```

#### member_accounts (Flyway V009 + doc/71 + doc/72 ALTER)

```sql
CREATE TABLE member_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    points_balance BIGINT NOT NULL DEFAULT 0,
    available_points BIGINT DEFAULT 0,
    frozen_points BIGINT DEFAULT 0,
    expired_points BIGINT DEFAULT 0,
    cash_balance_cents BIGINT NOT NULL DEFAULT 0,
    available_cash_cents BIGINT DEFAULT 0,
    frozen_cash_cents BIGINT DEFAULT 0,
    lifetime_spend_cents BIGINT NOT NULL DEFAULT 0,
    lifetime_recharge_cents BIGINT NOT NULL DEFAULT 0,
    total_points_earned BIGINT DEFAULT 0,
    total_points_redeemed BIGINT DEFAULT 0,
    total_coupons_used INT DEFAULT 0,
    current_tier_started_at TIMESTAMP NULL,
    next_tier_threshold_cents BIGINT NULL,
    last_activity_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_accounts_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT uk_member_accounts_member UNIQUE (member_id)
);
```

#### member_recharge_orders (Flyway V013)

```sql
CREATE TABLE member_recharge_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recharge_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    amount_cents BIGINT NOT NULL DEFAULT 0,
    bonus_amount_cents BIGINT NOT NULL DEFAULT 0,
    final_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    operator_name VARCHAR(128) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_recharge_orders_no UNIQUE (recharge_no),
    CONSTRAINT fk_member_recharge_orders_member FOREIGN KEY (member_id) REFERENCES members(id)
);
```

#### member_points_ledger (Flyway V013 + doc/72 ALTER)

```sql
CREATE TABLE member_points_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ledger_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    points_delta BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_ref VARCHAR(128) NULL,
    batch_id BIGINT NULL,
    rule_id BIGINT NULL,
    expires_at TIMESTAMP NULL,
    operator_name VARCHAR(128) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_points_ledger_no UNIQUE (ledger_no),
    CONSTRAINT fk_member_points_ledger_member FOREIGN KEY (member_id) REFERENCES members(id)
);
```

#### member_cash_ledger (doc/72)

```sql
CREATE TABLE member_cash_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    balance_after_cents BIGINT NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_ref VARCHAR(128) NULL,
    operator_type VARCHAR(32) DEFAULT 'SYSTEM',
    operator_id VARCHAR(64) NULL,
    notes VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mcl_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_mcl UNIQUE (ledger_no),
    INDEX idx_mcl_member (member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### member_tier_rules (doc/71)

```sql
CREATE TABLE member_tier_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    tier_code VARCHAR(32) NOT NULL,
    tier_name VARCHAR(64) NOT NULL,
    tier_level INT NOT NULL,
    upgrade_type VARCHAR(32) NOT NULL DEFAULT 'LIFETIME_SPEND',
    upgrade_threshold_cents BIGINT NOT NULL,
    downgrade_enabled BOOLEAN DEFAULT FALSE,
    downgrade_period_months INT DEFAULT 12,
    downgrade_threshold_cents BIGINT NULL,
    points_multiplier DECIMAL(3,2) DEFAULT 1.00,
    discount_percent INT DEFAULT 0,
    birthday_bonus_points BIGINT DEFAULT 0,
    free_delivery BOOLEAN DEFAULT FALSE,
    tier_icon VARCHAR(64) NULL,
    tier_color VARCHAR(7) NULL,
    tier_description VARCHAR(255) NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mtr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_mtr UNIQUE (merchant_id, tier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### coupon_templates (doc/71)

```sql
CREATE TABLE coupon_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,
    discount_amount_cents BIGINT NULL,
    discount_percent INT NULL,
    min_spend_cents BIGINT DEFAULT 0,
    max_discount_cents BIGINT NULL,
    applicable_stores JSON NULL,
    applicable_categories JSON NULL,
    applicable_skus JSON NULL,
    applicable_dining_modes JSON DEFAULT '["A_LA_CARTE","BUFFET","DELIVERY"]',
    total_quantity INT NULL,
    issued_count INT DEFAULT 0,
    per_member_limit INT DEFAULT 1,
    validity_type VARCHAR(32) NOT NULL DEFAULT 'FIXED',
    valid_from TIMESTAMP NULL,
    valid_until TIMESTAMP NULL,
    validity_days INT NULL,
    template_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ct_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_ct UNIQUE (merchant_id, template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### member_coupons (doc/71)

```sql
CREATE TABLE member_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_no VARCHAR(64) NOT NULL,
    member_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_ref VARCHAR(128) NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    coupon_status VARCHAR(32) DEFAULT 'AVAILABLE',
    used_at TIMESTAMP NULL,
    used_order_id VARCHAR(64) NULL,
    used_store_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mc_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_mc_template FOREIGN KEY (template_id) REFERENCES coupon_templates(id),
    CONSTRAINT uk_mc UNIQUE (coupon_no),
    INDEX idx_mc_member_status (member_id, coupon_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### recharge_campaigns (doc/71)

```sql
CREATE TABLE recharge_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,
    campaign_name VARCHAR(128) NOT NULL,
    recharge_amount_cents BIGINT NOT NULL,
    bonus_amount_cents BIGINT DEFAULT 0,
    bonus_points BIGINT DEFAULT 0,
    bonus_coupon_template_id BIGINT NULL,
    min_tier_level INT DEFAULT 0,
    max_per_member INT DEFAULT 0,
    total_quota INT DEFAULT 0,
    used_quota INT DEFAULT 0,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    campaign_status VARCHAR(32) DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rc_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_rc_coupon FOREIGN KEY (bonus_coupon_template_id) REFERENCES coupon_templates(id),
    CONSTRAINT uk_rc UNIQUE (merchant_id, campaign_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### points_rules (doc/72)

```sql
CREATE TABLE points_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    points_per_dollar INT DEFAULT 1,
    bonus_multiplier DECIMAL(3,2) DEFAULT 1.00,
    fixed_points BIGINT NULL,
    min_spend_cents BIGINT DEFAULT 0,
    max_points_per_order BIGINT NULL,
    max_points_per_day BIGINT NULL,
    applicable_tiers JSON NULL,
    applicable_stores JSON NULL,
    applicable_categories JSON NULL,
    applicable_dining_modes JSON NULL,
    applicable_days JSON NULL,
    applicable_time_slots JSON NULL,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    rule_status VARCHAR(32) DEFAULT 'ACTIVE',
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_pr UNIQUE (merchant_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### points_expiry_rules (doc/72)

```sql
CREATE TABLE points_expiry_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    expiry_type VARCHAR(32) NOT NULL DEFAULT 'ROLLING',
    expiry_months INT DEFAULT 12,
    year_end_clear BOOLEAN DEFAULT FALSE,
    year_end_clear_month INT DEFAULT 12,
    year_end_clear_day INT DEFAULT 31,
    grace_period_days INT DEFAULT 30,
    notify_before_days INT DEFAULT 7,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_per_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_per UNIQUE (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### points_batches (doc/72)

```sql
CREATE TABLE points_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_ref VARCHAR(128) NULL,
    rule_id BIGINT NULL,
    original_points BIGINT NOT NULL,
    remaining_points BIGINT NOT NULL,
    used_points BIGINT DEFAULT 0,
    expired_points BIGINT DEFAULT 0,
    earned_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    expired_at TIMESTAMP NULL,
    batch_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pb_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_pb UNIQUE (batch_no),
    INDEX idx_pb_member_expiry (member_id, expires_at, batch_status),
    INDEX idx_pb_expiry (expires_at, batch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### points_deduction_rules (doc/72)

```sql
CREATE TABLE points_deduction_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    points_per_cent INT NOT NULL DEFAULT 10,
    max_deduction_percent INT DEFAULT 50,
    min_points_to_deduct BIGINT DEFAULT 100,
    min_order_cents BIGINT DEFAULT 0,
    applicable_stores JSON NULL,
    applicable_dining_modes JSON NULL,
    excluded_categories JSON NULL,
    excluded_skus JSON NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pdr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_pdr UNIQUE (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### cash_balance_rules (doc/72)

```sql
CREATE TABLE cash_balance_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    min_balance_to_use_cents BIGINT DEFAULT 0,
    max_payment_percent INT DEFAULT 100,
    allow_partial_payment BOOLEAN DEFAULT TRUE,
    applicable_stores JSON NULL,
    applicable_dining_modes JSON NULL,
    allow_withdrawal BOOLEAN DEFAULT FALSE,
    withdrawal_fee_percent INT DEFAULT 0,
    min_withdrawal_cents BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cbr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_cbr UNIQUE (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### points_redemption_items (doc/71)

```sql
CREATE TABLE points_redemption_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    points_required BIGINT NOT NULL,
    reward_sku_id BIGINT NULL,
    reward_coupon_template_id BIGINT NULL,
    reward_description VARCHAR(255) NULL,
    total_stock INT NULL,
    redeemed_count INT DEFAULT 0,
    per_member_limit INT DEFAULT 0,
    min_tier_level INT DEFAULT 0,
    item_status VARCHAR(32) DEFAULT 'ACTIVE',
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pri_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_pri_sku FOREIGN KEY (reward_sku_id) REFERENCES skus(id),
    CONSTRAINT fk_pri_coupon FOREIGN KEY (reward_coupon_template_id) REFERENCES coupon_templates(id),
    CONSTRAINT uk_pri UNIQUE (merchant_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### points_redemption_records (doc/71)

```sql
CREATE TABLE points_redemption_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    redemption_no VARCHAR(64) NOT NULL,
    member_id BIGINT NOT NULL,
    redemption_item_id BIGINT NOT NULL,
    points_spent BIGINT NOT NULL,
    reward_type VARCHAR(32) NOT NULL,
    reward_ref VARCHAR(128) NULL,
    redemption_status VARCHAR(32) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prr_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_prr_item FOREIGN KEY (redemption_item_id) REFERENCES points_redemption_items(id),
    CONSTRAINT uk_prr UNIQUE (redemption_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### referral_rewards_config (doc/71)

```sql
CREATE TABLE referral_rewards_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    referrer_reward_type VARCHAR(32) NOT NULL,
    referrer_reward_points BIGINT DEFAULT 0,
    referrer_reward_coupon_template_id BIGINT NULL,
    referrer_reward_cash_cents BIGINT DEFAULT 0,
    referee_reward_type VARCHAR(32) NOT NULL,
    referee_reward_points BIGINT DEFAULT 0,
    referee_reward_coupon_template_id BIGINT NULL,
    referee_reward_cash_cents BIGINT DEFAULT 0,
    trigger_event VARCHAR(32) DEFAULT 'FIRST_ORDER',
    min_spend_cents BIGINT DEFAULT 0,
    max_referrals_per_member INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rrc_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_rrc UNIQUE (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### referral_records (doc/71)

```sql
CREATE TABLE referral_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    referrer_member_id BIGINT NOT NULL,
    referee_member_id BIGINT NOT NULL,
    referral_code VARCHAR(32) NOT NULL,
    referral_status VARCHAR(32) DEFAULT 'PENDING',
    referrer_rewarded BOOLEAN DEFAULT FALSE,
    referrer_reward_ref VARCHAR(128) NULL,
    referee_rewarded BOOLEAN DEFAULT FALSE,
    referee_reward_ref VARCHAR(128) NULL,
    trigger_order_id VARCHAR(64) NULL,
    triggered_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rr_referrer FOREIGN KEY (referrer_member_id) REFERENCES members(id),
    CONSTRAINT fk_rr_referee FOREIGN KEY (referee_member_id) REFERENCES members(id),
    CONSTRAINT uk_rr_referee UNIQUE (referee_member_id),
    INDEX idx_rr_referrer (referrer_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### member_tags (doc/71)

```sql
CREATE TABLE member_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    tag_code VARCHAR(64) NOT NULL,
    tag_name VARCHAR(128) NOT NULL,
    tag_type VARCHAR(32) NOT NULL,
    tag_color VARCHAR(7) DEFAULT '#666666',
    auto_rule_json JSON NULL,
    is_auto BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mt_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_mt UNIQUE (merchant_id, tag_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### member_tag_assignments (doc/71)

```sql
CREATE TABLE member_tag_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(32) DEFAULT 'SYSTEM',
    expires_at TIMESTAMP NULL,
    CONSTRAINT fk_mta_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_mta_tag FOREIGN KEY (tag_id) REFERENCES member_tags(id),
    CONSTRAINT uk_mta UNIQUE (member_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### member_consumption_profiles (doc/71)

```sql
CREATE TABLE member_consumption_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    profile_period VARCHAR(16) NOT NULL,
    order_count INT DEFAULT 0,
    total_spend_cents BIGINT DEFAULT 0,
    avg_spend_cents BIGINT DEFAULT 0,
    max_spend_cents BIGINT DEFAULT 0,
    preferred_time_slot VARCHAR(32) NULL,
    preferred_day_of_week VARCHAR(16) NULL,
    preferred_dining_mode VARCHAR(32) NULL,
    dine_in_count INT DEFAULT 0,
    delivery_count INT DEFAULT 0,
    buffet_count INT DEFAULT 0,
    top_categories_json JSON NULL,
    top_skus_json JSON NULL,
    preferred_store_id BIGINT NULL,
    preferred_payment_method VARCHAR(32) NULL,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mcp_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_mcp UNIQUE (member_id, profile_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### marketing_campaigns (doc/71)

```sql
CREATE TABLE marketing_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,
    campaign_name VARCHAR(128) NOT NULL,
    campaign_type VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_tag_ids JSON NULL,
    target_tier_codes JSON NULL,
    target_filter_json JSON NULL,
    estimated_reach INT DEFAULT 0,
    message_template TEXT NOT NULL,
    message_channel VARCHAR(32) NOT NULL,
    coupon_template_id BIGINT NULL,
    scheduled_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    total_sent INT DEFAULT 0,
    total_delivered INT DEFAULT 0,
    total_opened INT DEFAULT 0,
    total_converted INT DEFAULT 0,
    campaign_status VARCHAR(32) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mktc_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_mktc_coupon FOREIGN KEY (coupon_template_id) REFERENCES coupon_templates(id),
    CONSTRAINT uk_mktc UNIQUE (merchant_id, campaign_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### marketing_send_records (doc/71)

```sql
CREATE TABLE marketing_send_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(128) NOT NULL,
    send_status VARCHAR(32) DEFAULT 'PENDING',
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    opened_at TIMESTAMP NULL,
    converted_at TIMESTAMP NULL,
    converted_order_id VARCHAR(64) NULL,
    external_message_id VARCHAR(128) NULL,
    error_message VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_msr_campaign FOREIGN KEY (campaign_id) REFERENCES marketing_campaigns(id),
    CONSTRAINT fk_msr_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_msr UNIQUE (campaign_id, member_id),
    INDEX idx_msr_member (member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.10 Auth / RBAC Module

#### users (doc/68 — replaces auth_users + staff)

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    phone VARCHAR(64) NULL,
    email VARCHAR(255) NULL,
    username VARCHAR(64) NULL,
    password_hash VARCHAR(255) NULL,
    must_change_password BOOLEAN DEFAULT TRUE,
    pin_hash VARCHAR(255) NULL,
    user_status VARCHAR(32) DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP NULL,
    failed_login_count INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_users_code UNIQUE (merchant_id, user_code),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### permissions (doc/68)

```sql
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(64) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    permission_group VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    risk_level VARCHAR(16) DEFAULT 'LOW',
    CONSTRAINT uk_perm_code UNIQUE (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### custom_roles (doc/68)

```sql
CREATE TABLE custom_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    role_description VARCHAR(255) NULL,
    is_system BOOLEAN DEFAULT FALSE,
    is_editable BOOLEAN DEFAULT TRUE,
    role_level VARCHAR(32) NOT NULL DEFAULT 'STORE',
    max_refund_cents BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_role UNIQUE (merchant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### custom_role_permissions (doc/68)

```sql
CREATE TABLE custom_role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    CONSTRAINT fk_crp_role FOREIGN KEY (role_id) REFERENCES custom_roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_crp UNIQUE (role_id, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### user_roles (doc/68)

```sql
CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT NULL,
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES custom_roles(id),
    CONSTRAINT uk_ur UNIQUE (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### user_store_access (doc/68)

```sql
CREATE TABLE user_store_access (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    access_level VARCHAR(32) DEFAULT 'FULL',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT NULL,
    CONSTRAINT fk_usa_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_usa_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_usa UNIQUE (user_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.11 Employees Module

#### employees (doc/69)

```sql
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    employee_no VARCHAR(64) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    id_type VARCHAR(32) NULL,
    id_number VARCHAR(64) NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(16) NULL,
    nationality VARCHAR(64) NULL,
    phone VARCHAR(64) NULL,
    email VARCHAR(255) NULL,
    emergency_contact_name VARCHAR(128) NULL,
    emergency_contact_phone VARCHAR(64) NULL,
    address TEXT NULL,
    employment_type VARCHAR(32) NOT NULL DEFAULT 'FULL_TIME',
    hire_date DATE NOT NULL,
    probation_end_date DATE NULL,
    termination_date DATE NULL,
    termination_reason VARCHAR(255) NULL,
    primary_store_id BIGINT NULL,
    department VARCHAR(64) NULL,
    position VARCHAR(64) NULL,
    salary_type VARCHAR(32) NOT NULL DEFAULT 'MONTHLY',
    base_salary_cents BIGINT DEFAULT 0,
    hourly_rate_cents BIGINT DEFAULT 0,
    overtime_rate_multiplier DECIMAL(3,2) DEFAULT 1.50,
    employee_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_emp_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_emp_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_emp_store FOREIGN KEY (primary_store_id) REFERENCES stores(id),
    CONSTRAINT uk_emp_no UNIQUE (merchant_id, employee_no),
    CONSTRAINT uk_emp_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### shift_templates (doc/69)

```sql
CREATE TABLE shift_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_minutes INT DEFAULT 0,
    color_hex VARCHAR(7) DEFAULT '#4A90D9',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_st_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_st UNIQUE (merchant_id, template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### employee_schedules (doc/69)

```sql
CREATE TABLE employee_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    shift_template_id BIGINT NULL,
    scheduled_start TIME NOT NULL,
    scheduled_end TIME NOT NULL,
    break_minutes INT DEFAULT 0,
    schedule_type VARCHAR(32) DEFAULT 'NORMAL',
    schedule_status VARCHAR(32) DEFAULT 'SCHEDULED',
    swap_requested_by BIGINT NULL,
    swap_approved_by BIGINT NULL,
    original_employee_id BIGINT NULL,
    notes VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_es_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_es_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_es_template FOREIGN KEY (shift_template_id) REFERENCES shift_templates(id),
    CONSTRAINT uk_es UNIQUE (employee_id, store_id, schedule_date),
    INDEX idx_es_store_date (store_id, schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### attendance_records (doc/69)

```sql
CREATE TABLE attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    schedule_id BIGINT NULL,
    attendance_date DATE NOT NULL,
    clock_in_at TIMESTAMP NULL,
    clock_out_at TIMESTAMP NULL,
    clock_in_method VARCHAR(32) NULL,
    clock_out_method VARCHAR(32) NULL,
    scheduled_minutes INT DEFAULT 0,
    actual_minutes INT DEFAULT 0,
    overtime_minutes INT DEFAULT 0,
    break_minutes INT DEFAULT 0,
    late_minutes INT DEFAULT 0,
    early_leave_minutes INT DEFAULT 0,
    attendance_status VARCHAR(32) DEFAULT 'PENDING',
    exception_type VARCHAR(32) NULL,
    exception_reason VARCHAR(255) NULL,
    approved_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ar_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_ar_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ar_schedule FOREIGN KEY (schedule_id) REFERENCES employee_schedules(id),
    CONSTRAINT uk_ar UNIQUE (employee_id, store_id, attendance_date),
    INDEX idx_ar_store_date (store_id, attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### leave_requests (doc/69)

```sql
CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_days DECIMAL(4,1) NOT NULL,
    reason VARCHAR(512) NULL,
    request_status VARCHAR(32) DEFAULT 'PENDING',
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    reject_reason VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lr_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    INDEX idx_lr_employee_date (employee_id, start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### leave_balances (doc/69)

```sql
CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(32) NOT NULL,
    year INT NOT NULL,
    entitled_days DECIMAL(4,1) DEFAULT 0,
    used_days DECIMAL(4,1) DEFAULT 0,
    carried_over_days DECIMAL(4,1) DEFAULT 0,
    CONSTRAINT fk_lb_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT uk_lb UNIQUE (employee_id, leave_type, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### payroll_periods (doc/69)

```sql
CREATE TABLE payroll_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    period_code VARCHAR(32) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    period_status VARCHAR(32) DEFAULT 'OPEN',
    closed_at TIMESTAMP NULL,
    closed_by BIGINT NULL,
    CONSTRAINT fk_pp_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_pp UNIQUE (merchant_id, period_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### payroll_records (doc/69)

```sql
CREATE TABLE payroll_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    scheduled_hours DECIMAL(6,2) DEFAULT 0,
    actual_hours DECIMAL(6,2) DEFAULT 0,
    overtime_hours DECIMAL(6,2) DEFAULT 0,
    base_pay_cents BIGINT DEFAULT 0,
    overtime_pay_cents BIGINT DEFAULT 0,
    allowance_cents BIGINT DEFAULT 0,
    deduction_cents BIGINT DEFAULT 0,
    bonus_cents BIGINT DEFAULT 0,
    gross_pay_cents BIGINT DEFAULT 0,
    cpf_employee_cents BIGINT DEFAULT 0,
    cpf_employer_cents BIGINT DEFAULT 0,
    tax_cents BIGINT DEFAULT 0,
    other_deduction_cents BIGINT DEFAULT 0,
    net_pay_cents BIGINT DEFAULT 0,
    revenue_during_period_cents BIGINT DEFAULT 0,
    revenue_per_hour_cents BIGINT DEFAULT 0,
    payroll_status VARCHAR(32) DEFAULT 'DRAFT',
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pr_period FOREIGN KEY (period_id) REFERENCES payroll_periods(id),
    CONSTRAINT fk_pr_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_pr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_pr UNIQUE (period_id, employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### employee_performance_log (doc/69)

```sql
CREATE TABLE employee_performance_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    event_date DATE NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    recorded_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_epl_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_epl_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_epl_employee (employee_id, event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.12 Shifts Module

#### cashier_shifts (Flyway V026 + doc/69 ALTER)

```sql
CREATE TABLE cashier_shifts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    cashier_staff_id VARCHAR(64) NOT NULL,
    employee_id BIGINT NULL,
    attendance_id BIGINT NULL,
    cashier_name VARCHAR(128) NOT NULL,
    shift_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    opening_cash_cents BIGINT NOT NULL DEFAULT 0,
    closing_cash_cents BIGINT NULL,
    expected_cash_cents BIGINT NULL,
    cash_difference_cents BIGINT NULL,
    total_sales_cents BIGINT NOT NULL DEFAULT 0,
    total_refunds_cents BIGINT NOT NULL DEFAULT 0,
    total_transaction_count INT NOT NULL DEFAULT 0,
    notes TEXT NULL
);
```

#### cashier_shift_settlements (Flyway V026)

```sql
CREATE TABLE cashier_shift_settlements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift_id BIGINT NOT NULL,
    settlement_no VARCHAR(64) NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    amount_cents BIGINT NOT NULL,
    settled_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_shift_settlement FOREIGN KEY (shift_id) REFERENCES cashier_shifts(id)
);
```

---

### 2.13 Reservations Module

#### reservations (Flyway V018)

```sql
CREATE TABLE reservations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NULL,
    guest_name VARCHAR(120) NOT NULL,
    reservation_time VARCHAR(16) NOT NULL,
    party_size INT NOT NULL,
    reservation_status VARCHAR(32) NOT NULL,
    area VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reservation_no (reservation_no),
    KEY idx_reservation_store_status (store_id, reservation_status),
    KEY idx_reservation_store_table (store_id, table_id)
);
```

---

### 2.14 Queue Module

#### queue_tickets (doc/66)

```sql
CREATE TABLE queue_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    ticket_no VARCHAR(64) NOT NULL,
    guest_phone VARCHAR(64) NULL,
    member_id BIGINT NULL,
    party_size INT NOT NULL DEFAULT 2,
    queue_position INT NOT NULL,
    ticket_status VARCHAR(32) DEFAULT 'WAITING',
    estimated_wait_minutes INT NULL,
    called_at TIMESTAMP NULL,
    seated_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    table_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_qt_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_qt UNIQUE (store_id, ticket_no),
    INDEX idx_qt_status (store_id, ticket_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.15 GTO Module

#### gto_export_batches (Flyway V020)

```sql
CREATE TABLE gto_export_batches (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    export_date DATE NOT NULL,
    batch_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_sales_cents BIGINT NOT NULL DEFAULT 0,
    total_tax_cents BIGINT NOT NULL DEFAULT 0,
    total_transaction_count INT NOT NULL DEFAULT 0,
    file_content_json JSON NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT uk_gto_batch_store_date UNIQUE (store_id, export_date)
);
```

#### gto_export_items (Flyway V020)

```sql
CREATE TABLE gto_export_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    payment_scheme VARCHAR(64) NULL,
    sale_count INT NOT NULL DEFAULT 0,
    sale_total_cents BIGINT NOT NULL DEFAULT 0,
    refund_count INT NOT NULL DEFAULT 0,
    refund_total_cents BIGINT NOT NULL DEFAULT 0,
    net_total_cents BIGINT NOT NULL DEFAULT 0,
    tax_cents BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gto_item_batch FOREIGN KEY (batch_id) REFERENCES gto_export_batches(id)
);
```

---

### 2.16 Promotions Module

#### promotion_rules (Flyway V010 + V030 + V035)

```sql
CREATE TABLE promotion_rules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(64) NOT NULL,
    rule_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    priority INT NOT NULL DEFAULT 100,
    usage_count INT NOT NULL DEFAULT 0,
    max_usage INT NULL,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    actor_type VARCHAR(32) DEFAULT 'HUMAN',
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) DEFAULT 'MANUAL',
    change_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_rules_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_promotion_rules_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_promotion_rules_code UNIQUE (rule_code)
);
```

#### promotion_rule_conditions (Flyway V010)

```sql
CREATE TABLE promotion_rule_conditions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    condition_type VARCHAR(64) NOT NULL,
    threshold_amount_cents BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_rule_conditions_rule
        FOREIGN KEY (rule_id) REFERENCES promotion_rules(id) ON DELETE CASCADE
);
```

#### promotion_rule_rewards (Flyway V010 + V035)

```sql
CREATE TABLE promotion_rule_rewards (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    reward_type VARCHAR(64) NOT NULL,
    discount_amount_cents BIGINT NULL,
    discount_percent INT NULL,
    gift_sku_id BIGINT NULL,
    gift_quantity INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_rule_rewards_rule
        FOREIGN KEY (rule_id) REFERENCES promotion_rules(id) ON DELETE CASCADE,
    CONSTRAINT fk_promotion_rule_rewards_gift_sku
        FOREIGN KEY (gift_sku_id) REFERENCES skus(id)
);
```

#### promotion_hits (Flyway V010)

```sql
CREATE TABLE promotion_hits (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    active_order_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    discount_amount_cents BIGINT NOT NULL DEFAULT 0,
    gift_snapshot_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_hits_active_order
        FOREIGN KEY (active_order_id) REFERENCES active_table_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_promotion_hits_rule
        FOREIGN KEY (rule_id) REFERENCES promotion_rules(id)
);
```

---

### 2.17 Recommendations Module

#### recommendation_slots (doc/66)

```sql
CREATE TABLE recommendation_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    slot_code VARCHAR(64) NOT NULL,
    slot_name VARCHAR(128) NOT NULL,
    slot_type VARCHAR(32) NOT NULL,
    position VARCHAR(64) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_rs UNIQUE (store_id, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### recommendation_slot_items (doc/66)

```sql
CREATE TABLE recommendation_slot_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slot_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    display_label VARCHAR(255) NULL,
    sort_order INT DEFAULT 0,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    CONSTRAINT fk_rsi_slot FOREIGN KEY (slot_id) REFERENCES recommendation_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_rsi_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_rsi UNIQUE (slot_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.18 Delivery Module

#### delivery_platform_configs (doc/66)

```sql
CREATE TABLE delivery_platform_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    platform_code VARCHAR(64) NOT NULL,
    platform_name VARCHAR(128) NOT NULL,
    api_key VARCHAR(512) NULL,
    api_secret VARCHAR(512) NULL,
    store_mapping_id VARCHAR(128) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    config_json JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dpc_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_dpc UNIQUE (store_id, platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### delivery_orders (doc/66)

```sql
CREATE TABLE delivery_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submitted_order_id BIGINT NOT NULL,
    platform_code VARCHAR(64) NOT NULL,
    platform_order_no VARCHAR(128) NOT NULL,
    platform_status VARCHAR(64) NULL,
    rider_name VARCHAR(128) NULL,
    rider_phone VARCHAR(64) NULL,
    estimated_pickup_at TIMESTAMP NULL,
    actual_pickup_at TIMESTAMP NULL,
    estimated_delivery_at TIMESTAMP NULL,
    actual_delivery_at TIMESTAMP NULL,
    delivery_fee_cents BIGINT DEFAULT 0,
    platform_commission_cents BIGINT DEFAULT 0,
    merchant_receivable_cents BIGINT DEFAULT 0,
    platform_status_snapshot JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_do_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    CONSTRAINT uk_do_platform UNIQUE (platform_code, platform_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.19 Channels Module

#### channels (doc/73)

```sql
CREATE TABLE channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    channel_code VARCHAR(64) NOT NULL,
    channel_name VARCHAR(128) NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    contact_name VARCHAR(128) NULL,
    contact_phone VARCHAR(64) NULL,
    contact_email VARCHAR(255) NULL,
    tracking_param VARCHAR(64) NULL,
    tracking_url_prefix VARCHAR(512) NULL,
    channel_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ch_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_ch UNIQUE (merchant_id, channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### channel_commission_rules (doc/73)

```sql
CREATE TABLE channel_commission_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    commission_type VARCHAR(32) NOT NULL,
    commission_rate_percent DECIMAL(5,2) NULL,
    commission_fixed_cents BIGINT NULL,
    tier_rules_json JSON NULL,
    calculation_base VARCHAR(32) DEFAULT 'NET_SALES',
    applicable_stores JSON NULL,
    applicable_categories JSON NULL,
    applicable_dining_modes JSON NULL,
    min_commission_cents BIGINT NULL,
    max_commission_cents BIGINT NULL,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    rule_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ccr_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT uk_ccr UNIQUE (channel_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### order_channel_attribution (doc/73)

```sql
CREATE TABLE order_channel_attribution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submitted_order_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    attribution_type VARCHAR(32) NOT NULL,
    tracking_value VARCHAR(255) NULL,
    landing_url VARCHAR(512) NULL,
    first_touch_at TIMESTAMP NULL,
    conversion_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_oca_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    CONSTRAINT fk_oca_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT uk_oca UNIQUE (submitted_order_id, channel_id),
    INDEX idx_oca_channel (channel_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### channel_commission_records (doc/73)

```sql
CREATE TABLE channel_commission_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commission_no VARCHAR(64) NOT NULL,
    channel_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    submitted_order_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    order_amount_cents BIGINT NOT NULL,
    calculation_base_cents BIGINT NOT NULL,
    commission_type VARCHAR(32) NOT NULL,
    commission_rate_percent DECIMAL(5,2) NULL,
    commission_fixed_cents BIGINT NULL,
    commission_amount_cents BIGINT NOT NULL,
    commission_status VARCHAR(32) DEFAULT 'PENDING',
    settlement_batch_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ccrd_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT fk_ccrd_rule FOREIGN KEY (rule_id) REFERENCES channel_commission_rules(id),
    CONSTRAINT fk_ccrd_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    CONSTRAINT uk_ccrd UNIQUE (commission_no),
    INDEX idx_ccrd_channel_status (channel_id, commission_status),
    INDEX idx_ccrd_store (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### channel_settlement_batches (doc/73)

```sql
CREATE TABLE channel_settlement_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL,
    channel_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_orders INT DEFAULT 0,
    total_order_amount_cents BIGINT DEFAULT 0,
    total_commission_cents BIGINT DEFAULT 0,
    adjustment_cents BIGINT DEFAULT 0,
    adjustment_reason VARCHAR(255) NULL,
    final_settlement_cents BIGINT DEFAULT 0,
    batch_status VARCHAR(32) DEFAULT 'DRAFT',
    confirmed_at TIMESTAMP NULL,
    confirmed_by BIGINT NULL,
    paid_at TIMESTAMP NULL,
    paid_by BIGINT NULL,
    payment_ref VARCHAR(128) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_csb_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT uk_csb UNIQUE (batch_no),
    INDEX idx_csb_channel_period (channel_id, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### channel_performance_daily (doc/73)

```sql
CREATE TABLE channel_performance_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    report_date DATE NOT NULL,
    impressions INT DEFAULT 0,
    clicks INT DEFAULT 0,
    unique_visitors INT DEFAULT 0,
    orders INT DEFAULT 0,
    new_customers INT DEFAULT 0,
    returning_customers INT DEFAULT 0,
    gross_sales_cents BIGINT DEFAULT 0,
    net_sales_cents BIGINT DEFAULT 0,
    commission_cents BIGINT DEFAULT 0,
    profit_after_commission_cents BIGINT DEFAULT 0,
    cost_per_order_cents BIGINT DEFAULT 0,
    customer_acquisition_cost_cents BIGINT DEFAULT 0,
    roi_percent DECIMAL(6,2) DEFAULT 0,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cpd_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT uk_cpd UNIQUE (channel_id, store_id, report_date),
    INDEX idx_cpd_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.20 AI Module

#### action_log (Flyway V031)

```sql
CREATE TABLE action_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_name VARCHAR(128) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    recommendation_id VARCHAR(64),
    approval_status VARCHAR(32),
    risk_level VARCHAR(16),
    params_json JSON,
    result_json JSON,
    change_reason TEXT,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_action_log_actor (actor_type, created_at),
    KEY idx_action_log_tool (tool_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### ai_proposal (Flyway V032)

```sql
CREATE TABLE ai_proposal (
    id VARCHAR(64) PRIMARY KEY,
    advisor_role VARCHAR(32) NOT NULL,
    proposal_type VARCHAR(64) NOT NULL,
    target_domain VARCHAR(32) NOT NULL,
    target_tool VARCHAR(64) NOT NULL,
    params_json JSON,
    reason TEXT,
    risk_level VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6),
    reviewed_by VARCHAR(64),
    reviewed_at DATETIME(6),
    executed_at DATETIME(6),
    KEY idx_proposal_status (status, created_at),
    KEY idx_proposal_role (advisor_role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### ai_recommendations (Flyway V040)

```sql
CREATE TABLE ai_recommendations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recommendation_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    advisor_role VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    detail_json JSON NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    proposed_action VARCHAR(64) NULL,
    proposed_params_json JSON NULL,
    approved_by VARCHAR(64) NULL,
    approved_at TIMESTAMP NULL,
    rejected_reason TEXT NULL,
    executed_at TIMESTAMP NULL,
    execution_result_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    INDEX idx_rec_store_status (store_id, status),
    INDEX idx_rec_store_role (store_id, advisor_role)
);
```

#### ai_scheduled_checks (Flyway V040)

```sql
CREATE TABLE ai_scheduled_checks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    advisor_role VARCHAR(32) NOT NULL,
    check_type VARCHAR(64) NOT NULL,
    cron_expression VARCHAR(64) NOT NULL DEFAULT '0 8 * * *',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMP NULL,
    CONSTRAINT uk_ai_check UNIQUE (store_id, advisor_role, check_type)
);
```

#### restaurant_agents (Flyway V045)

```sql
CREATE TABLE restaurant_agents (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    agent_name VARCHAR(255) NOT NULL,
    agent_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    cuisine_type VARCHAR(64) NULL,
    address VARCHAR(512) NULL,
    operating_hours VARCHAR(255) NULL,
    capabilities_json JSON NULL,
    agent_config_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_agent_store UNIQUE (store_id)
);
```

#### agent_wallets (Flyway V045)

```sql
CREATE TABLE agent_wallets (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wallet_id VARCHAR(64) NOT NULL UNIQUE,
    agent_id VARCHAR(64) NOT NULL,
    balance_cents BIGINT NOT NULL DEFAULT 0,
    total_income_cents BIGINT NOT NULL DEFAULT 0,
    total_expense_cents BIGINT NOT NULL DEFAULT 0,
    currency_code VARCHAR(16) NOT NULL DEFAULT 'SGD',
    wallet_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_agent UNIQUE (agent_id)
);
```

#### wallet_transactions (Flyway V045)

```sql
CREATE TABLE wallet_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    wallet_id VARCHAR(64) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    balance_after_cents BIGINT NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_ref VARCHAR(128) NULL,
    counterparty VARCHAR(255) NULL,
    description VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wallet_tx (wallet_id, created_at)
);
```

#### agent_interactions (Flyway V045)

```sql
CREATE TABLE agent_interactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    interaction_id VARCHAR(64) NOT NULL UNIQUE,
    agent_id VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    interaction_type VARCHAR(64) NOT NULL,
    requester_agent_id VARCHAR(128) NULL,
    request_summary TEXT NOT NULL,
    request_detail_json JSON NULL,
    response_summary TEXT NULL,
    response_detail_json JSON NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    auto_handled BOOLEAN NOT NULL DEFAULT FALSE,
    handled_by VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    INDEX idx_agent_interaction (agent_id, status)
);
```

---

### 2.21 Assets Module

#### image_assets (Flyway V065)

```sql
CREATE TABLE image_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255),
    content_type VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_image_id UNIQUE (image_id),
    INDEX idx_image_merchant (merchant_id),
    INDEX idx_image_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 2.22 Legacy Tables (to be replaced)

#### auth_users (Flyway V060 — replaced by `users`)

```sql
CREATE TABLE auth_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128),
    role VARCHAR(32) NOT NULL DEFAULT 'CASHIER',
    merchant_id BIGINT NULL,
    store_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_auth_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### staff (Flyway V023 — replaced by `users`)

```sql
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
```

#### roles (Flyway V023 — replaced by `custom_roles`)

```sql
CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL
);
```

#### role_permissions (Flyway V023 — replaced by `custom_role_permissions`)

```sql
CREATE TABLE role_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    CONSTRAINT uk_role_permission UNIQUE (role_code, permission_code),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_code) REFERENCES roles(role_code)
);
```

---

## 3. ALTER TABLE Statements

All modifications to existing Flyway tables from design documents 66-73:

### store_tables (doc/66)
```sql
ALTER TABLE store_tables ADD COLUMN area VARCHAR(64) NULL AFTER table_name;
ALTER TABLE store_tables ADD COLUMN qr_code_url VARCHAR(512) NULL AFTER sort_order;
```

### table_sessions (doc/66 + doc/73)
```sql
ALTER TABLE table_sessions ADD COLUMN dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE' AFTER session_status;
ALTER TABLE table_sessions ADD COLUMN guest_count INT DEFAULT 1 AFTER dining_mode;
ALTER TABLE table_sessions ADD COLUMN buffet_package_id BIGINT NULL AFTER guest_count;
ALTER TABLE table_sessions ADD COLUMN buffet_started_at TIMESTAMP NULL AFTER buffet_package_id;
ALTER TABLE table_sessions ADD COLUMN buffet_ends_at TIMESTAMP NULL AFTER buffet_started_at;
ALTER TABLE table_sessions ADD COLUMN buffet_status VARCHAR(32) NULL AFTER buffet_ends_at;
ALTER TABLE table_sessions ADD COLUMN channel_code VARCHAR(64) NULL AFTER buffet_status;
ALTER TABLE table_sessions ADD COLUMN tracking_value VARCHAR(255) NULL AFTER channel_code;
```

### products (doc/66)
```sql
ALTER TABLE products ADD COLUMN menu_modes JSON NULL AFTER image_id;
ALTER TABLE products ADD COLUMN is_featured BOOLEAN DEFAULT FALSE AFTER menu_modes;
ALTER TABLE products ADD COLUMN is_recommended BOOLEAN DEFAULT FALSE AFTER is_featured;
ALTER TABLE products ADD COLUMN allergen_info VARCHAR(512) NULL AFTER is_recommended;
ALTER TABLE products ADD COLUMN sort_order INT DEFAULT 0 AFTER allergen_info;
```

### skus (doc/66 + doc/67)
```sql
-- doc/66: Kitchen routing and inventory
ALTER TABLE skus ADD COLUMN station_id BIGINT NULL AFTER image_id;
ALTER TABLE skus ADD COLUMN print_route VARCHAR(64) NULL AFTER station_id;
ALTER TABLE skus ADD COLUMN cost_price_cents BIGINT NULL AFTER base_price_cents;
ALTER TABLE skus ADD COLUMN recipe_id BIGINT NULL AFTER cost_price_cents;

-- doc/67: Customer-facing fields
ALTER TABLE skus ADD COLUMN description TEXT NULL AFTER sku_name;
ALTER TABLE skus ADD COLUMN allergen_tags JSON NULL AFTER description;
ALTER TABLE skus ADD COLUMN spice_level INT DEFAULT 0 AFTER allergen_tags;
ALTER TABLE skus ADD COLUMN is_featured BOOLEAN DEFAULT FALSE AFTER spice_level;
ALTER TABLE skus ADD COLUMN is_new BOOLEAN DEFAULT FALSE AFTER is_featured;
ALTER TABLE skus ADD COLUMN tags JSON NULL AFTER is_new;
ALTER TABLE skus ADD COLUMN min_order_qty INT DEFAULT 1 AFTER tags;
ALTER TABLE skus ADD COLUMN max_order_qty INT DEFAULT 0 AFTER min_order_qty;
ALTER TABLE skus ADD COLUMN calories INT NULL AFTER max_order_qty;
ALTER TABLE skus ADD COLUMN sort_order INT DEFAULT 0 AFTER calories;
ALTER TABLE skus ADD COLUMN merchant_id BIGINT NULL AFTER product_id;

-- doc/67: Kitchen-side fields
ALTER TABLE skus ADD COLUMN prep_time_minutes INT NULL AFTER print_route;
ALTER TABLE skus ADD COLUMN kitchen_note_template VARCHAR(512) NULL AFTER prep_time_minutes;
ALTER TABLE skus ADD COLUMN kitchen_priority INT DEFAULT 0 AFTER kitchen_note_template;

-- doc/67: Inventory-side fields
ALTER TABLE skus ADD COLUMN requires_stock_deduct BOOLEAN DEFAULT TRUE AFTER recipe_id;
```

### submitted_orders (doc/66 + doc/73)
```sql
ALTER TABLE submitted_orders ADD COLUMN dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE' AFTER source_order_type;
ALTER TABLE submitted_orders ADD COLUMN external_platform VARCHAR(64) NULL AFTER dining_mode;
ALTER TABLE submitted_orders ADD COLUMN external_order_no VARCHAR(128) NULL AFTER external_platform;
ALTER TABLE submitted_orders ADD COLUMN delivery_status VARCHAR(32) NULL AFTER external_order_no;
ALTER TABLE submitted_orders ADD COLUMN delivery_address TEXT NULL AFTER delivery_status;
ALTER TABLE submitted_orders ADD COLUMN delivery_contact_name VARCHAR(128) NULL AFTER delivery_address;
ALTER TABLE submitted_orders ADD COLUMN delivery_contact_phone VARCHAR(64) NULL AFTER delivery_contact_name;
ALTER TABLE submitted_orders ADD COLUMN channel_code VARCHAR(64) NULL AFTER delivery_contact_phone;
```

### inventory_items (doc/70)
```sql
ALTER TABLE inventory_items ADD COLUMN purchase_unit VARCHAR(32) NULL AFTER unit;
ALTER TABLE inventory_items ADD COLUMN purchase_to_stock_ratio DECIMAL(10,4) DEFAULT 1.0 AFTER purchase_unit;
ALTER TABLE inventory_items ADD COLUMN usage_unit VARCHAR(32) NULL AFTER purchase_to_stock_ratio;
ALTER TABLE inventory_items ADD COLUMN stock_to_usage_ratio DECIMAL(10,4) DEFAULT 1.0 AFTER usage_unit;
ALTER TABLE inventory_items ADD COLUMN shelf_life_days INT NULL AFTER expiry_warning_days;
ALTER TABLE inventory_items ADD COLUMN requires_batch_tracking BOOLEAN DEFAULT FALSE AFTER shelf_life_days;
ALTER TABLE inventory_items ADD COLUMN default_supplier_id BIGINT NULL AFTER item_status;
ALTER TABLE inventory_items ADD COLUMN last_purchase_price_cents BIGINT NULL AFTER default_supplier_id;
ALTER TABLE inventory_items ADD COLUMN avg_daily_usage DECIMAL(14,4) DEFAULT 0 AFTER last_purchase_price_cents;
```

### suppliers (doc/70)
```sql
ALTER TABLE suppliers ADD COLUMN payment_terms VARCHAR(64) NULL AFTER address;
ALTER TABLE suppliers ADD COLUMN lead_time_days INT DEFAULT 1 AFTER payment_terms;
ALTER TABLE suppliers ADD COLUMN rating DECIMAL(2,1) NULL AFTER lead_time_days;
ALTER TABLE suppliers ADD COLUMN notes TEXT NULL AFTER rating;
```

### inventory_movements (doc/70)
```sql
ALTER TABLE inventory_movements ADD COLUMN batch_id BIGINT NULL AFTER inventory_item_id;
ALTER TABLE inventory_movements ADD COLUMN unit_cost_cents BIGINT NULL AFTER quantity_change;
```

### order_suggestions (doc/70)
```sql
ALTER TABLE order_suggestions ADD COLUMN supplier_id BIGINT NULL AFTER inventory_item_id;
ALTER TABLE order_suggestions ADD COLUMN purchase_order_id BIGINT NULL AFTER approved_by;
ALTER TABLE order_suggestions ADD COLUMN estimated_cost_cents BIGINT NULL AFTER suggested_qty;
```

### members (doc/71)
```sql
ALTER TABLE members ADD COLUMN referral_code VARCHAR(32) NULL AFTER member_status;
ALTER TABLE members ADD COLUMN referred_by_member_id BIGINT NULL AFTER referral_code;
ALTER TABLE members ADD COLUMN referral_count INT DEFAULT 0 AFTER referred_by_member_id;
ALTER TABLE members ADD COLUMN last_visit_at TIMESTAMP NULL AFTER referral_count;
ALTER TABLE members ADD COLUMN total_visit_count INT DEFAULT 0 AFTER last_visit_at;
ALTER TABLE members ADD COLUMN avg_spend_cents BIGINT DEFAULT 0 AFTER total_visit_count;
ALTER TABLE members ADD COLUMN preferred_dining_mode VARCHAR(32) NULL AFTER avg_spend_cents;
ALTER TABLE members ADD COLUMN birthday DATE NULL AFTER preferred_dining_mode;
ALTER TABLE members ADD COLUMN anniversary DATE NULL AFTER birthday;
ALTER TABLE members ADD COLUMN language VARCHAR(16) DEFAULT 'zh' AFTER anniversary;
ALTER TABLE members ADD COLUMN communication_preference VARCHAR(32) DEFAULT 'WHATSAPP' AFTER language;
```

### member_accounts (doc/71 + doc/72)
```sql
-- doc/71
ALTER TABLE member_accounts ADD COLUMN total_points_earned BIGINT DEFAULT 0 AFTER lifetime_recharge_cents;
ALTER TABLE member_accounts ADD COLUMN total_points_redeemed BIGINT DEFAULT 0 AFTER total_points_earned;
ALTER TABLE member_accounts ADD COLUMN total_coupons_used INT DEFAULT 0 AFTER total_points_redeemed;
ALTER TABLE member_accounts ADD COLUMN current_tier_started_at TIMESTAMP NULL AFTER total_coupons_used;
ALTER TABLE member_accounts ADD COLUMN next_tier_threshold_cents BIGINT NULL AFTER current_tier_started_at;

-- doc/72
ALTER TABLE member_accounts ADD COLUMN available_points BIGINT DEFAULT 0 AFTER points_balance;
ALTER TABLE member_accounts ADD COLUMN frozen_points BIGINT DEFAULT 0 AFTER available_points;
ALTER TABLE member_accounts ADD COLUMN expired_points BIGINT DEFAULT 0 AFTER frozen_points;
ALTER TABLE member_accounts ADD COLUMN available_cash_cents BIGINT DEFAULT 0 AFTER cash_balance_cents;
ALTER TABLE member_accounts ADD COLUMN frozen_cash_cents BIGINT DEFAULT 0 AFTER available_cash_cents;
```

### member_points_ledger (doc/72)
```sql
ALTER TABLE member_points_ledger ADD COLUMN batch_id BIGINT NULL AFTER source_ref;
ALTER TABLE member_points_ledger ADD COLUMN rule_id BIGINT NULL AFTER batch_id;
ALTER TABLE member_points_ledger ADD COLUMN expires_at TIMESTAMP NULL AFTER rule_id;
```

### cashier_shifts (doc/69)
```sql
ALTER TABLE cashier_shifts ADD COLUMN employee_id BIGINT NULL AFTER cashier_staff_id;
ALTER TABLE cashier_shifts ADD COLUMN attendance_id BIGINT NULL AFTER employee_id;
```

---

## 4. Foreign Key Map

| Source Table | FK Column | Target Table |
|-------------|-----------|-------------|
| merchant_configs | merchant_id | merchants |
| brands | merchant_id | merchants |
| brand_countries | brand_id | brands |
| stores | merchant_id | merchants |
| store_tables | store_id | stores |
| store_terminals | store_id | stores |
| product_categories | store_id | stores |
| products | store_id, category_id | stores, product_categories |
| skus | product_id | products |
| store_sku_availability | store_id, sku_id | stores, skus |
| sku_price_overrides | sku_id, store_id | skus, stores |
| sku_channel_configs | sku_id | skus |
| sku_faq | sku_id | skus |
| modifier_groups | merchant_id | merchants |
| modifier_options | group_id | modifier_groups |
| sku_modifier_group_bindings | sku_id, modifier_group_id | skus, modifier_groups |
| menu_time_slots | store_id | stores |
| menu_time_slot_products | time_slot_id, product_id | menu_time_slots, products |
| buffet_packages | store_id | stores |
| buffet_package_items | package_id, sku_id | buffet_packages, skus |
| active_table_orders | merchant_id, store_id, table_id | merchants, stores, store_tables |
| active_table_order_items | active_order_id, sku_id | active_table_orders, skus |
| order_events | active_order_id | active_table_orders |
| table_sessions | merchant_id, store_id, table_id | merchants, stores, store_tables |
| submitted_orders | table_session_id, merchant_id, store_id, table_id | table_sessions, merchants, stores, store_tables |
| submitted_order_items | submitted_order_db_id, sku_id | submitted_orders, skus |
| kitchen_stations | store_id | stores |
| kitchen_tickets | store_id, station_id, submitted_order_id | stores, kitchen_stations, submitted_orders |
| kitchen_ticket_items | ticket_id | kitchen_tickets |
| refund_records | settlement_id | settlement_records |
| inventory_items | store_id | stores |
| inventory_batches | store_id, inventory_item_id, supplier_id | stores, inventory_items, suppliers |
| recipes | sku_id, inventory_item_id | skus, inventory_items |
| purchase_invoices | store_id | stores |
| purchase_invoice_items | invoice_id, inventory_item_id | purchase_invoices, inventory_items |
| purchase_orders | store_id, supplier_id | stores, suppliers |
| purchase_order_items | po_id, inventory_item_id | purchase_orders, inventory_items |
| inventory_movements | store_id, inventory_item_id | stores, inventory_items |
| suppliers | merchant_id | merchants |
| supplier_price_history | supplier_id, inventory_item_id | suppliers, inventory_items |
| order_suggestions | store_id, inventory_item_id | stores, inventory_items |
| stocktake_tasks | store_id | stores |
| stocktake_items | task_id, inventory_item_id | stocktake_tasks, inventory_items |
| waste_records | store_id, inventory_item_id, batch_id | stores, inventory_items, inventory_batches |
| inventory_transfers | from_store_id, to_store_id | stores, stores |
| inventory_transfer_items | transfer_id, inventory_item_id, batch_id | inventory_transfers, inventory_items, inventory_batches |
| members | merchant_id | merchants |
| member_accounts | member_id | members |
| member_recharge_orders | member_id | members |
| member_points_ledger | member_id | members |
| member_cash_ledger | member_id | members |
| member_tier_rules | merchant_id | merchants |
| coupon_templates | merchant_id | merchants |
| member_coupons | member_id, template_id | members, coupon_templates |
| recharge_campaigns | merchant_id, bonus_coupon_template_id | merchants, coupon_templates |
| points_rules | merchant_id | merchants |
| points_expiry_rules | merchant_id | merchants |
| points_batches | member_id | members |
| points_deduction_rules | merchant_id | merchants |
| cash_balance_rules | merchant_id | merchants |
| points_redemption_items | merchant_id, reward_sku_id, reward_coupon_template_id | merchants, skus, coupon_templates |
| points_redemption_records | member_id, redemption_item_id | members, points_redemption_items |
| referral_rewards_config | merchant_id | merchants |
| referral_records | referrer_member_id, referee_member_id | members, members |
| member_tags | merchant_id | merchants |
| member_tag_assignments | member_id, tag_id | members, member_tags |
| member_consumption_profiles | member_id | members |
| marketing_campaigns | merchant_id, coupon_template_id | merchants, coupon_templates |
| marketing_send_records | campaign_id, member_id | marketing_campaigns, members |
| users | merchant_id | merchants |
| custom_role_permissions | role_id | custom_roles |
| user_roles | user_id, role_id | users, custom_roles |
| user_store_access | user_id, store_id | users, stores |
| employees | user_id, merchant_id, primary_store_id | users, merchants, stores |
| employee_schedules | employee_id, store_id, shift_template_id | employees, stores, shift_templates |
| attendance_records | employee_id, store_id, schedule_id | employees, stores, employee_schedules |
| leave_requests | employee_id | employees |
| leave_balances | employee_id | employees |
| payroll_periods | merchant_id | merchants |
| payroll_records | period_id, employee_id, store_id | payroll_periods, employees, stores |
| employee_performance_log | employee_id, store_id | employees, stores |
| shift_templates | merchant_id | merchants |
| cashier_shift_settlements | shift_id | cashier_shifts |
| queue_tickets | store_id | stores |
| gto_export_items | batch_id | gto_export_batches |
| promotion_rules | merchant_id, store_id | merchants, stores |
| promotion_rule_conditions | rule_id | promotion_rules |
| promotion_rule_rewards | rule_id, gift_sku_id | promotion_rules, skus |
| promotion_hits | active_order_id, rule_id | active_table_orders, promotion_rules |
| recommendation_slots | store_id | stores |
| recommendation_slot_items | slot_id, sku_id | recommendation_slots, skus |
| delivery_platform_configs | store_id | stores |
| delivery_orders | submitted_order_id | submitted_orders |
| channels | merchant_id | merchants |
| channel_commission_rules | channel_id | channels |
| order_channel_attribution | submitted_order_id, channel_id | submitted_orders, channels |
| channel_commission_records | channel_id, rule_id, submitted_order_id | channels, channel_commission_rules, submitted_orders |
| channel_settlement_batches | channel_id | channels |
| channel_performance_daily | channel_id | channels |
| restaurant_agents | store_id | stores |

---

## 5. Index Summary

| Table | Index Name | Columns |
|-------|-----------|---------|
| stores | idx_stores_brand | brand_id |
| stores | idx_stores_country | country_id |
| table_sessions | idx_table_sessions_open | store_id, table_id, session_status |
| submitted_orders | idx_submitted_orders_lookup | store_id, table_id, settlement_status |
| kitchen_tickets | idx_kt_station_status | station_id, ticket_status |
| kitchen_tickets | idx_kt_store_status | store_id, ticket_status |
| inventory_movements | idx_im_item | inventory_item_id, created_at |
| inventory_batches | idx_ib_item_expiry | inventory_item_id, expiry_date |
| inventory_batches | idx_ib_status | store_id, batch_status |
| purchase_orders | idx_po_status | store_id, po_status |
| supplier_price_history | idx_sph_item_date | inventory_item_id, effective_date DESC |
| supplier_price_history | idx_sph_supplier_item | supplier_id, inventory_item_id |
| stocktake_tasks | idx_stt_date | store_id, stocktake_date |
| waste_records | idx_wr_date | store_id, created_at |
| member_coupons | idx_mc_member_status | member_id, coupon_status |
| points_batches | idx_pb_member_expiry | member_id, expires_at, batch_status |
| points_batches | idx_pb_expiry | expires_at, batch_status |
| member_cash_ledger | idx_mcl_member | member_id, created_at |
| referral_records | idx_rr_referrer | referrer_member_id |
| marketing_send_records | idx_msr_member | member_id, created_at |
| employee_schedules | idx_es_store_date | store_id, schedule_date |
| attendance_records | idx_ar_store_date | store_id, attendance_date |
| leave_requests | idx_lr_employee_date | employee_id, start_date |
| employee_performance_log | idx_epl_employee | employee_id, event_date |
| queue_tickets | idx_qt_status | store_id, ticket_status |
| reservations | idx_reservation_store_status | store_id, reservation_status |
| reservations | idx_reservation_store_table | store_id, table_id |
| refund_records | idx_refund_store | store_id, created_at |
| refund_records | idx_refund_settlement | settlement_id |
| payment_attempts | idx_payment_attempts_provider_payment_id | provider, provider_payment_id |
| payment_attempts | idx_payment_attempts_table | store_id, table_id, attempt_status |
| action_log | idx_action_log_actor | actor_type, created_at |
| action_log | idx_action_log_tool | tool_name, created_at |
| ai_proposal | idx_proposal_status | status, created_at |
| ai_proposal | idx_proposal_role | advisor_role, status |
| ai_recommendations | idx_rec_store_status | store_id, status |
| ai_recommendations | idx_rec_store_role | store_id, advisor_role |
| wallet_transactions | idx_wallet_tx | wallet_id, created_at |
| agent_interactions | idx_agent_interaction | agent_id, status |
| image_assets | idx_image_merchant | merchant_id |
| image_assets | idx_image_status | status |
| order_channel_attribution | idx_oca_channel | channel_id, created_at |
| channel_commission_records | idx_ccrd_channel_status | channel_id, commission_status |
| channel_commission_records | idx_ccrd_store | store_id, created_at |
| channel_settlement_batches | idx_csb_channel_period | channel_id, period_start |
| channel_performance_daily | idx_cpd_date | report_date |
