-- G01/G03/G10: 桌台增强 (区域/容量/QR/状态扩展) + stores 加 JWT 密钥
ALTER TABLE store_tables
  ADD COLUMN zone VARCHAR(64) NULL COMMENT '区域(大厅/包间/露台)' AFTER table_name,
  ADD COLUMN min_guests INT NOT NULL DEFAULT 1 AFTER zone,
  ADD COLUMN max_guests INT NOT NULL DEFAULT 4 AFTER min_guests,
  ADD COLUMN qr_token VARCHAR(64) NULL COMMENT 'UUID, DB lookup 验证' AFTER max_guests,
  ADD COLUMN qr_generated_at TIMESTAMP NULL AFTER qr_token,
  ADD COLUMN qr_expires_at TIMESTAMP NULL AFTER qr_generated_at,
  MODIFY COLUMN table_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE'
    COMMENT 'AVAILABLE|OCCUPIED|RESERVED|PENDING_CLEAN|MERGED|DISABLED';

ALTER TABLE stores
  ADD COLUMN jwt_secret VARCHAR(128) NULL COMMENT 'QR ordering JWT 签名密钥';
