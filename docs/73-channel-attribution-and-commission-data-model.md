# 渠道归因与分润数据模型

**Version:** V20260328009
**Date:** 2026-03-28
**Status:** DRAFT

---

## 1. 业务场景

```
渠道带来客户 → 客户消费 → 渠道拿分润
```

| 渠道类型 | 示例 | 分润模式 |
|---------|------|---------|
| 外卖平台 | Grab、Foodpanda、Deliveroo | 按单抽佣 25-35% |
| 点评/导流平台 | 大众点评、Google Maps、TripAdvisor | 按到店核销 CPC/CPA |
| KOL/达人 | 小红书博主、Instagram 网红 | 按销售额分成或固定佣金 |
| 商场 CRM | 商场会员推送到店 | 按核销笔数或销售额 |
| 会员推荐 | 老带新推荐码 | 积分/储值/现金奖励 |
| 自有渠道 | 官网、公众号、WhatsApp | 无分润 |
| 团购 | 抖音团购、美团团购 | 平台抽佣 + 核销差价 |
| 企业客户 | 企业团餐、企业充值卡 | 协议折扣 + 月结 |

---

## 2. 设计原则

1. **每笔订单记录来源渠道** — 不只是外卖，堂食也要追踪（扫码里带渠道参数）
2. **分润规则可配** — 不同渠道不同比例，同一渠道不同时期可能调整
3. **分润和结算分开** — 先记账（应付），再结算（实付）
4. **支持多层分润** — 一笔订单可能同时涉及平台抽佣 + KOL 分成

---

## 3. 新增表

### 3.1 channels（渠道定义）

```sql
CREATE TABLE channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    channel_code VARCHAR(64) NOT NULL,
    channel_name VARCHAR(128) NOT NULL,
    channel_type VARCHAR(32) NOT NULL,

    -- 联系信息
    contact_name VARCHAR(128) NULL,
    contact_phone VARCHAR(64) NULL,
    contact_email VARCHAR(255) NULL,

    -- 追踪配置
    tracking_param VARCHAR(64) NULL,
    tracking_url_prefix VARCHAR(512) NULL,

    -- 状态
    channel_status VARCHAR(32) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ch_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_ch UNIQUE (merchant_id, channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**channel_type：** `DELIVERY_PLATFORM`（外卖平台）| `REVIEW_PLATFORM`（点评平台）| `KOL`（达人/博主）| `MALL_CRM`（商场 CRM）| `REFERRAL`（会员推荐）| `OWN`（自有渠道）| `GROUP_BUY`（团购）| `ENTERPRISE`（企业客户）

**tracking_param：** QR 扫码 URL 里带的参数名，如 `?ch=GRAB` 或 `?ref=KOL_JANE`

**示例数据：**

| channel_code | channel_name | channel_type | tracking_param |
|-------------|-------------|-------------|----------------|
| GRAB | Grab Food | DELIVERY_PLATFORM | — |
| FOODPANDA | Foodpanda | DELIVERY_PLATFORM | — |
| DIANPING | 大众点评 | REVIEW_PLATFORM | ch |
| KOL_JANE | Jane Wang 小红书 | KOL | ref |
| MALL_JEWEL | Jewel 商场 CRM | MALL_CRM | mall |
| TIKTOK_GP | 抖音团购 | GROUP_BUY | gp |
| CORP_DBS | DBS 企业团餐 | ENTERPRISE | corp |
| OWN_WECHAT | 微信公众号 | OWN | — |

### 3.2 channel_commission_rules（渠道分润规则）

```sql
CREATE TABLE channel_commission_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,

    -- 分润模式
    commission_type VARCHAR(32) NOT NULL,
    commission_rate_percent DECIMAL(5,2) NULL,
    commission_fixed_cents BIGINT NULL,

    -- 阶梯分润（可选）
    tier_rules_json JSON NULL,

    -- 计算基数
    calculation_base VARCHAR(32) DEFAULT 'NET_SALES',

    -- 适用范围
    applicable_stores JSON NULL,
    applicable_categories JSON NULL,
    applicable_dining_modes JSON NULL,

    -- 上下限
    min_commission_cents BIGINT NULL,
    max_commission_cents BIGINT NULL,

    -- 有效期
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    rule_status VARCHAR(32) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ccr_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT uk_ccr UNIQUE (channel_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**commission_type：**

| commission_type | 含义 | 示例 |
|----------------|------|------|
| `PERCENTAGE` | 按比例 | Grab 抽 30% |
| `FIXED_PER_ORDER` | 每单固定 | 大众点评每核销 1 单收 5 元 |
| `FIXED_PER_CUSTOMER` | 每个到店客户 | KOL 每带来 1 人给 10 元 |
| `TIERED_PERCENTAGE` | 阶梯比例 | 月销 <1万: 25%, 1-5万: 20%, >5万: 15% |
| `TIERED_FIXED` | 阶梯固定 | 月销 <100 单: 8元/单, >100 单: 5元/单 |

**calculation_base：**
- `GROSS_SALES` — 按原价计算（优惠前）
- `NET_SALES` — 按实收计算（优惠后）
- `NET_AFTER_TAX` — 按税后净额

**tier_rules_json 示例（阶梯分润）：**

```json
[
  {"min_amount_cents": 0, "max_amount_cents": 1000000, "rate_percent": 30},
  {"min_amount_cents": 1000000, "max_amount_cents": 5000000, "rate_percent": 25},
  {"min_amount_cents": 5000000, "max_amount_cents": null, "rate_percent": 20}
]
```

### 3.3 order_channel_attribution（订单渠道归因）

```sql
CREATE TABLE order_channel_attribution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submitted_order_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,

    -- 归因来源
    attribution_type VARCHAR(32) NOT NULL,
    tracking_value VARCHAR(255) NULL,
    landing_url VARCHAR(512) NULL,

    -- 归因时间
    first_touch_at TIMESTAMP NULL,
    conversion_at TIMESTAMP NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_oca_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    CONSTRAINT fk_oca_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT uk_oca UNIQUE (submitted_order_id, channel_id),
    INDEX idx_oca_channel (channel_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**attribution_type：**

| attribution_type | 含义 | 示例 |
|-----------------|------|------|
| `DIRECT` | 外卖平台直接订单 | Grab 订单自动归因 |
| `QR_PARAM` | 扫码 URL 带渠道参数 | `?ch=DIANPING` |
| `REFERRAL_CODE` | 推荐码 | `?ref=KOL_JANE` |
| `COUPON` | 通过特定渠道券到店 | 大众点评券核销 |
| `GROUP_BUY_CODE` | 团购码核销 | 抖音团购验券 |
| `MANUAL` | 手工标记 | 收银员选择渠道来源 |

**一笔订单可以有多个渠道归因**（例如：通过 KOL 链接来 + 用了大众点评的券）。分润时每个渠道各算各的。

### 3.4 channel_commission_records（分润记账）

```sql
CREATE TABLE channel_commission_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commission_no VARCHAR(64) NOT NULL,
    channel_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    submitted_order_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,

    -- 订单金额
    order_amount_cents BIGINT NOT NULL,
    calculation_base_cents BIGINT NOT NULL,

    -- 分润计算
    commission_type VARCHAR(32) NOT NULL,
    commission_rate_percent DECIMAL(5,2) NULL,
    commission_fixed_cents BIGINT NULL,
    commission_amount_cents BIGINT NOT NULL,

    -- 状态
    commission_status VARCHAR(32) DEFAULT 'PENDING',

    -- 结算关联
    settlement_batch_id BIGINT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ccrd_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT fk_ccrd_rule FOREIGN KEY (rule_id) REFERENCES channel_commission_rules(id),
    CONSTRAINT fk_ccrd_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    CONSTRAINT uk_ccrd UNIQUE (commission_no),
    INDEX idx_ccrd_channel_status (channel_id, commission_status),
    INDEX idx_ccrd_store (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**commission_status：** `PENDING`（待结算）| `CONFIRMED`（已确认）| `SETTLED`（已结算）| `DISPUTED`（有争议）| `CANCELLED`（已取消）

### 3.5 channel_settlement_batches（渠道结算批次）

定期把分润记录汇总成结算单，和渠道方对账：

```sql
CREATE TABLE channel_settlement_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL,
    channel_id BIGINT NOT NULL,
    store_id BIGINT NULL,

    -- 结算周期
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,

    -- 汇总
    total_orders INT DEFAULT 0,
    total_order_amount_cents BIGINT DEFAULT 0,
    total_commission_cents BIGINT DEFAULT 0,

    -- 调整
    adjustment_cents BIGINT DEFAULT 0,
    adjustment_reason VARCHAR(255) NULL,
    final_settlement_cents BIGINT DEFAULT 0,

    -- 状态
    batch_status VARCHAR(32) DEFAULT 'DRAFT',
    confirmed_at TIMESTAMP NULL,
    confirmed_by BIGINT NULL,
    paid_at TIMESTAMP NULL,
    paid_by BIGINT NULL,
    payment_ref VARCHAR(128) NULL,

    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_csb_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT uk_csb UNIQUE (batch_no),
    INDEX idx_csb_channel_period (channel_id, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**batch_status：** `DRAFT` → `CONFIRMED` → `PAID` | `DISPUTED`

**结算流程：**

```
每月 1 号自动生成上月结算批次
  ↓
汇总: channel_commission_records WHERE commission_status = 'PENDING'
     AND created_at BETWEEN period_start AND period_end
  ↓
生成: channel_settlement_batches
  total_orders, total_order_amount, total_commission
  ↓
财务审核: 可加 adjustment（调整金额，如扣回争议单）
  final_settlement = total_commission + adjustment
  ↓
确认 → 付款 → 标记 PAID
  ↓
对应 commission_records 全部标记 SETTLED
```

### 3.6 channel_performance_daily（渠道每日表现，聚合表）

```sql
CREATE TABLE channel_performance_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    report_date DATE NOT NULL,

    -- 流量
    impressions INT DEFAULT 0,
    clicks INT DEFAULT 0,
    unique_visitors INT DEFAULT 0,

    -- 转化
    orders INT DEFAULT 0,
    new_customers INT DEFAULT 0,
    returning_customers INT DEFAULT 0,

    -- 金额
    gross_sales_cents BIGINT DEFAULT 0,
    net_sales_cents BIGINT DEFAULT 0,
    commission_cents BIGINT DEFAULT 0,
    profit_after_commission_cents BIGINT DEFAULT 0,

    -- ROI
    cost_per_order_cents BIGINT DEFAULT 0,
    customer_acquisition_cost_cents BIGINT DEFAULT 0,
    roi_percent DECIMAL(6,2) DEFAULT 0,

    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cpd_channel FOREIGN KEY (channel_id) REFERENCES channels(id),
    CONSTRAINT uk_cpd UNIQUE (channel_id, store_id, report_date),
    INDEX idx_cpd_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**每日跑批聚合，给老板看：**

```
渠道表现看板:
  Grab:     本月 120 单, 营业额 $8,500, 佣金 $2,550, 净利 $5,950
  大众点评: 本月 45 单,  营业额 $3,200, 佣金 $225,   净利 $2,975
  KOL Jane: 本月 30 单,  营业额 $2,100, 佣金 $315,   净利 $1,785
  自然流量: 本月 200 单, 营业额 $14,000, 佣金 $0,    净利 $14,000
  ↓
  结论: 自然流量最赚钱，Grab 佣金太高考虑降依赖
```

---

## 4. 订单归因流程

### 4.1 堂食扫码（自动归因）

```
顾客扫桌码: http://pos.example.com/qr/S001/T01?ch=DIANPING
  ↓
前端解析 URL 参数: ch = DIANPING
  ↓
创建 table_session 时记录 channel_code
  ↓
提交订单时自动写入 order_channel_attribution:
  attribution_type = QR_PARAM
  tracking_value = DIANPING
```

### 4.2 外卖平台（自动归因）

```
Grab 订单推送到系统
  ↓
自动归因: channel = GRAB, attribution_type = DIRECT
  ↓
自动计算分润: commission_rules → commission_records
```

### 4.3 KOL 推荐（链接归因）

```
KOL 分享链接: http://pos.example.com/menu?ref=KOL_JANE
  ↓
顾客点击 → 注册会员 / 到店扫码
  ↓
系统记录 first_touch channel = KOL_JANE
  ↓
消费后归因 + 分润
```

### 4.4 团购核销（券码归因）

```
顾客在抖音买了团购券
  ↓
到店出示券码
  ↓
收银员扫码核销 → 系统识别来源 = TIKTOK_GP
  ↓
归因 + 分润
```

### 4.5 手动标记

```
顾客到店说"我是朋友介绍来的"
  ↓
收银员在 POS 上选择渠道来源 = 会员推荐
  ↓
归因 attribution_type = MANUAL
```

---

## 5. 需要改动的现有表

### 5.1 table_sessions — 加渠道追踪

```sql
ALTER TABLE table_sessions ADD COLUMN channel_code VARCHAR(64) NULL AFTER buffet_status;
ALTER TABLE table_sessions ADD COLUMN tracking_value VARCHAR(255) NULL AFTER channel_code;
```

### 5.2 submitted_orders — 加渠道快照

```sql
ALTER TABLE submitted_orders ADD COLUMN channel_code VARCHAR(64) NULL AFTER delivery_contact_phone;
```

---

## 6. 分润计算触发点

```
结账成功 (Settlement Hook)
  ↓
1. 查 order_channel_attribution → 有归因？
     ↓ yes
2. 对每个归因渠道:
   查 channel_commission_rules (ACTIVE, 在有效期内)
     ↓
3. 计算分润:
   PERCENTAGE: commission = calculation_base × rate / 100
   FIXED_PER_ORDER: commission = fixed_cents
   TIERED: 查当月累计 → 匹配阶梯 → 用对应 rate
     ↓
4. 检查 min/max 限制
     ↓
5. 写入 channel_commission_records (PENDING)
     ↓
6. 每月汇总 → channel_settlement_batches
     ↓
7. 财务审核 → 付款
```

---

## 7. 完整 ER 关系

```
channels (渠道定义)
 ├── channel_commission_rules (分润规则)
 ├── channel_commission_records → submitted_orders (每单分润记录)
 │    └── channel_settlement_batches (月度结算)
 ├── order_channel_attribution → submitted_orders (订单归因)
 └── channel_performance_daily (每日表现聚合)

table_sessions (+channel_code)
submitted_orders (+channel_code)
```

---

## 8. 新增表清单

| 表 | 用途 |
|----|------|
| channels | 渠道定义（外卖平台/KOL/商场/团购...） |
| channel_commission_rules | 分润规则（比例/固定/阶梯） |
| order_channel_attribution | 订单渠道归因 |
| channel_commission_records | 每单分润记账 |
| channel_settlement_batches | 月度结算批次 |
| channel_performance_daily | 渠道每日表现（ROI 分析） |

**6 张新表 + 2 张改动（table_sessions, submitted_orders）**

**累计：113 (doc/72) + 6 = 119 张表**
