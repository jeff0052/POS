-- Phase1 自助餐: 档位-SKU 关联表
CREATE TABLE buffet_package_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    inclusion_type VARCHAR(32) NOT NULL DEFAULT 'INCLUDED'
      COMMENT 'INCLUDED(套餐内免费)|SURCHARGE(有差价)|EXCLUDED(套餐外原价)',
    surcharge_cents BIGINT NOT NULL DEFAULT 0 COMMENT '差价(分)，仅 SURCHARGE 时有效',
    max_qty_per_person INT NULL COMMENT '每人限点数量，NULL=不限',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bpi_package FOREIGN KEY (package_id) REFERENCES buffet_packages(id) ON DELETE CASCADE,
    CONSTRAINT fk_bpi_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_bpi UNIQUE (package_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
