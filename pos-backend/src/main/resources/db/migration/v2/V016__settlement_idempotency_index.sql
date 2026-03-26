-- Add index on active_order_id for idempotency lookups
-- Note: Cannot use UNIQUE here because the same active_order_id may appear
-- across different stores. Using a regular index for query performance.
CREATE INDEX idx_settlement_active_order_id ON settlement_records (active_order_id);
