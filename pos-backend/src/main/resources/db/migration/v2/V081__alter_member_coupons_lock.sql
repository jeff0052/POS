-- V081: 优惠券 CAS 乐观锁
-- Journey: J04 券并发
-- CAS: UPDATE SET coupon_status='LOCKED', lock_version=lock_version+1
--      WHERE id=? AND lock_version=? AND coupon_status='AVAILABLE'
ALTER TABLE member_coupons
  ADD COLUMN lock_version INT NOT NULL DEFAULT 0 AFTER coupon_status,
  ADD COLUMN locked_at TIMESTAMP NULL AFTER lock_version;
