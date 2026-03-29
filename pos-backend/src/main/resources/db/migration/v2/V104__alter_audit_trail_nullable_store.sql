-- V104: Allow audit_trail.store_id to be NULL for merchant-level operations
ALTER TABLE audit_trail DROP FOREIGN KEY fk_at_store;
ALTER TABLE audit_trail MODIFY COLUMN store_id BIGINT NULL;
ALTER TABLE audit_trail ADD CONSTRAINT fk_at_store2 FOREIGN KEY (store_id) REFERENCES stores(id);
