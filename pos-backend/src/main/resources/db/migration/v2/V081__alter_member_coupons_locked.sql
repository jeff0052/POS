-- G02 支付叠加: 优惠券加 LOCKED 状态 + 乐观锁
-- coupon_status 扩展: AVAILABLE | LOCKED | USED | EXPIRED
-- AVAILABLE → LOCKED (结算冻结, CAS on lock_version)
-- LOCKED → USED (结算确认)
-- LOCKED → AVAILABLE (支付失败/放弃/超时回收)
ALTER TABLE member_coupons
  ADD COLUMN lock_version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER coupon_status,
  ADD COLUMN locked_by_session VARCHAR(64) NULL COMMENT '锁定的 table_session.session_id' AFTER lock_version,
  ADD COLUMN locked_at TIMESTAMP NULL COMMENT '锁定时间，用于超时回收' AFTER locked_by_session;
