# 积分与储值拆分数据模型

**Version:** V20260328008
**Date:** 2026-03-28
**Status:** DRAFT

---

## 1. 为什么拆

积分和储值是两套独立的货币系统，规则完全不同：

| | 积分 | 储值 |
|---|------|------|
| 本质 | 虚拟奖励 | 预付款余额 |
| 获取 | 消费赚、活动送、签到、推荐奖励、评价奖励 | 充钱 |
| 使用 | 兑换（菜品/券）、抵扣（按比例折现） | 直接当钱花 |
| 过期 | 有过期规则（年底/N月后清零） | 不过期 |
| 冻结 | 可冻结（兑换中/争议中） | 可冻结（退款争议） |
| 退回 | 退单退积分 | 退单退余额 |
| 汇率 | 100积分=1元（可配） | 1分=1分 |
| 规则 | 多倍积分日、品类加倍、等级加倍、上限 | 充X送Y |

混在一张表里会导致规则互相干扰，流水无法独立审计。

---

## 2. 改造 member_accounts

保留为**余额汇总表**（只存当前余额，不存规则）：

```sql
-- 删掉旧的混合字段，改为清晰分区
ALTER TABLE member_accounts ADD COLUMN available_points BIGINT DEFAULT 0 AFTER points_balance;
ALTER TABLE member_accounts ADD COLUMN frozen_points BIGINT DEFAULT 0 AFTER available_points;
ALTER TABLE member_accounts ADD COLUMN expired_points BIGINT DEFAULT 0 AFTER frozen_points;
ALTER TABLE member_accounts ADD COLUMN available_cash_cents BIGINT DEFAULT 0 AFTER cash_balance_cents;
ALTER TABLE member_accounts ADD COLUMN frozen_cash_cents BIGINT DEFAULT 0 AFTER available_cash_cents;
```

**最终 member_accounts 字段：**

```
points_balance           ← 积分总余额 = available + frozen
available_points         ← 可用积分
frozen_points            ← 冻结积分（兑换中/争议中）
expired_points           ← 累计过期积分
cash_balance_cents       ← 储值总余额 = available + frozen
available_cash_cents     ← 可用储值
frozen_cash_cents        ← 冻结储值
lifetime_spend_cents     ← 累计消费
lifetime_recharge_cents  ← 累计充值
total_points_earned      ← 累计获得积分
total_points_redeemed    ← 累计使用积分
```

---

## 3. 积分体系

### 3.1 points_rules（积分赚取规则）

```sql
CREATE TABLE points_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,

    -- 赚取规则
    points_per_dollar INT DEFAULT 1,
    bonus_multiplier DECIMAL(3,2) DEFAULT 1.00,
    fixed_points BIGINT NULL,
    min_spend_cents BIGINT DEFAULT 0,
    max_points_per_order BIGINT NULL,
    max_points_per_day BIGINT NULL,

    -- 适用范围
    applicable_tiers JSON NULL,
    applicable_stores JSON NULL,
    applicable_categories JSON NULL,
    applicable_dining_modes JSON NULL,
    applicable_days JSON NULL,
    applicable_time_slots JSON NULL,

    -- 有效期
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    rule_status VARCHAR(32) DEFAULT 'ACTIVE',
    priority INT DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_pr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_pr UNIQUE (merchant_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**rule_type：**

| rule_type | 含义 | 示例 |
|-----------|------|------|
| `SPEND` | 消费赚积分 | 每消费 1 元得 1 积分 |
| `BONUS_MULTIPLIER` | 多倍积分 | Gold 会员双倍积分 |
| `CATEGORY_BONUS` | 品类加倍 | 饮品消费 3 倍积分 |
| `FIRST_ORDER` | 首单奖励 | 首单额外送 100 积分 |
| `BIRTHDAY` | 生日奖励 | 生日当天 5 倍积分 |
| `TIME_BONUS` | 时段奖励 | 下午茶时段双倍积分 |
| `DAY_BONUS` | 特定日期 | 周三会员日 3 倍 |
| `REVIEW` | 评价奖励 | 写评价送 50 积分 |
| `CHECKIN` | 签到 | 每日签到送 10 积分 |
| `REFERRAL` | 推荐奖励 | 推荐新客送 200 积分 |

**积分计算逻辑：**

```
基础积分 = spend_cents / 100 × points_per_dollar
  ↓
匹配所有 ACTIVE 规则（按 priority 排序）
  ↓
叠加 bonus_multiplier（等级加倍 × 品类加倍 × 时段加倍）
  ↓
检查 max_points_per_order 上限
  ↓
检查 max_points_per_day 上限（查当天已赚积分）
  ↓
最终积分 = min(计算结果, 每单上限, 每日剩余额度)
```

### 3.2 points_expiry_rules（积分过期规则）

```sql
CREATE TABLE points_expiry_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,

    expiry_type VARCHAR(32) NOT NULL DEFAULT 'ROLLING',
    expiry_months INT DEFAULT 12,
    year_end_clear BOOLEAN DEFAULT FALSE,
    year_end_clear_month INT DEFAULT 12,
    year_end_clear_day INT DEFAULT 31,
    grace_period_days INT DEFAULT 30,
    notify_before_days INT DEFAULT 7,

    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_per_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_per UNIQUE (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**expiry_type：**

| expiry_type | 含义 | 示例 |
|-------------|------|------|
| `NEVER` | 永不过期 | — |
| `ROLLING` | 获得后 N 个月过期 | 获得后 12 个月 |
| `YEAR_END` | 每年固定日期清零 | 每年 12/31 清零 |
| `YEAR_END_WITH_GRACE` | 固定日期 + 宽限期 | 12/31 到期，宽限到 1/31 |

### 3.3 points_batches（积分批次 — 支持分批过期）

```sql
CREATE TABLE points_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,

    -- 来源
    source_type VARCHAR(32) NOT NULL,
    source_ref VARCHAR(128) NULL,
    rule_id BIGINT NULL,

    -- 数量
    original_points BIGINT NOT NULL,
    remaining_points BIGINT NOT NULL,
    used_points BIGINT DEFAULT 0,
    expired_points BIGINT DEFAULT 0,

    -- 过期
    earned_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    expired_at TIMESTAMP NULL,

    batch_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pb_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_pb UNIQUE (batch_no),
    INDEX idx_pb_member_expiry (member_id, expires_at, batch_status),
    INDEX idx_pb_expiry (expires_at, batch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**batch_status：** `ACTIVE` | `DEPLETED`（用完）| `EXPIRED`（过期）

**FIFO 使用：** 使用积分时按 `expires_at ASC` 排序，先到期的先扣（和库存批次一样的逻辑）。

**source_type：** `SPEND`（消费赚）| `BONUS`（活动送）| `BIRTHDAY`（生日送）| `REFERRAL`（推荐奖励）| `REVIEW`（评价奖励）| `CHECKIN`（签到）| `MANUAL`（手动调整）| `REFUND_RETURN`（退单退回）

### 3.4 points_deduction_rules（积分抵扣规则）

```sql
CREATE TABLE points_deduction_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,

    -- 抵扣汇率
    points_per_cent INT NOT NULL DEFAULT 10,

    -- 限制
    max_deduction_percent INT DEFAULT 50,
    min_points_to_deduct BIGINT DEFAULT 100,
    min_order_cents BIGINT DEFAULT 0,

    -- 适用范围
    applicable_stores JSON NULL,
    applicable_dining_modes JSON NULL,
    excluded_categories JSON NULL,
    excluded_skus JSON NULL,

    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_pdr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_pdr UNIQUE (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**字段说明：**
- `points_per_cent = 10` → 10 积分 = 1 分钱（即 1000 积分 = 1 元）
- `max_deduction_percent = 50` → 最多用积分抵扣订单金额的 50%
- `min_points_to_deduct = 100` → 至少攒到 100 积分才能用
- `excluded_categories` → 某些品类不能用积分抵扣（如烟酒）

**抵扣计算：**

```
顾客选择积分抵扣
  ↓
可抵扣上限 = order_payable × max_deduction_percent / 100
需要积分 = 可抵扣上限 × points_per_cent
实际使用 = min(需要积分, available_points)
实际抵扣金额 = 实际使用 / points_per_cent
  ↓
FIFO 扣减 points_batches
写入 member_points_ledger (DEDUCT)
```

---

## 4. 储值体系

### 4.1 member_cash_ledger（储值流水）

与 `member_points_ledger` 对称的储值流水表：

```sql
CREATE TABLE member_cash_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,

    -- 变动
    change_type VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    balance_after_cents BIGINT NOT NULL,

    -- 来源
    source_type VARCHAR(64) NOT NULL,
    source_ref VARCHAR(128) NULL,

    -- 操作
    operator_type VARCHAR(32) DEFAULT 'SYSTEM',
    operator_id VARCHAR(64) NULL,
    notes VARCHAR(255) NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mcl_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uk_mcl UNIQUE (ledger_no),
    INDEX idx_mcl_member (member_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**change_type：**

| change_type | 含义 | amount_cents |
|-------------|------|-------------|
| `RECHARGE` | 充值 | +50000 |
| `BONUS` | 充值赠送 | +8000 |
| `PAYMENT` | 储值支付 | -3500 |
| `REFUND` | 退款退回 | +3500 |
| `FREEZE` | 冻结 | -金额（从 available 移到 frozen） |
| `UNFREEZE` | 解冻 | +金额（从 frozen 移回 available） |
| `ADJUSTMENT` | 手动调整 | ±金额 |
| `TRANSFER_IN` | 转入（合并账户） | +金额 |
| `TRANSFER_OUT` | 转出 | -金额 |
| `EXPIRY` | 过期清零（如果启用） | -金额 |

### 4.2 cash_balance_rules（储值使用规则）

```sql
CREATE TABLE cash_balance_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,

    -- 使用限制
    min_balance_to_use_cents BIGINT DEFAULT 0,
    max_payment_percent INT DEFAULT 100,
    allow_partial_payment BOOLEAN DEFAULT TRUE,

    -- 适用范围
    applicable_stores JSON NULL,
    applicable_dining_modes JSON NULL,

    -- 提现（如果允许）
    allow_withdrawal BOOLEAN DEFAULT FALSE,
    withdrawal_fee_percent INT DEFAULT 0,
    min_withdrawal_cents BIGINT DEFAULT 0,

    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_cbr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_cbr UNIQUE (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**字段说明：**
- `max_payment_percent = 100` → 储值可以付 100%（全额储值支付）
- `allow_partial_payment = true` → 储值不够可以混合支付（储值 + 现金/卡）
- `allow_withdrawal = false` → 不允许提现（大部分餐饮不允许）

---

## 5. 改造 member_points_ledger

现有表保留，加字段关联积分批次：

```sql
ALTER TABLE member_points_ledger ADD COLUMN batch_id BIGINT NULL AFTER source_ref;
ALTER TABLE member_points_ledger ADD COLUMN rule_id BIGINT NULL AFTER batch_id;
ALTER TABLE member_points_ledger ADD COLUMN expires_at TIMESTAMP NULL AFTER rule_id;
```

---

## 6. 积分 + 储值 在结账中的使用流程

```
结账页面
  ↓
1. 显示当前可用积分 (available_points) 和储值 (available_cash_cents)
2. 顾客选择:
   □ 使用积分抵扣 → 输入使用积分数（或"全部使用"）
   □ 使用储值支付 → 输入使用金额（或"全部使用"）
  ↓
3. 计算:
   order_payable = 5000 (50元)

   积分抵扣:
     可用 2000 积分, 汇率 10积分=1分
     → 最多抵 2000/10 = 200分 = 2元
     → 上限 50% = 2500分
     → 实际抵扣 200分 = 2元
     → order_after_points = 4800

   储值支付:
     可用 10000分 = 100元
     → 顾客选择用储值付剩余 4800分 = 48元
     → order_after_cash = 0

   剩余 = 0 → 无需其他支付
  ↓
4. 确认结账:
   - 扣积分: FIFO 扣 points_batches, 写 member_points_ledger (DEDUCT)
   - 扣储值: 写 member_cash_ledger (PAYMENT)
   - 更新 member_accounts 余额
   - 结算记录: payment_method = "POINTS+STORED_VALUE"
  ↓
5. 结账完成后:
   - 消费赚积分: 按 points_rules 计算新积分（基于实际支付金额还是原价，可配）
   - 新积分入 points_batches
   - 写 member_points_ledger (EARN)
```

---

## 7. 积分过期自动处理（每日跑批）

```
每天凌晨:
  ↓
SELECT * FROM points_batches
WHERE batch_status = 'ACTIVE'
AND expires_at IS NOT NULL
AND expires_at < NOW()
  ↓
对每个到期批次:
  expired_amount = remaining_points
  batch_status = 'EXPIRED'
  expired_points = remaining_points
  remaining_points = 0
  ↓
  写 member_points_ledger (EXPIRE, -expired_amount)
  更新 member_accounts (points_balance -= expired_amount, expired_points += expired_amount)
  ↓
提前通知（notify_before_days）:
  SELECT * FROM points_batches
  WHERE expires_at BETWEEN NOW() AND NOW() + INTERVAL notify_days DAY
  AND batch_status = 'ACTIVE'
  AND remaining_points > 0
  → 生成触达任务（WhatsApp/SMS: "您有 X 积分将于 Y 天后过期"）
```

---

## 8. 完整 ER 关系

```
members
 └── member_accounts (积分余额 + 储值余额，分 available/frozen)
      ├── points_batches (积分分批，FIFO 过期)
      │    └── points_rules (赚取规则)
      │    └── points_expiry_rules (过期规则)
      │    └── points_deduction_rules (抵扣规则)
      ├── member_points_ledger (积分流水, +batch_id)
      ├── member_cash_ledger (储值流水) ← NEW
      │    └── cash_balance_rules (储值使用规则)
      └── member_recharge_orders → recharge_campaigns
```

---

## 9. 新增表清单

| 表 | 用途 |
|----|------|
| points_rules | 积分赚取规则（消费/多倍/品类/时段/生日...） |
| points_expiry_rules | 积分过期规则（滚动/年底清零/宽限期） |
| points_batches | 积分批次（分批过期，FIFO 使用） |
| points_deduction_rules | 积分抵扣规则（汇率/上限/排除品类） |
| member_cash_ledger | 储值流水 |
| cash_balance_rules | 储值使用规则（限额/提现/混合支付） |

**6 张新表 + 2 张改动（member_accounts, member_points_ledger）**

**累计：107 (doc/71) + 6 = 113 张表**
