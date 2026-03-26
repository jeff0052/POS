-- Merchant hierarchy: Merchant → Brand → Country → Store

-- Brands table (一个商户可以有多个品牌)
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

-- Country presence (一个品牌可以进入多个国家)
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

-- Add brand_id and country_id to stores
ALTER TABLE stores ADD COLUMN brand_id BIGINT NULL AFTER merchant_id;
ALTER TABLE stores ADD COLUMN country_id BIGINT NULL AFTER brand_id;

CREATE INDEX idx_stores_brand ON stores(brand_id);
CREATE INDEX idx_stores_country ON stores(country_id);
