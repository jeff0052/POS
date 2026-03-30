-- V099: Add optimistic locking version columns + unique constraints for inventory entities

-- Optimistic locking: version column for concurrent settlement safety
ALTER TABLE inventory_items ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE inventory_batches ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Unique constraints to prevent race-condition duplicates
ALTER TABLE inventory_items ADD CONSTRAINT uq_inventory_items_store_code UNIQUE (store_id, item_code);
ALTER TABLE purchase_invoices ADD CONSTRAINT uq_purchase_invoices_store_no UNIQUE (store_id, invoice_no);
