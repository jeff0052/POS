-- V066: Catch-up migration for docs 66-73 tables
-- Creates all tables designed in docs 66-73 that were never built via Flyway
-- Source of truth: docs/75-complete-database-schema.md
-- Total: ~65 new tables + ALTER statements for existing tables

-- sku_price_overrides
CREATE TABLE IF NOT EXISTS sku_price_overrides (
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

-- sku_channel_configs
CREATE TABLE IF NOT EXISTS sku_channel_configs (
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

-- sku_faq
CREATE TABLE IF NOT EXISTS sku_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    question VARCHAR(255) NOT NULL,
    answer TEXT NOT NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sf_sku FOREIGN KEY (sku_id) REFERENCES skus(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- modifier_groups
CREATE TABLE IF NOT EXISTS modifier_groups (
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

-- modifier_options
CREATE TABLE IF NOT EXISTS modifier_options (
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

-- sku_modifier_group_bindings
CREATE TABLE IF NOT EXISTS sku_modifier_group_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    modifier_group_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    CONSTRAINT fk_smgb_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT fk_smgb_group FOREIGN KEY (modifier_group_id) REFERENCES modifier_groups(id),
    CONSTRAINT uk_smgb UNIQUE (sku_id, modifier_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- menu_time_slots
CREATE TABLE IF NOT EXISTS menu_time_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    slot_code VARCHAR(64) NOT NULL,
    slot_name VARCHAR(128) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    applicable_days JSON NULL,
    dining_modes JSON NULL,
    is_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mts_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_mts UNIQUE (store_id, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- menu_time_slot_products
CREATE TABLE IF NOT EXISTS menu_time_slot_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_slot_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_mtsp_slot FOREIGN KEY (time_slot_id) REFERENCES menu_time_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_mtsp_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_mtsp UNIQUE (time_slot_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- buffet_packages
CREATE TABLE IF NOT EXISTS buffet_packages (
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

-- buffet_package_items
CREATE TABLE IF NOT EXISTS buffet_package_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    is_included BOOLEAN DEFAULT TRUE,
    surcharge_cents BIGINT DEFAULT 0,
    CONSTRAINT fk_bpi_package FOREIGN KEY (package_id) REFERENCES buffet_packages(id) ON DELETE CASCADE,
    CONSTRAINT fk_bpi_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_bpi UNIQUE (package_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- kitchen_stations
CREATE TABLE IF NOT EXISTS kitchen_stations (
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

-- kitchen_tickets
CREATE TABLE IF NOT EXISTS kitchen_tickets (
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

-- kitchen_ticket_items
CREATE TABLE IF NOT EXISTS kitchen_ticket_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_name_snapshot VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    item_remark VARCHAR(255) NULL,
    option_snapshot_json JSON NULL,
    CONSTRAINT fk_kti_ticket FOREIGN KEY (ticket_id) REFERENCES kitchen_tickets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- suppliers
CREATE TABLE IF NOT EXISTS suppliers (
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

-- inventory_items
CREATE TABLE IF NOT EXISTS inventory_items (
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

-- inventory_batches
CREATE TABLE IF NOT EXISTS inventory_batches (
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

-- recipes
CREATE TABLE IF NOT EXISTS recipes (
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

-- purchase_invoices
CREATE TABLE IF NOT EXISTS purchase_invoices (
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

-- purchase_invoice_items
CREATE TABLE IF NOT EXISTS purchase_invoice_items (
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

-- purchase_orders
CREATE TABLE IF NOT EXISTS purchase_orders (
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

-- purchase_order_items
CREATE TABLE IF NOT EXISTS purchase_order_items (
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

-- inventory_movements
CREATE TABLE IF NOT EXISTS inventory_movements (
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

-- supplier_price_history
CREATE TABLE IF NOT EXISTS supplier_price_history (
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

-- order_suggestions
CREATE TABLE IF NOT EXISTS order_suggestions (
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

-- stocktake_tasks
CREATE TABLE IF NOT EXISTS stocktake_tasks (
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

-- stocktake_items
CREATE TABLE IF NOT EXISTS stocktake_items (
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

-- waste_records
CREATE TABLE IF NOT EXISTS waste_records (
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

-- inventory_transfers
CREATE TABLE IF NOT EXISTS inventory_transfers (
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

-- inventory_transfer_items
CREATE TABLE IF NOT EXISTS inventory_transfer_items (
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

-- member_cash_ledger
CREATE TABLE IF NOT EXISTS member_cash_ledger (
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

-- member_tier_rules
CREATE TABLE IF NOT EXISTS member_tier_rules (
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

-- coupon_templates
CREATE TABLE IF NOT EXISTS coupon_templates (
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
    applicable_dining_modes JSON NULL,
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

-- member_coupons
CREATE TABLE IF NOT EXISTS member_coupons (
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

-- recharge_campaigns
CREATE TABLE IF NOT EXISTS recharge_campaigns (
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

-- points_rules
CREATE TABLE IF NOT EXISTS points_rules (
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

-- points_expiry_rules
CREATE TABLE IF NOT EXISTS points_expiry_rules (
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

-- points_batches
CREATE TABLE IF NOT EXISTS points_batches (
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

-- points_deduction_rules
CREATE TABLE IF NOT EXISTS points_deduction_rules (
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

-- cash_balance_rules
CREATE TABLE IF NOT EXISTS cash_balance_rules (
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

-- points_redemption_items
CREATE TABLE IF NOT EXISTS points_redemption_items (
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

-- points_redemption_records
CREATE TABLE IF NOT EXISTS points_redemption_records (
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

-- referral_rewards_config
CREATE TABLE IF NOT EXISTS referral_rewards_config (
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

-- referral_records
CREATE TABLE IF NOT EXISTS referral_records (
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

-- member_tags
CREATE TABLE IF NOT EXISTS member_tags (
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

-- member_tag_assignments
CREATE TABLE IF NOT EXISTS member_tag_assignments (
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

-- member_consumption_profiles
CREATE TABLE IF NOT EXISTS member_consumption_profiles (
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

-- marketing_campaigns
CREATE TABLE IF NOT EXISTS marketing_campaigns (
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

-- marketing_send_records
CREATE TABLE IF NOT EXISTS marketing_send_records (
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

-- users
CREATE TABLE IF NOT EXISTS users (
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

-- permissions
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(64) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    permission_group VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    risk_level VARCHAR(16) DEFAULT 'LOW',
    CONSTRAINT uk_perm_code UNIQUE (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- custom_roles
CREATE TABLE IF NOT EXISTS custom_roles (
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

-- custom_role_permissions
CREATE TABLE IF NOT EXISTS custom_role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    CONSTRAINT fk_crp_role FOREIGN KEY (role_id) REFERENCES custom_roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_crp UNIQUE (role_id, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT NULL,
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES custom_roles(id),
    CONSTRAINT uk_ur UNIQUE (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_store_access
CREATE TABLE IF NOT EXISTS user_store_access (
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

-- employees
CREATE TABLE IF NOT EXISTS employees (
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

-- shift_templates
CREATE TABLE IF NOT EXISTS shift_templates (
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

-- employee_schedules
CREATE TABLE IF NOT EXISTS employee_schedules (
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

-- attendance_records
CREATE TABLE IF NOT EXISTS attendance_records (
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

-- leave_requests
CREATE TABLE IF NOT EXISTS leave_requests (
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

-- leave_balances
CREATE TABLE IF NOT EXISTS leave_balances (
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

-- payroll_periods
CREATE TABLE IF NOT EXISTS payroll_periods (
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

-- payroll_records
CREATE TABLE IF NOT EXISTS payroll_records (
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

-- employee_performance_log
CREATE TABLE IF NOT EXISTS employee_performance_log (
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

-- queue_tickets
CREATE TABLE IF NOT EXISTS queue_tickets (
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

-- recommendation_slots
CREATE TABLE IF NOT EXISTS recommendation_slots (
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
    CONSTRAINT fk_recslot_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_recslot UNIQUE (store_id, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- recommendation_slot_items
CREATE TABLE IF NOT EXISTS recommendation_slot_items (
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

-- delivery_platform_configs
CREATE TABLE IF NOT EXISTS delivery_platform_configs (
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

-- delivery_orders
CREATE TABLE IF NOT EXISTS delivery_orders (
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

-- channels
CREATE TABLE IF NOT EXISTS channels (
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

-- channel_commission_rules
CREATE TABLE IF NOT EXISTS channel_commission_rules (
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

-- order_channel_attribution
CREATE TABLE IF NOT EXISTS order_channel_attribution (
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

-- channel_commission_records
CREATE TABLE IF NOT EXISTS channel_commission_records (
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

-- channel_settlement_batches
CREATE TABLE IF NOT EXISTS channel_settlement_batches (
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

-- channel_performance_daily
CREATE TABLE IF NOT EXISTS channel_performance_daily (
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
