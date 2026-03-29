-- V096: 预约表增强
-- Journey: J12 预约→入座
-- source: MANUAL | QR | PHONE | GOOGLE | PLATFORM
ALTER TABLE reservations
  ADD COLUMN contact_phone VARCHAR(32) NULL AFTER guest_name,
  ADD COLUMN source VARCHAR(32) DEFAULT 'MANUAL' AFTER reservation_status,
  ADD COLUMN reservation_date DATE NULL AFTER reservation_time,
  ADD COLUMN notes VARCHAR(512) NULL AFTER source;
