-- V089: 库存驱动促销草案表
-- Journey: J07 店长, J08 库存
-- 临期/滞销自动生成草案，店长审批后创建 promotion_rule
CREATE TABLE inventory_driven_promotions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    -- NOTE: inventory_batch_id intentionally has no FK constraint — batches may be EXHAUSTED/deleted
    -- while the promotion draft record needs to be retained for audit purposes.
    inventory_batch_id BIGINT NULL,
    trigger_type VARCHAR(32) NOT NULL
      COMMENT 'NEAR_EXPIRY | OVERSTOCK | LOW_TURNOVER',
    suggested_discount_percent DECIMAL(5,2) NOT NULL
      COMMENT 'supports fractional discount like 12.50%',
    suggested_sku_ids JSON NOT NULL
      COMMENT '[101, 102, ...]',
    draft_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
      COMMENT 'DRAFT -> APPROVED -> CREATED | REJECTED | EXPIRED',
    promotion_rule_id BIGINT NULL
      COMMENT 'created promotion_rule after approval',
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_idp_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_idp_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    INDEX idx_idp_store_status (store_id, draft_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
