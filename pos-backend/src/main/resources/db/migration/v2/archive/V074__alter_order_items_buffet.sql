-- Phase1 自助餐: 订单明细加 buffet 语义 (active + submitted 两张表同步)
ALTER TABLE active_table_order_items
  ADD COLUMN is_buffet_included BOOLEAN NOT NULL DEFAULT FALSE
    COMMENT '套餐内免费项' AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '差价(分)' AFTER is_buffet_included,
  ADD COLUMN buffet_package_id BIGINT NULL
    COMMENT '关联的自助餐档位' AFTER buffet_surcharge_cents;

ALTER TABLE submitted_order_items
  ADD COLUMN is_buffet_included BOOLEAN NOT NULL DEFAULT FALSE
    COMMENT '套餐内免费项' AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '差价(分)' AFTER is_buffet_included,
  ADD COLUMN buffet_package_id BIGINT NULL
    COMMENT '关联的自助餐档位' AFTER buffet_surcharge_cents;
