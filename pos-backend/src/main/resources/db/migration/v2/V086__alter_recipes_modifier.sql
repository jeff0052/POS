-- G14 不同规格 SOP 消耗差异: recipes 支持修饰符级别消耗
ALTER TABLE recipes
  ADD COLUMN modifier_option_id BIGINT NULL
    COMMENT '关联修饰符选项，NULL=基础消耗' AFTER inventory_item_id,
  ADD COLUMN consumption_multiplier DECIMAL(5,2) NOT NULL DEFAULT 1.00
    COMMENT '消耗倍率(大份=1.5)' AFTER consumption_qty,
  DROP INDEX uk_recipe,
  ADD UNIQUE INDEX uk_recipe_v2 (sku_id, inventory_item_id, modifier_option_id);
