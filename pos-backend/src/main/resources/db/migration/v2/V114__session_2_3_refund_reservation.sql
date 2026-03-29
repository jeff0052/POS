-- Session 2.3: Refund enhancement + Reservation contactPhone

-- 1. refund_line_items — item-level refund tracking
CREATE TABLE refund_line_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    refund_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    refund_amount_cents BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rli_refund FOREIGN KEY (refund_id) REFERENCES refund_records(id),
    INDEX idx_rli_refund (refund_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Extend refund_records with approval + reversal tracking
ALTER TABLE refund_records
    ADD COLUMN approval_status VARCHAR(32) NOT NULL DEFAULT 'AUTO_APPROVED' AFTER refund_status,
    ADD COLUMN points_reversed_cents BIGINT NOT NULL DEFAULT 0 AFTER approval_status,
    ADD COLUMN cash_reversed_cents BIGINT NOT NULL DEFAULT 0 AFTER points_reversed_cents,
    ADD COLUMN coupon_reversed BOOLEAN NOT NULL DEFAULT FALSE AFTER cash_reversed_cents,
    ADD COLUMN external_refund_status VARCHAR(32) NULL AFTER coupon_reversed;

-- 3. Add contactPhone to reservations
ALTER TABLE reservations
    ADD COLUMN contact_phone VARCHAR(32) NULL AFTER guest_name;
