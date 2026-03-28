-- V098: 候位票叫号追踪
-- Journey: J12 预约→入座
-- called_count 追踪叫了几次，3 次未应答 → SKIPPED
ALTER TABLE queue_tickets
  ADD COLUMN called_count INT DEFAULT 0 AFTER ticket_status,
  ADD COLUMN skipped_at TIMESTAMP NULL AFTER called_count;
