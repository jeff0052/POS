-- Add percentage discount support
ALTER TABLE promotion_rule_rewards ADD COLUMN discount_percent INT NULL AFTER discount_amount_cents;

-- Add usage tracking
ALTER TABLE promotion_rules ADD COLUMN usage_count INT NOT NULL DEFAULT 0 AFTER priority;
ALTER TABLE promotion_rules ADD COLUMN max_usage INT NULL AFTER usage_count;
