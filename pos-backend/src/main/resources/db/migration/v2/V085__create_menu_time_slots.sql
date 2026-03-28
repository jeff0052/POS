-- Phase1 自助餐: 时段菜单
CREATE TABLE menu_time_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    slot_code VARCHAR(64) NOT NULL,
    slot_name VARCHAR(128) NOT NULL COMMENT '如"早茶""午市""晚市"',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    applicable_days JSON NOT NULL DEFAULT '["MON","TUE","WED","THU","FRI","SAT","SUN"]',
    dining_modes JSON NOT NULL DEFAULT '["A_LA_CARTE"]' COMMENT '该时段适用的用餐模式',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0 COMMENT '重叠时段取 priority 最高的',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mts_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_mts UNIQUE (store_id, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE menu_time_slot_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_slot_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'true=该时段显示, false=隐藏',
    CONSTRAINT fk_mtsp_slot FOREIGN KEY (time_slot_id) REFERENCES menu_time_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_mtsp_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_mtsp UNIQUE (time_slot_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
