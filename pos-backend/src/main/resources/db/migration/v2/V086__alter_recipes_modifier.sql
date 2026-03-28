-- V086: SOP 配方加修饰符消耗规则
-- Journey: J08 库存
-- modifier_consumption_rules JSON 示例:
--   {"大份": {"multiplier": 1.5}, "加辣": {"add": [{"item_id": 42, "qty_grams": 10}]}}
ALTER TABLE recipes
  ADD COLUMN modifier_consumption_rules JSON NULL AFTER consumption_unit,
  ADD COLUMN base_multiplier DECIMAL(5,2) DEFAULT 1.00 AFTER modifier_consumption_rules,
  ADD COLUMN notes VARCHAR(512) NULL AFTER base_multiplier;
