DELETE duplicate_record
FROM settlement_records duplicate_record
JOIN settlement_records kept_record
  ON duplicate_record.active_order_id = kept_record.active_order_id
 AND duplicate_record.id > kept_record.id;

ALTER TABLE settlement_records
    ADD CONSTRAINT uk_settlement_records_active_order UNIQUE (active_order_id);
