-- V067: ALTER existing tables per docs 66-73 design
-- These columns were designed in docs 66-73 but never added via Flyway
-- Source: docs/75-complete-database-schema.md Section 3 (ALTER TABLE statements)

-- table_sessions: buffet + channel support (doc/66 + doc/73)
ALTER TABLE table_sessions
  ADD COLUMN dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE' AFTER session_status,
  ADD COLUMN guest_count INT DEFAULT 1 AFTER dining_mode,
  ADD COLUMN buffet_package_id BIGINT NULL AFTER guest_count,
  ADD COLUMN buffet_started_at TIMESTAMP NULL AFTER buffet_package_id,
  ADD COLUMN buffet_ends_at TIMESTAMP NULL AFTER buffet_started_at,
  ADD COLUMN buffet_status VARCHAR(32) NULL AFTER buffet_ends_at,
  ADD COLUMN channel_code VARCHAR(64) NULL AFTER buffet_status,
  ADD COLUMN tracking_value VARCHAR(255) NULL AFTER channel_code;

-- store_tables: area + qr (doc/66)
ALTER TABLE store_tables
  ADD COLUMN area VARCHAR(64) NULL AFTER table_name,
  ADD COLUMN qr_code_url VARCHAR(512) NULL AFTER sort_order;

-- products: menu modes + features (doc/66)
ALTER TABLE products
  ADD COLUMN menu_modes JSON NULL AFTER image_id,
  ADD COLUMN is_featured BOOLEAN DEFAULT FALSE AFTER menu_modes,
  ADD COLUMN is_recommended BOOLEAN DEFAULT FALSE AFTER is_featured,
  ADD COLUMN allergen_info VARCHAR(512) NULL AFTER is_recommended,
  ADD COLUMN sort_order INT DEFAULT 0 AFTER allergen_info;

-- skus: kitchen routing + customer-facing fields (doc/66 + doc/67)
ALTER TABLE skus
  ADD COLUMN station_id BIGINT NULL AFTER image_id,
  ADD COLUMN print_route VARCHAR(64) NULL AFTER station_id,
  ADD COLUMN prep_time_minutes INT NULL AFTER print_route,
  ADD COLUMN kitchen_note_template VARCHAR(512) NULL AFTER prep_time_minutes,
  ADD COLUMN kitchen_priority INT DEFAULT 0 AFTER kitchen_note_template,
  ADD COLUMN recipe_id BIGINT NULL AFTER kitchen_priority,
  ADD COLUMN requires_stock_deduct BOOLEAN DEFAULT TRUE AFTER recipe_id,
  ADD COLUMN description TEXT NULL AFTER sku_name,
  ADD COLUMN allergen_tags JSON NULL AFTER description,
  ADD COLUMN spice_level INT DEFAULT 0 AFTER allergen_tags,
  ADD COLUMN is_featured BOOLEAN DEFAULT FALSE AFTER spice_level,
  ADD COLUMN is_new BOOLEAN DEFAULT FALSE AFTER is_featured,
  ADD COLUMN tags JSON NULL AFTER is_new,
  ADD COLUMN min_order_qty INT DEFAULT 1 AFTER tags,
  ADD COLUMN max_order_qty INT DEFAULT 0 AFTER min_order_qty,
  ADD COLUMN calories INT NULL AFTER max_order_qty,
  ADD COLUMN cost_price_cents BIGINT NULL AFTER base_price_cents;

-- submitted_orders: delivery + channel fields (doc/66 + doc/73)
ALTER TABLE submitted_orders
  ADD COLUMN dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE' AFTER source_order_type,
  ADD COLUMN external_platform VARCHAR(64) NULL AFTER dining_mode,
  ADD COLUMN external_order_no VARCHAR(128) NULL AFTER external_platform,
  ADD COLUMN delivery_status VARCHAR(32) NULL AFTER external_order_no,
  ADD COLUMN delivery_address TEXT NULL AFTER delivery_status,
  ADD COLUMN delivery_contact_name VARCHAR(128) NULL AFTER delivery_address,
  ADD COLUMN delivery_contact_phone VARCHAR(64) NULL AFTER delivery_contact_name,
  ADD COLUMN channel_code VARCHAR(64) NULL AFTER delivery_contact_phone;

-- cashier_shifts: employee link (doc/69)
ALTER TABLE cashier_shifts
  ADD COLUMN employee_id BIGINT NULL AFTER cashier_staff_id,
  ADD COLUMN attendance_id BIGINT NULL AFTER employee_id;

-- members: CRM fields (doc/71)
ALTER TABLE members
  ADD COLUMN referral_code VARCHAR(32) NULL AFTER member_status,
  ADD COLUMN referred_by_member_id BIGINT NULL AFTER referral_code,
  ADD COLUMN referral_count INT DEFAULT 0 AFTER referred_by_member_id,
  ADD COLUMN last_visit_at TIMESTAMP NULL AFTER referral_count,
  ADD COLUMN total_visit_count INT DEFAULT 0 AFTER last_visit_at,
  ADD COLUMN avg_spend_cents BIGINT DEFAULT 0 AFTER total_visit_count,
  ADD COLUMN preferred_dining_mode VARCHAR(32) NULL AFTER avg_spend_cents,
  ADD COLUMN birthday DATE NULL AFTER preferred_dining_mode,
  ADD COLUMN anniversary DATE NULL AFTER birthday,
  ADD COLUMN language VARCHAR(16) DEFAULT 'zh' AFTER anniversary,
  ADD COLUMN communication_preference VARCHAR(32) DEFAULT 'WHATSAPP' AFTER language;

-- member_accounts: frozen + stats (doc/71 + doc/72)
-- Note: last_activity_at already exists from V009
ALTER TABLE member_accounts
  ADD COLUMN available_points BIGINT DEFAULT 0 AFTER points_balance,
  ADD COLUMN frozen_points BIGINT DEFAULT 0 AFTER available_points,
  ADD COLUMN expired_points BIGINT DEFAULT 0 AFTER frozen_points,
  ADD COLUMN available_cash_cents BIGINT DEFAULT 0 AFTER cash_balance_cents,
  ADD COLUMN frozen_cash_cents BIGINT DEFAULT 0 AFTER available_cash_cents,
  ADD COLUMN total_points_earned BIGINT DEFAULT 0 AFTER lifetime_recharge_cents,
  ADD COLUMN total_points_redeemed BIGINT DEFAULT 0 AFTER total_points_earned,
  ADD COLUMN total_coupons_used INT DEFAULT 0 AFTER total_points_redeemed,
  ADD COLUMN current_tier_started_at TIMESTAMP NULL AFTER total_coupons_used,
  ADD COLUMN next_tier_threshold_cents BIGINT NULL AFTER current_tier_started_at;

-- member_points_ledger: batch + rule tracking (doc/72)
ALTER TABLE member_points_ledger
  ADD COLUMN batch_id BIGINT NULL AFTER source_ref,
  ADD COLUMN rule_id BIGINT NULL AFTER batch_id,
  ADD COLUMN expires_at TIMESTAMP NULL AFTER rule_id;
