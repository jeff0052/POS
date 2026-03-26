CREATE TABLE product_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_categories_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_product_categories_code UNIQUE (store_id, category_code)
);

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES product_categories(id),
    CONSTRAINT uk_products_code UNIQUE (store_id, product_code)
);

CREATE TABLE skus (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    sku_name VARCHAR(255) NOT NULL,
    base_price_cents BIGINT NOT NULL,
    sku_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_skus_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_skus_code UNIQUE (product_id, sku_code)
);

CREATE TABLE store_sku_availability (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_sku_availability_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_store_sku_availability_sku
        FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_store_sku_availability UNIQUE (store_id, sku_id)
);
