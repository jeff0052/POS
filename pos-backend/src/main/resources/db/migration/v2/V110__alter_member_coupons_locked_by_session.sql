-- V110: member_coupons 加 locked_by_session（会话级锁 owner）
ALTER TABLE member_coupons
  ADD COLUMN locked_by_session BIGINT NULL AFTER locked_at,
  ADD INDEX idx_mc_locked_timeout (coupon_status, locked_at),
  ADD INDEX idx_mc_locked_session (locked_by_session, coupon_status);
