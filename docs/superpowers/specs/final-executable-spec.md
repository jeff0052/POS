# FounderPOS V3 — 最终可执行 Spec

**Version:** V20260328014
**Date:** 2026-03-28
**Status:** FINAL — 可开工版本
**废弃前置文档:** design-v2-post-review.md, sprint-plan-complete.md, 17-gaps-phase1-buffet-design.md（均被本文档取代）
**审计轮次:** Round 1 + Round 2 修复全部纳入

---

## 0. 真实系统现状速查

本节列出设计依赖的**真实字段名和状态值**，所有 DDL 必须引用这些而非文档中的概念名。

| 表 | 真实字段/状态 | 注意 |
|---|---|---|
| settlement_records | `payable_amount_cents`, `collected_amount_cents` | **不存在** total_amount_cents |
| settlement_records.active_order_id | VARCHAR — 存 `session.getSessionId()` | 不是 order ID |
| table_sessions.session_status | `OPEN` / `CLOSED` | **不存在** SETTLED |
| payment_attempts.attempt_status | `PENDING_CUSTOMER` / `SUCCEEDED` / `SETTLED` / `FAILED` / `EXPIRED` | 保留全部 |
| member_accounts | `points_balance`, `cash_balance_cents` | **不存在** available_points |
| active_table_orders | UK: `(store_id, table_id)` — 每桌只一个 draft | |
| active_table_order_items | FK: `active_order_id` → active_table_orders.id | **不存在** session_id |
| submitted_orders | FK: `table_session_id` → table_sessions.id | 结算聚合点 |
| store_tables.table_status | 结算后代码写 `AVAILABLE` | 目前不经过 PENDING_CLEAN |
| action_log | MCP/AI 工具日志 — tool_name, params_json | 无 store_id |
| member_coupons.coupon_status | `AVAILABLE` / `USED` / `EXPIRED` | **不存在** LOCKED |
| collectForTable() | memberId 从 submitted_orders.member_id 取 | 不接受外部传入 |

---

## 1. Flyway Migrations（V070–V095，连续无间隔）

### V070__alter_table_sessions_merge.sql

```sql
ALTER TABLE table_sessions
  ADD COLUMN merged_into_session_id BIGINT NULL
    COMMENT '被并入的主桌 session id' AFTER session_status,
  ADD COLUMN guest_count INT NOT NULL DEFAULT 1
    COMMENT '本桌开台人数' AFTER merged_into_session_id,
  ADD INDEX idx_ts_merged (merged_into_session_id);
```

### V071__create_qr_tokens.sql

> **SUPERSEDED**: 本 spec 原方案为 `store_tables.qr_token` + `stores.jwt_secret`。
> 实际采用 `qr_tokens` 独立表方案（见 docs/80 D4 决策）。
> QR 签发/校验/旋转逻辑以 `qr_tokens` 表为准。

```sql
-- 见 V071__create_qr_tokens.sql（已在 DB 中）
-- store_tables 的 zone/min_guests/max_guests 列已在 V067 中通过 ALTER 添加
```

### V072__create_table_merge_records.sql

```sql
CREATE TABLE table_merge_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    master_session_id BIGINT NOT NULL,
    merged_session_id BIGINT NOT NULL,
    master_table_id BIGINT NOT NULL,
    merged_table_id BIGINT NOT NULL,
    guest_count_at_merge INT NOT NULL,
    merged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unmerged_at TIMESTAMP NULL,
    operated_by BIGINT NOT NULL,
    reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tmr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_tmr_master_session FOREIGN KEY (master_session_id) REFERENCES table_sessions(id),
    CONSTRAINT fk_tmr_merged_session FOREIGN KEY (merged_session_id) REFERENCES table_sessions(id),
    INDEX idx_tmr_master (master_session_id),
    INDEX idx_tmr_merged (merged_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V073__create_audit_trail.sql

```sql
CREATE TABLE audit_trail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    actor_display_name VARCHAR(128) NULL,
    actor_ip VARCHAR(64) NULL,
    actor_device VARCHAR(128) NULL,
    action VARCHAR(128) NOT NULL,
    action_category VARCHAR(64) NOT NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW',
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    target_description VARCHAR(255) NULL,
    before_snapshot JSON NULL,
    after_snapshot JSON NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    approval_status VARCHAR(32) NULL COMMENT 'PENDING|APPROVED|REJECTED',
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    approval_notes VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_at_store (store_id, created_at),
    INDEX idx_at_actor (actor_user_id, created_at),
    INDEX idx_at_risk (risk_level, created_at),
    INDEX idx_at_target (target_type, target_id),
    INDEX idx_at_approval (requires_approval, approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- action_log 不动，保持原样作为 MCP/AI 工具日志
```

### V074__alter_order_items_buffet.sql

```sql
ALTER TABLE active_table_order_items
  ADD COLUMN is_buffet_included BOOLEAN NOT NULL DEFAULT FALSE AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT NOT NULL DEFAULT 0 AFTER is_buffet_included,
  ADD COLUMN buffet_package_id BIGINT NULL AFTER buffet_surcharge_cents;

ALTER TABLE submitted_order_items
  ADD COLUMN is_buffet_included BOOLEAN NOT NULL DEFAULT FALSE AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT NOT NULL DEFAULT 0 AFTER is_buffet_included,
  ADD COLUMN buffet_package_id BIGINT NULL AFTER buffet_surcharge_cents;
```

### V075__alter_table_sessions_buffet.sql

```sql
ALTER TABLE table_sessions
  ADD COLUMN dining_mode VARCHAR(32) NOT NULL DEFAULT 'A_LA_CARTE' AFTER guest_count,
  ADD COLUMN child_count INT NOT NULL DEFAULT 0 AFTER dining_mode,
  ADD COLUMN buffet_package_id BIGINT NULL AFTER child_count,
  ADD COLUMN buffet_started_at TIMESTAMP NULL AFTER buffet_package_id,
  ADD COLUMN buffet_ends_at TIMESTAMP NULL AFTER buffet_started_at,
  ADD COLUMN buffet_status VARCHAR(32) NULL AFTER buffet_ends_at,
  ADD COLUMN buffet_overtime_minutes INT NOT NULL DEFAULT 0 AFTER buffet_status,
  ADD INDEX idx_ts_dining (store_id, dining_mode),
  ADD INDEX idx_ts_buffet (store_id, buffet_status);
```

### V076__create_payment_stacking_rules.sql

```sql
CREATE TABLE payment_stacking_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    rule_name VARCHAR(128) NOT NULL,
    allow_points_deduct BOOLEAN NOT NULL DEFAULT TRUE,
    allow_cash_balance BOOLEAN NOT NULL DEFAULT TRUE,
    allow_coupon BOOLEAN NOT NULL DEFAULT TRUE,
    allow_mixed_payment BOOLEAN NOT NULL DEFAULT TRUE,
    points_priority INT NOT NULL DEFAULT 1,
    coupon_priority INT NOT NULL DEFAULT 2,
    cash_balance_priority INT NOT NULL DEFAULT 3,
    external_payment_priority INT NOT NULL DEFAULT 4,
    max_points_deduct_percent INT NOT NULL DEFAULT 50,
    points_to_cents_rate INT NOT NULL DEFAULT 100,
    min_points_deduct BIGINT NOT NULL DEFAULT 0,
    max_cash_balance_percent INT NOT NULL DEFAULT 100,
    max_coupons_per_order INT NOT NULL DEFAULT 1,
    coupon_stackable_with_promotion BOOLEAN NOT NULL DEFAULT FALSE,
    applicable_dining_modes JSON NOT NULL DEFAULT '["A_LA_CARTE","BUFFET","DELIVERY"]',
    applicable_order_min_cents BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_psr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_psr_lookup (merchant_id, store_id, is_active, priority DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V077__create_settlement_payment_holds.sql

```sql
CREATE TABLE settlement_payment_holds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    table_session_id BIGINT NOT NULL,
    stacking_rule_id BIGINT NULL,
    step_order INT NOT NULL,
    hold_type VARCHAR(32) NOT NULL COMMENT 'POINTS|COUPON|CASH_BALANCE|EXTERNAL',
    hold_amount_cents BIGINT NOT NULL,
    points_held BIGINT NULL,
    coupon_id BIGINT NULL,
    member_id BIGINT NULL,
    hold_status VARCHAR(32) NOT NULL DEFAULT 'HELD' COMMENT 'HELD|CONFIRMED|RELEASED',
    held_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    released_at TIMESTAMP NULL,
    release_reason VARCHAR(255) NULL,
    payment_attempt_id BIGINT NULL,
    settlement_record_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sph_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_sph_session FOREIGN KEY (table_session_id) REFERENCES table_sessions(id),
    INDEX idx_sph_session (table_session_id, hold_status),
    INDEX idx_sph_member (member_id, hold_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V078__alter_member_accounts_frozen.sql

```sql
ALTER TABLE member_accounts
  ADD COLUMN frozen_points BIGINT NOT NULL DEFAULT 0 AFTER points_balance,
  ADD COLUMN frozen_cash_cents BIGINT NOT NULL DEFAULT 0 AFTER cash_balance_cents;
-- 可用积分 = points_balance - frozen_points
-- 可用储值 = cash_balance_cents - frozen_cash_cents
```

### V079__alter_settlement_records_stacking.sql

```sql
-- 注意: 字段加在 collected_amount_cents 之后，不是 total_amount_cents (该字段不存在)
ALTER TABLE settlement_records
  ADD COLUMN points_deduct_cents BIGINT NOT NULL DEFAULT 0 AFTER collected_amount_cents,
  ADD COLUMN points_deducted BIGINT NOT NULL DEFAULT 0 AFTER points_deduct_cents,
  ADD COLUMN cash_balance_deduct_cents BIGINT NOT NULL DEFAULT 0 AFTER points_deducted,
  ADD COLUMN coupon_discount_cents BIGINT NOT NULL DEFAULT 0 AFTER cash_balance_deduct_cents,
  ADD COLUMN promotion_discount_cents BIGINT NOT NULL DEFAULT 0 AFTER coupon_discount_cents,
  ADD COLUMN external_payment_cents BIGINT NOT NULL DEFAULT 0 AFTER promotion_discount_cents,
  ADD COLUMN coupon_id BIGINT NULL AFTER external_payment_cents,
  ADD COLUMN stacking_rule_id BIGINT NULL AFTER coupon_id;
```

### V080__alter_payment_attempts_retry.sql

```sql
-- 保留现有 5 种状态，只新增 REPLACED 使用
ALTER TABLE payment_attempts
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER attempt_status,
  ADD COLUMN max_retries INT NOT NULL DEFAULT 3 AFTER retry_count,
  ADD COLUMN failure_reason VARCHAR(512) NULL AFTER max_retries,
  ADD COLUMN failure_code VARCHAR(64) NULL AFTER failure_reason,
  ADD COLUMN replaced_by_attempt_id BIGINT NULL AFTER failure_code,
  ADD COLUMN parent_attempt_id BIGINT NULL AFTER replaced_by_attempt_id,
  ADD INDEX idx_pa_parent (parent_attempt_id);
```

### V081__alter_member_coupons_locked.sql

```sql
-- coupon_status 新增 LOCKED 状态 + 乐观锁版本号
ALTER TABLE member_coupons
  ADD COLUMN lock_version INT NOT NULL DEFAULT 0 COMMENT '乐观锁' AFTER coupon_status,
  ADD COLUMN locked_by_session VARCHAR(64) NULL COMMENT '锁定的 session_id' AFTER lock_version,
  ADD COLUMN locked_at TIMESTAMP NULL AFTER locked_by_session;
-- coupon_status 现在是: AVAILABLE | LOCKED | USED | EXPIRED
-- AVAILABLE → LOCKED (结算冻结时)
-- LOCKED → USED (结算确认时)
-- LOCKED → AVAILABLE (结算取消/支付失败时)
```

### V082__create_buffet_packages.sql

```sql
CREATE TABLE buffet_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    package_code VARCHAR(64) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    price_cents BIGINT NOT NULL,
    child_price_cents BIGINT NULL,
    child_age_max INT NULL,
    duration_minutes INT NOT NULL DEFAULT 90,
    warning_before_minutes INT NOT NULL DEFAULT 10,
    overtime_fee_per_minute_cents BIGINT NOT NULL DEFAULT 0,
    overtime_grace_minutes INT NOT NULL DEFAULT 5,
    max_overtime_minutes INT NOT NULL DEFAULT 60,
    package_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    applicable_time_slots JSON NULL,
    applicable_days JSON NULL,
    sort_order INT NOT NULL DEFAULT 0,
    image_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_bp_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_bp_code UNIQUE (store_id, package_code),
    INDEX idx_bp_status (store_id, package_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V083__create_buffet_package_items.sql

```sql
CREATE TABLE buffet_package_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    inclusion_type VARCHAR(32) NOT NULL DEFAULT 'INCLUDED'
      COMMENT 'INCLUDED|SURCHARGE|EXCLUDED',
    surcharge_cents BIGINT NOT NULL DEFAULT 0,
    max_qty_per_person INT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bpi_package FOREIGN KEY (package_id) REFERENCES buffet_packages(id) ON DELETE CASCADE,
    CONSTRAINT fk_bpi_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_bpi UNIQUE (package_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V084__alter_products_menu_modes.sql

```sql
ALTER TABLE products
  ADD COLUMN menu_modes JSON NULL
    COMMENT '["A_LA_CARTE","BUFFET","DELIVERY"]，NULL=全模式' AFTER image_id;
```

### V085__create_menu_time_slots.sql

```sql
CREATE TABLE menu_time_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    slot_code VARCHAR(64) NOT NULL,
    slot_name VARCHAR(128) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    applicable_days JSON NOT NULL DEFAULT '["MON","TUE","WED","THU","FRI","SAT","SUN"]',
    dining_modes JSON NOT NULL DEFAULT '["A_LA_CARTE"]',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mts_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_mts UNIQUE (store_id, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE menu_time_slot_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_slot_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_mtsp_slot FOREIGN KEY (time_slot_id) REFERENCES menu_time_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_mtsp_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_mtsp UNIQUE (time_slot_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V086--V095: Sprint 4-6 (库存/运营/报表)

```
V086__alter_recipes_modifier.sql          — recipes 加 modifier_option_id + multiplier
V087__alter_purchase_invoices_ocr.sql     — OCR 字段
V088__create_sop_import_batches.sql
V089__create_inventory_driven_promotions.sql
V090__create_inspection_records.sql       — 含 inspection_items
V091__create_customer_feedback.sql
V092__create_external_integration_logs.sql
V093__create_cctv_events.sql
V094__create_report_snapshots.sql
V095__alter_kitchen_stations_fallback.sql
```

（V086-V095 的 DDL 参考原 spec，此处省略重复。关键点：这些表不涉及 round 1/2 的任何争议字段。）

---

## 2. 核心设计决策（全部已审计确认）

### D1 并台：纯指针 + 结算时聚合

```
不动 active_table_orders，不动 active_table_order_items，不动 submitted_orders。
只在 table_sessions.merged_into_session_id 上打指针。

结算时:
  sessionChain = [masterSessionId] + findAll(merged_into_session_id = masterSessionId)
  unpaidOrders = submitted_orders WHERE table_session_id IN sessionChain AND settlement_status = 'UNPAID'
  payableTotal = sum(unpaidOrders.payable_amount_cents)

改造点: CashierSettlementApplicationService.collectForTable() line 188
  现有: findByTableSessionIdAndSettlementStatus(session.getId(), "UNPAID")
  改为: findByTableSessionIdInAndSettlementStatus(sessionChainIds, "UNPAID")
```

### D2 支付叠加：持久化 holds + 保持 table-centric API

```
API 入口: POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/...
         (和现有 TableSettlementV2Controller 一致)

memberId 来源: 从 submitted_orders.member_id 取（和现有 collectForTable line 112-116 一致）
              不接受外部传入 memberId，不允许结算时换绑会员。

冻结流程:
  preview-stacking → 计算各步骤金额（不冻结）
  collect-stacking → 冻结 + 支付
    积分: member_accounts.frozen_points += X
    储值: member_accounts.frozen_cash_cents += X
    券: member_coupons.coupon_status = 'LOCKED' (乐观锁 CAS)
    每步写 settlement_payment_holds (hold_status = 'HELD')

确认 (外部支付成功):
  holds → CONFIRMED
  member_accounts.points_balance -= X, frozen_points -= X
  member_accounts.cash_balance_cents -= X, frozen_cash_cents -= X
  member_coupons.coupon_status = 'USED'
  写 settlement_records (含明细字段)

释放 (支付失败/放弃):
  holds → RELEASED
  frozen_points -= X, frozen_cash_cents -= X
  member_coupons.coupon_status = 'AVAILABLE', locked_by_session = NULL
```

### D3 优惠券并发防重

```
锁券:
  UPDATE member_coupons
  SET coupon_status = 'LOCKED',
      lock_version = lock_version + 1,
      locked_by_session = :sessionId,
      locked_at = NOW()
  WHERE id = :couponId
    AND coupon_status = 'AVAILABLE'
    AND lock_version = :expectedVersion

  如果 affected_rows == 0 → 抛 CouponAlreadyLockedException
  (乐观锁 CAS，不用 SELECT FOR UPDATE)

超时回收:
  定时任务每分钟检查:
  SELECT * FROM member_coupons
  WHERE coupon_status = 'LOCKED' AND locked_at < NOW() - INTERVAL 10 MINUTE
  → 释放为 AVAILABLE（settlement_payment_holds 也做 RELEASED）
```

### D4 动态 QR：qr_tokens 独立表 + JWT，支持空桌扫码

> **已统一**: 采用 `qr_tokens` 独立表方案（V071）。
> 不再使用 `store_tables.qr_token` / `stores.jwt_secret`。

```
QR 生成:
  INSERT INTO qr_tokens (store_id, table_id, token, token_type, expires_at)
  QR URL = https://{domain}/qr/{storeId}/{tableId}/{token}

扫码验证:
  GET /qr/{storeId}/{tableId}/{token}
  1. DB 查 qr_tokens WHERE store_id = storeId AND table_id = tableId AND token = token AND is_active = TRUE
  2. expires_at < NOW() → 400
  3. 查当前 session
  4. 生成 JWT (用 app-level secret，非 per-store secret):
     {
       storeId, tableId, sessionId (nullable), tableCode, exp: NOW() + 4h
     }
  5. 302 → /ordering?token={jwt}

清台刷新:
  UPDATE qr_tokens SET is_active = FALSE WHERE table_id = tableId;
  INSERT INTO qr_tokens ... (新 token)

关键: 后端不依赖 JWT 中的 sessionId 做业务决策。
     storeId + tableId 是必要的（防止跨桌操作）。
     qr_tokens 表支持审计、旋转、多 token 类型。
```

### D5 自助餐开台：用 tableId 找 session

```
POST /api/v2/stores/{storeId}/tables/{tableId}/buffet/start
Body: { packageId, guestCount, childCount }

  1. session = findOrCreateOpenSession(storeId, tableId)
     // 已有的逻辑，不需要前端传 sessionId
  2. session.dining_mode = 'BUFFET'
  3. session.buffet_package_id = packageId
  4. session.guest_count = guestCount
  5. session.child_count = childCount
  6. session.buffet_started_at = NOW()
  7. session.buffet_ends_at = NOW() + package.duration_minutes
  8. session.buffet_status = 'ACTIVE'
```

### D6 buffet 价格持久化到 submitted_order_items

```
toSubmittedItem() 必须拷贝:
  - is_buffet_included
  - buffet_surcharge_cents
  - buffet_package_id

结账时用 submitted_order_items 的 buffet 字段计算:
  套餐内免费: is_buffet_included=true AND buffet_surcharge_cents=0 → 不计价
  差价: is_buffet_included=true AND buffet_surcharge_cents>0 → 按 surcharge
  套餐外: is_buffet_included=false → 按 line_total_cents (原价)
```

### D7 支付重试：保留现有状态机

```
现有 (不改):
  PENDING_CUSTOMER → SUCCEEDED → SETTLED
  PENDING_CUSTOMER → FAILED
  PENDING_CUSTOMER → EXPIRED

新增:
  FAILED → REPLACED (换方式时标记旧的)

webhook 处理完全不变:
  VibeCashPaymentApplicationService.handleWebhook() 逻辑不动
  新增 retry/switch 只在 FAILED 状态上操作
```

### D8 审计：audit_trail 新表，action_log 不动

```
action_log → MCP/AI 工具日志，保持原样
audit_trail → 人工操作审计 + 审批流

两张表完全独立，无外键关联。
AOP @Audited 切面写 audit_trail。
MCP tool handler 写 action_log（现有逻辑不变）。
```

### D9 清台：table_status 新状态

```
现有代码 collectForTable() line 247: table.setTableStatus("AVAILABLE")
改为: table.setTableStatus("PENDING_CLEAN")

服务员确认清台:
  POST /stores/{storeId}/tables/{tableId}/mark-clean
  → table_status = 'AVAILABLE'
  → 刷新 QR token

自动清台（可选）:
  merchant_configs key='auto_clean_timeout_minutes', 默认 0=关闭
  > 0 时: cron 每分钟检查 PENDING_CLEAN 超时的桌台
```

### D10 结算时不允许换绑会员

```
结算使用的 memberId 来自 submitted_orders.member_id（现有逻辑 line 112-116）。
支付叠加 API 不接受 memberId 参数。
积分/储值/券都必须属于同一个 member。
如果 submitted_orders 中没有 member → 跳过积分/储值/券，只走外部支付。
```

---

## 3. Sprint 分配

| Sprint | Migration | 内容 |
|--------|-----------|------|
| S1 | V070-V073 | 并台 + 清台 + QR + 审计 |
| S2 | V074-V075, V082-V085 | 自助餐全流程 |
| S3 | V076-V081 | 支付叠加 + 重试 + 券锁定 |
| S4 | V086-V089 | 库存 (SOP/OCR/促销) |
| S5 | V090-V093 | 运营 (巡店/反馈/日志/CCTV) |
| S6 | V094-V095 | 报表 + KDS 回退 |

---

## 4. Java 改动清单（按 Sprint）

### S1 改动

| 文件 | 改动 | 说明 |
|------|------|------|
| CashierSettlementApplicationService.java:188 | findByTableSessionIdIn**And**SettlementStatus | 并台聚合 |
| CashierSettlementApplicationService.java:247 | setTableStatus("PENDING_CLEAN") | 清台中间态 |
| QrOrderingV2Controller.java | 加 JWT 验证 filter | 全链路 QR 保护 |
| 新 TableMergeApplicationService | 并台/拆台逻辑 | |
| 新 QrTokenService | token 生成/验证/JWT 签发 | |
| 新 AuditAspect + @Audited | AOP 切面写 audit_trail | |

### S2 改动

| 文件 | 改动 | 说明 |
|------|------|------|
| ActiveTableOrderApplicationService.java:446-456 | toSubmittedItem() 拷贝 buffet 字段 | R4 修复 |
| SubmittedOrderItemEntity.java | 加 isBuffetIncluded, buffetSurchargeCents, buffetPackageId | |
| ActiveTableOrderItemEntity.java | 加同上三个字段 | |
| 新 BuffetPackageApplicationService | 档位 CRUD + 商品绑定 | |
| 新 BuffetSessionService | 开台/计时/超时/金额计算 | |
| 新 BuffetMenuService | 菜单过滤 + 价格标注 | |

### S3 改动

| 文件 | 改动 | 说明 |
|------|------|------|
| CashierSettlementApplicationService.java | 加 previewStacking / collectStacking | 叠加结算 |
| MemberAccountEntity.java | 加 frozenPoints, frozenCashCents | |
| MemberCouponEntity.java | 加 lockVersion, lockedBySession, lockedAt | |
| VibeCashPaymentApplicationService.java | handleWebhook 成功后触发 hold confirm | |
| 新 PaymentStackingService | 叠加计算 + 冻结/确认/释放 | |
| 新 PaymentRetryService | 重试 + 换方式 | |

---

## 5. 和旧 spec 的差异总结

| 旧 spec (已废弃) | 本文档 (最终) |
|---|---|
| 并台移动 order_items | 纯指针，结算时聚合 sessionChain |
| settlement_records.total_amount_cents | payable_amount_cents (真实字段) |
| session_status = SETTLED | OPEN / CLOSED (真实值) |
| attempt_status 新状态集 | 保留 PENDING_CUSTOMER/SUCCEEDED/SETTLED/FAILED/EXPIRED + REPLACED |
| member.available_points | points_balance - frozen_points |
| HMAC QR | qr_tokens 独立表 + JWT session token (D4 已统一) |
| JWT 必带 sessionId | sessionId 可为 null (空桌扫码) |
| POST /sessions/{id}/buffet/start | POST /tables/{tableId}/buffet/start |
| buffet 字段只在 active_items | active + submitted 都有 |
| 改造 action_log | 新建 audit_trail，action_log 不动 |
| 结算 API 接受 memberId | memberId 从 submitted_orders 取，不接受传入 |
| coupon 直接标 USED | AVAILABLE → LOCKED (CAS) → USED/AVAILABLE |
| 两份分叉文档 | 本单一文档 |

---

*This is the single source of truth for implementation. All prior spec documents are superseded.*
