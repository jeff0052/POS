ALTER TABLE products
    ADD COLUMN attribute_config_json TEXT NULL,
    ADD COLUMN modifier_config_json TEXT NULL,
    ADD COLUMN combo_slot_config_json TEXT NULL;
