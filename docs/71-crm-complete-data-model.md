# CRM 会员管理完整数据模型

**Version:** V20260328007
**Date:** 2026-03-28
**Status:** DRAFT

---

## 1. 会员全链路

```
注册/扫码绑定 → 消费积分 → 充值储值 → 领券用券 → 等级升降
                                                    ↓
推荐裂变（老带新）→ 推荐奖励 → 推荐人+被推荐人都得好处
                                                    ↓
消费画像（偏好/频次/客单价）→ 会员标签 → 精准营销触达
                                                    ↓
积分兑换（换菜品/换券）→ 流失预警 → 召回活动
```

---

## 2. 现有表（保留不变）

| 表 | 用途 | 状态 |
|----|------|------|
| members | 会员基础信息 | ✅ 保留，加字段 |
| member_accounts | 积分/储值余额 | ✅ 保留，加字段 |
| member_recharge_orders | 充值记录 | ✅ 保留 |
| member_points_ledger | 积分流水 | ✅ 保留 |

---

## 3. 现有表改动

### 3.1 members — 加推荐和画像字段

```sql
-- 推荐关系
ALTER TABLE members ADD COLUMN referral_code VARCHAR(32) NULL AFTER member_status;
ALTER TABLE members ADD COLUMN referred_by_member_id BIGINT NULL AFTER referral_code;
ALTER TABLE members ADD COLUMN referral_count INT DEFAULT 0 AFTER referred_by_member_id;

-- 消费画像摘要（定期聚合更新，不实时计算）
ALTER TABLE members ADD COLUMN last_visit_at TIMESTAMP NULL AFTER referral_count;
ALTER TABLE members ADD COLUMN total_visit_count INT DEFAULT 0 AFTER last_visit_at;
ALTER TABLE members ADD COLUMN avg_spend_cents BIGINT DEFAULT 0 AFTER total_visit_count;
ALTER TABLE members ADD COLUMN preferred_dining_mode VARCHAR(32) NULL AFTER avg_spend_cents;
ALTER TABLE members ADD COLUMN birthday DATE NULL AFTER preferred_dining_mode;
ALTER TABLE members ADD COLUMN anniversary DATE NULL AFTER birthday;
ALTER TABLE members ADD COLUMN language VARCHAR(16) DEFAULT 'zh' AFTER anniversary;
ALTER TABLE members ADD COLUMN communication_preference VARCHAR(32) DEFAULT 'WHATSAPP' AFTER language;

-- 系统生成唯一推荐码
-- referral_code 格式: 'REF' + 6位随机字母数字
```

### 3.2 member_accounts — 加券和兑换余额

```sql
ALTER TABLE member_accounts ADD COLUMN total_points_earned BIGINT DEFAULT 0 AFTER lifetime_recharge_cents;
ALTER TABLE member_accounts ADD COLUMN total_points_redeemed BIGINT DEFAULT 0 AFTER total_points_earned;
ALTER TABLE member_accounts ADD COLUMN total_coupons_used INT DEFAULT 0 AFTER total_points_redeemed;
ALTER TABLE member_accounts ADD COLUMN current_tier_started_at TIMESTAMP NULL AFTER total_coupons_used;
ALTER TABLE member_accounts ADD COLUMN next_tier_threshold_cents BIGINT NULL AFTER current_tier_started_at;
```

---

## 4. 新增表 — 等级规则

### 4.1 member_tier_rules（等级规则，可配置）

替代硬编码的等级阈值：

```sql
CREATE TABLE member_tier_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    tier_code VARCHAR(32) NOT NULL,
    tier_name VARCHAR(64) NOT NULL,
    tier_level INT NOT NULL,

    -- 升级条件
    upgrade_type VARCHAR(32) NOT NULL DEFAULT 'LIFETIME_SPEND',
    upgrade_threshold_cents BIGINT NOT NULL,

    -- 降级规则
    downgrade_enabled BOOLEAN DEFAULT FALSE,
    downgrade_period_months INT DEFAULT 12,
    downgrade_threshold_cents BIGINT NULL,

    -- 权益
    points_multiplier DECIMAL(3,2) DEFAULT 1.00,
    discount_percent INT DEFAULT 0,
    birthday_bonus_points BIGINT DEFAULT 0,
    free_delivery BOOLEAN DEFAULT FALSE,

    -- 展示
    tier_icon VARCHAR(64) NULL,
    tier_color VARCHAR(7) NULL,
    tier_description VARCHAR(255) NULL,

    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_mtr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_mtr UNIQUE (merchant_id, tier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**upgrade_type：** `LIFETIME_SPEND`（累计消费）| `PERIOD_SPEND`（周期消费，如年度）| `VISIT_COUNT`（消费次数）| `MANUAL`（人工指定）

**示例数据：**

| tier_code | tier_name | tier_level | upgrade_threshold | discount | points_multiplier |
|-----------|-----------|------------|-------------------|----------|-------------------|
| STANDARD | 普通会员 | 1 | 0 | 0% | 1.0x |
| SILVER | 银卡会员 | 2 | 50000 ($500) | 5% | 1.2x |
| GOLD | 金卡会员 | 3 | 200000 ($2000) | 8% | 1.5x |
| VIP | VIP 会员 | 4 | 500000 ($5000) | 12% | 2.0x |
| DIAMOND | 钻石会员 | 5 | 1000000 ($10000) | 15% | 3.0x |

---

## 5. 新增表 — 优惠券

### 5.1 coupon_templates（优惠券模板）

```sql
CREATE TABLE coupon_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,

    -- 类型
    coupon_type VARCHAR(32) NOT NULL,

    -- 金额/折扣
    discount_amount_cents BIGINT NULL,
    discount_percent INT NULL,
    min_spend_cents BIGINT DEFAULT 0,
    max_discount_cents BIGINT NULL,

    -- 适用范围
    applicable_stores JSON NULL,
    applicable_categories JSON NULL,
    applicable_skus JSON NULL,
    applicable_dining_modes JSON DEFAULT '["A_LA_CARTE","BUFFET","DELIVERY"]',

    -- 发放控制
    total_quantity INT NULL,
    issued_count INT DEFAULT 0,
    per_member_limit INT DEFAULT 1,

    -- 有效期
    validity_type VARCHAR(32) NOT NULL DEFAULT 'FIXED',
    valid_from TIMESTAMP NULL,
    valid_until TIMESTAMP NULL,
    validity_days INT NULL,

    -- 状态
    template_status VARCHAR(32) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ct_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_ct UNIQUE (merchant_id, template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**coupon_type：** `AMOUNT_OFF`（满减）| `PERCENT_OFF`（折扣）| `FREE_ITEM`（赠菜）| `FREE_DELIVERY`（免运费）

**validity_type：** `FIXED`（固定日期范围）| `ROLLING`（领取后 N 天有效）

### 5.2 member_coupons（会员持有的券）

```sql
CREATE TABLE member_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_no VARCHAR(64) NOT NULL,
    member_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,

    -- 来源
    source_type VARCHAR(32) NOT NULL,
    source_ref VARCHAR(128) NULL,

    -- 有效期（从模板计算或固定）
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,

    -- 使用
    coupon_status VARCHAR(32) DEFAULT 'AVAILABLE',
    used_at TIMESTAMP NULL,
    used_order_id VARCHAR(64) NULL,
    used_store_id BIGINT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mc_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_mc_template FOREIGN KEY (template_id) REFERENCES coupon_templates(id),
    CONSTRAINT uk_mc UNIQUE (coupon_no),
    INDEX idx_mc_member_status (member_id, coupon_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**source_type：** `SYSTEM_GRANT`（系统发放）| `REFERRAL`（推荐奖励）| `PROMOTION`（活动领取）| `BIRTHDAY`（生日自动发）| `RECHARGE`（充值赠送）| `REDEEM`（积分兑换）| `MANUAL`（手动发放）

**coupon_status：** `AVAILABLE`（可用）| `USED`（已使用）| `EXPIRED`（已过期）| `CANCELLED`（已取消）

---

## 6. 新增表 — 储值活动

### 6.1 recharge_campaigns（充值活动/档位）

```sql
CREATE TABLE recharge_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,
    campaign_name VARCHAR(128) NOT NULL,

    -- 充值档位
    recharge_amount_cents BIGINT NOT NULL,
    bonus_amount_cents BIGINT DEFAULT 0,
    bonus_points BIGINT DEFAULT 0,
    bonus_coupon_template_id BIGINT NULL,

    -- 限制
    min_tier_level INT DEFAULT 0,
    max_per_member INT DEFAULT 0,
    total_quota INT DEFAULT 0,
    used_quota INT DEFAULT 0,

    -- 有效期
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    campaign_status VARCHAR(32) DEFAULT 'ACTIVE',

    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_rc_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_rc_coupon FOREIGN KEY (bonus_coupon_template_id) REFERENCES coupon_templates(id),
    CONSTRAINT uk_rc UNIQUE (merchant_id, campaign_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**示例：**

| campaign_name | recharge | bonus_amount | bonus_points | bonus_coupon |
|---------------|----------|-------------|-------------|-------------|
| 充 100 送 10 | 10000 | 1000 | 0 | null |
| 充 500 送 80+券 | 50000 | 8000 | 100 | 满50减10券 |
| 充 1000 送 200+双倍积分 | 100000 | 20000 | 500 | null |

---

## 7. 新增表 — 积分兑换

### 7.1 points_redemption_items（积分兑换商品）

```sql
CREATE TABLE points_redemption_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    item_type VARCHAR(32) NOT NULL,

    -- 兑换价格
    points_required BIGINT NOT NULL,

    -- 兑换内容
    reward_sku_id BIGINT NULL,
    reward_coupon_template_id BIGINT NULL,
    reward_description VARCHAR(255) NULL,

    -- 库存
    total_stock INT NULL,
    redeemed_count INT DEFAULT 0,

    -- 限制
    per_member_limit INT DEFAULT 0,
    min_tier_level INT DEFAULT 0,

    -- 状态
    item_status VARCHAR(32) DEFAULT 'ACTIVE',
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,

    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pri_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_pri_sku FOREIGN KEY (reward_sku_id) REFERENCES skus(id),
    CONSTRAINT fk_pri_coupon FOREIGN KEY (reward_coupon_template_id) REFERENCES coupon_templates(id),
    CONSTRAINT uk_pri UNIQUE (merchant_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**item_type：** `SKU`（兑换菜品）| `COUPON`（兑换优惠券）| `GIFT`（兑换实物礼品）

### 7.2 points_redemption_records（积分兑换记录）

```sql
CREATE TABLE points_redemption_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    redemption_no VARCHAR(64) NOT NULL,
    member_id BIGINT NOT NULL,
    redemption_item_id BIGINT NOT NULL,
    points_spent BIGINT NOT NULL,

    -- 兑换结果
    reward_type VARCHAR(32) NOT NULL,
    reward_ref VARCHAR(128) NULL,

    redemption_status VARCHAR(32) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_prr_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_prr_item FOREIGN KEY (redemption_item_id) REFERENCES points_redemption_items(id),
    CONSTRAINT uk_prr UNIQUE (redemption_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 8. 新增表 — 推荐裂变

### 8.1 referral_rewards_config（推荐奖励规则）

```sql
CREATE TABLE referral_rewards_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,

    -- 推荐人奖励
    referrer_reward_type VARCHAR(32) NOT NULL,
    referrer_reward_points BIGINT DEFAULT 0,
    referrer_reward_coupon_template_id BIGINT NULL,
    referrer_reward_cash_cents BIGINT DEFAULT 0,

    -- 被推荐人奖励
    referee_reward_type VARCHAR(32) NOT NULL,
    referee_reward_points BIGINT DEFAULT 0,
    referee_reward_coupon_template_id BIGINT NULL,
    referee_reward_cash_cents BIGINT DEFAULT 0,

    -- 触发条件
    trigger_event VARCHAR(32) DEFAULT 'FIRST_ORDER',
    min_spend_cents BIGINT DEFAULT 0,

    -- 限制
    max_referrals_per_member INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_rrc_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_rrc UNIQUE (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**trigger_event：** `REGISTRATION`（注册即奖励）| `FIRST_ORDER`（首单后奖励）| `FIRST_SPEND_THRESHOLD`（首单满 X 元后奖励）

### 8.2 referral_records（推荐记录）

```sql
CREATE TABLE referral_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    referrer_member_id BIGINT NOT NULL,
    referee_member_id BIGINT NOT NULL,
    referral_code VARCHAR(32) NOT NULL,

    -- 状态
    referral_status VARCHAR(32) DEFAULT 'PENDING',

    -- 奖励发放
    referrer_rewarded BOOLEAN DEFAULT FALSE,
    referrer_reward_ref VARCHAR(128) NULL,
    referee_rewarded BOOLEAN DEFAULT FALSE,
    referee_reward_ref VARCHAR(128) NULL,

    trigger_order_id VARCHAR(64) NULL,
    triggered_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rr_referrer FOREIGN KEY (referrer_member_id) REFERENCES members(id),
    CONSTRAINT fk_rr_referee FOREIGN KEY (referee_member_id) REFERENCES members(id),
    CONSTRAINT uk_rr_referee UNIQUE (referee_member_id),
    INDEX idx_rr_referrer (referrer_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**referral_status：** `PENDING`（被推荐人已注册，未触发奖励）| `TRIGGERED`（满足条件，奖励已发）| `EXPIRED`（超过有效期未触发）

---

## 9. 新增表 — 会员标签

### 9.1 member_tags（会员标签定义）

```sql
CREATE TABLE member_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    tag_code VARCHAR(64) NOT NULL,
    tag_name VARCHAR(128) NOT NULL,
    tag_type VARCHAR(32) NOT NULL,
    tag_color VARCHAR(7) DEFAULT '#666666',

    -- 自动标签规则（JSON，系统定期计算）
    auto_rule_json JSON NULL,

    is_auto BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mt_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_mt UNIQUE (merchant_id, tag_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**tag_type：** `VALUE`（价值类）| `BEHAVIOR`（行为类）| `LIFECYCLE`（生命周期类）| `CUSTOM`（自定义）

**预设标签：**

| tag_code | tag_name | tag_type | auto_rule |
|----------|----------|----------|-----------|
| HIGH_VALUE | 高价值客户 | VALUE | {"avg_spend_cents":{"gte":5000}} |
| FREQUENT | 常客 | BEHAVIOR | {"visit_count_30d":{"gte":4}} |
| LAPSED | 流失风险 | LIFECYCLE | {"days_since_last_visit":{"gte":30}} |
| NEW | 新客 | LIFECYCLE | {"total_visit_count":{"lte":1}} |
| BIRTHDAY_SOON | 即将生日 | LIFECYCLE | {"birthday_within_days":7} |
| WHALE | 鲸鱼客户 | VALUE | {"lifetime_spend_cents":{"gte":500000}} |
| LUNCH_REGULAR | 午餐常客 | BEHAVIOR | {"preferred_time_slot":"LUNCH"} |
| BUFFET_LOVER | 自助餐爱好者 | BEHAVIOR | {"preferred_dining_mode":"BUFFET"} |

### 9.2 member_tag_assignments（会员-标签关联）

```sql
CREATE TABLE member_tag_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(32) DEFAULT 'SYSTEM',
    expires_at TIMESTAMP NULL,

    CONSTRAINT fk_mta_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_mta_tag FOREIGN KEY (tag_id) REFERENCES member_tags(id),
    CONSTRAINT uk_mta UNIQUE (member_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 10. 新增表 — 消费画像

### 10.1 member_consumption_profiles（消费画像，定期聚合）

```sql
CREATE TABLE member_consumption_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    profile_period VARCHAR(16) NOT NULL,

    -- 消费概况
    order_count INT DEFAULT 0,
    total_spend_cents BIGINT DEFAULT 0,
    avg_spend_cents BIGINT DEFAULT 0,
    max_spend_cents BIGINT DEFAULT 0,

    -- 时段偏好
    preferred_time_slot VARCHAR(32) NULL,
    preferred_day_of_week VARCHAR(16) NULL,

    -- 用餐偏好
    preferred_dining_mode VARCHAR(32) NULL,
    dine_in_count INT DEFAULT 0,
    delivery_count INT DEFAULT 0,
    buffet_count INT DEFAULT 0,

    -- 品类偏好（Top 3 category IDs）
    top_categories_json JSON NULL,

    -- SKU 偏好（Top 5 SKU IDs）
    top_skus_json JSON NULL,

    -- 门店偏好
    preferred_store_id BIGINT NULL,

    -- 支付偏好
    preferred_payment_method VARCHAR(32) NULL,

    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mcp_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_mcp UNIQUE (member_id, profile_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**profile_period：** `ALL_TIME` | `LAST_30D` | `LAST_90D` | `2026_Q1` | `2026_03`

**定期聚合：** 每天凌晨跑批，从 submitted_orders + submitted_order_items 聚合更新。

---

## 11. 新增表 — 营销触达

### 11.1 marketing_campaigns（营销活动）

```sql
CREATE TABLE marketing_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    campaign_code VARCHAR(64) NOT NULL,
    campaign_name VARCHAR(128) NOT NULL,
    campaign_type VARCHAR(32) NOT NULL,

    -- 目标人群
    target_type VARCHAR(32) NOT NULL,
    target_tag_ids JSON NULL,
    target_tier_codes JSON NULL,
    target_filter_json JSON NULL,
    estimated_reach INT DEFAULT 0,

    -- 内容
    message_template TEXT NOT NULL,
    message_channel VARCHAR(32) NOT NULL,

    -- 关联优惠
    coupon_template_id BIGINT NULL,

    -- 排期
    scheduled_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,

    -- 统计
    total_sent INT DEFAULT 0,
    total_delivered INT DEFAULT 0,
    total_opened INT DEFAULT 0,
    total_converted INT DEFAULT 0,

    campaign_status VARCHAR(32) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_mktc_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_mktc_coupon FOREIGN KEY (coupon_template_id) REFERENCES coupon_templates(id),
    CONSTRAINT uk_mktc UNIQUE (merchant_id, campaign_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**campaign_type：** `PROMOTION`（促销推广）| `RECALL`（流失召回）| `BIRTHDAY`（生日关怀）| `NEW_PRODUCT`（新品推荐）| `RECHARGE`（充值活动推广）

**target_type：** `ALL`（全部会员）| `TAG`（按标签）| `TIER`（按等级）| `CUSTOM`（自定义筛选）

**message_channel：** `WHATSAPP` | `SMS` | `EMAIL` | `IN_APP`（站内信）

### 11.2 marketing_send_records（触达发送记录）

```sql
CREATE TABLE marketing_send_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(128) NOT NULL,

    -- 状态
    send_status VARCHAR(32) DEFAULT 'PENDING',
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    opened_at TIMESTAMP NULL,
    converted_at TIMESTAMP NULL,
    converted_order_id VARCHAR(64) NULL,

    -- 外部追踪
    external_message_id VARCHAR(128) NULL,
    error_message VARCHAR(255) NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_msr_campaign FOREIGN KEY (campaign_id) REFERENCES marketing_campaigns(id),
    CONSTRAINT fk_msr_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_msr UNIQUE (campaign_id, member_id),
    INDEX idx_msr_member (member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**send_status：** `PENDING` → `SENT` → `DELIVERED` → `OPENED` → `CONVERTED` | `FAILED` | `BOUNCED`

---

## 12. 完整 CRM ER 关系

```
members (基础信息 + 推荐码 + 画像摘要)
 ├── member_accounts (余额 + 统计)
 ├── member_recharge_orders (充值记录)
 │    └── recharge_campaigns (充值活动档位)
 ├── member_points_ledger (积分流水)
 │    └── points_redemption_records → points_redemption_items (积分兑换)
 ├── member_coupons → coupon_templates (优惠券)
 ├── member_tag_assignments → member_tags (标签)
 ├── member_consumption_profiles (消费画像)
 ├── referral_records (推荐关系)
 │    └── referral_rewards_config (推荐奖励规则)
 └── marketing_send_records → marketing_campaigns (触达记录)

member_tier_rules (等级规则，可配置)
```

---

## 13. 关键业务流程

### 13.1 结账后会员处理

```
结账成功
  ↓
1. 积分计算: spend × tier.points_multiplier → member_points_ledger (EARN)
2. 等级评估: lifetime_spend vs member_tier_rules → 升/降级
3. 消费记录: 更新 members 画像摘要字段
4. 推荐检查: 是否是被推荐人的首单 → 触发 referral_records 奖励
5. 消费画像: 标记需要聚合更新（异步）
6. 标签更新: 标记需要重新计算自动标签（异步）
```

### 13.2 优惠券使用

```
结账时选择用券
  ↓
1. 校验: member_coupons.coupon_status = 'AVAILABLE'
2. 校验: 未过期 (valid_until > now)
3. 校验: 适用门店 (coupon_templates.applicable_stores)
4. 校验: 适用品类/SKU
5. 校验: 满足最低消费 (min_spend_cents)
6. 计算折扣: amount_off 或 percent_off (受 max_discount_cents 限制)
7. 核销: coupon_status = 'USED', used_order_id, used_at
```

### 13.3 积分兑换

```
会员选择兑换商品
  ↓
1. 校验: points_balance >= points_required
2. 校验: 兑换库存 (total_stock - redeemed_count > 0)
3. 校验: 个人限制 (per_member_limit)
4. 扣积分: member_points_ledger (REDEEM)
5. 发放奖励:
   - SKU → 写入订单（免费菜品）
   - COUPON → 创建 member_coupons
   - GIFT → 记录待领取
6. 记录: points_redemption_records
```

### 13.4 自动标签计算（每日跑批）

```
遍历所有 ACTIVE 会员
  ↓
对每个 auto 标签:
  评估 auto_rule_json 条件
  匹配 → 插入 member_tag_assignments (如不存在)
  不匹配 → 删除 member_tag_assignments (如存在)
  ↓
结果: 每个会员身上挂着最新的自动标签
  ↓
营销触达时按标签筛选目标人群
```

---

## 14. 新增表清单

| 表 | 用途 |
|----|------|
| member_tier_rules | 等级规则（可配置升降级条件+权益） |
| coupon_templates | 优惠券模板（类型/金额/范围/有效期） |
| member_coupons | 会员持有的券（发放/使用/过期） |
| recharge_campaigns | 充值活动档位（充X送Y） |
| points_redemption_items | 积分兑换商品 |
| points_redemption_records | 积分兑换记录 |
| referral_rewards_config | 推荐奖励规则 |
| referral_records | 推荐关系记录 |
| member_tags | 会员标签定义（含自动规则） |
| member_tag_assignments | 会员-标签关联 |
| member_consumption_profiles | 消费画像（定期聚合） |
| marketing_campaigns | 营销活动 |
| marketing_send_records | 触达发送记录 |

**13 张新表 + 2 张改动（members, member_accounts）**

**累计：94 (doc/70) + 13 = 107 张表**
