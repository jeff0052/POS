-- V074: 订单行项加 buffet 字段
-- Journey: J02 自助餐
-- 两张表都加，提交时从 active 拷贝到 submitted
-- buffet_inclusion_type: INCLUDED | SURCHARGE | EXCLUDED
ALTER TABLE submitted_order_items
  ADD COLUMN is_buffet_included BOOLEAN DEFAULT FALSE AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT DEFAULT 0 AFTER is_buffet_included,
  ADD COLUMN buffet_inclusion_type VARCHAR(32) NULL AFTER buffet_surcharge_cents;

ALTER TABLE active_table_order_items
  ADD COLUMN is_buffet_included BOOLEAN DEFAULT FALSE AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT DEFAULT 0 AFTER is_buffet_included,
  ADD COLUMN buffet_inclusion_type VARCHAR(32) NULL AFTER buffet_surcharge_cents;
