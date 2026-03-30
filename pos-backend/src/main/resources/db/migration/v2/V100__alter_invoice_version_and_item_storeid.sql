-- Add optimistic locking to purchase_invoices
ALTER TABLE purchase_invoices ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add store_id to purchase_invoice_items for defense-in-depth tenant isolation
ALTER TABLE purchase_invoice_items ADD COLUMN store_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE purchase_invoice_items ADD CONSTRAINT fk_invoice_items_store FOREIGN KEY (store_id) REFERENCES stores(id);
