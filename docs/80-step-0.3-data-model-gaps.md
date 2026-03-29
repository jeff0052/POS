# Step 0.3 — 数据模型遗漏补缺

**Version:** V20260328017
**Date:** 2026-03-28
**Status:** FINAL (reviewed + all fixes applied 2026-03-28)
**源自:** 12 条 User Journey + 12 个状态机 → 对照 `docs/75-complete-database-schema.md`（119 表）

---

## 总览

| 类别 | 变更 | 涉及 Journey |
|------|------|-------------|
| **ALTER（9 项）** | | |
| 1. `table_sessions` 加 `merged_into_session_id` | ALTER ADD COLUMN | J11 |
| 2. `submitted_order_items` 加 buffet 字段 | ALTER ADD 3 COLUMNS | J02 |
| 3. `kitchen_stations` 加 fallback + health 字段 | ALTER ADD 4 COLUMNS | J06 |
| 4. `member_coupons` 加 `lock_version` | ALTER ADD COLUMN | J04 |
| 5. `reservations` 加联系人/来源/日期字段 | ALTER ADD 4 COLUMNS | J12 |
| 6. `purchase_invoices` 加 OCR 结果字段 | ALTER ADD 1 COLUMN | J08 |
| 7. `recipes` 加修饰符消耗字段 | ALTER ADD 3 COLUMNS | J08 |
| ~~8. `submitted_orders` 加 `delivery_status`~~ | ❌ 已存在，已删除 | ~~J03~~ |
| 8. `queue_tickets` 加 `called_count` / `skipped_at` | ALTER ADD 2 COLUMNS | J12 |
| **CREATE（9 张新表）** | | |
| 10. `settlement_payment_holds` | 冻结/确认/释放 | J04 |
| 11. `qr_tokens` | 二维码令牌 | J01 |
| 12. `customer_feedback` | 顾客评价 | J01, J07 |
| 13. `external_integration_logs` | 外部对接日志 | J03, J10, J12 |
| 14. `audit_trail` | 审计追踪 | J05, J07, J09, J10 |
| 15. `table_merge_records` | 并台记录 | J11 |
| 16. `report_snapshots` | 报表快照 + AI 摘要 | J09 |
| 17. `inventory_driven_promotions` | 库存驱动促销草案 | J07, J08 |
| 18. `sop_import_batches` | SOP 批量导入 | J08 |

> 变更后总计：**128 物理表**（不含 flyway_schema_history）。
> 原有 55 表 + V066 追赶 64 表 + 9 新表 = 128。
> 框架内 125 表（不含 3 Legacy: auth_users, staff, roles+role_permissions 已合并到 V060 users 体系）。

---

## 一、ALTER 现有表

### 1. table_sessions — 加 merged_into_session_id（J11 并台）

```sql
-- V070: table_sessions merge support
ALTER TABLE table_sessions
  ADD COLUMN merged_into_session_id BIGINT NULL AFTER buffet_status,
  ADD INDEX idx_ts_merged (merged_into_session_id);
```

**说明：** 被并桌的 session 指向主桌 session。NULL = 未并台。结账时通过 `WHERE merged_into_session_id = :masterSessionId OR id = :masterSessionId` 聚合。

---

### 2. submitted_order_items — 加 buffet 字段（J02 自助餐）

```sql
-- V074: submitted_order_items buffet fields
ALTER TABLE submitted_order_items
  ADD COLUMN is_buffet_included BOOLEAN DEFAULT FALSE AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT DEFAULT 0 AFTER is_buffet_included,
  ADD COLUMN buffet_inclusion_type VARCHAR(32) NULL AFTER buffet_surcharge_cents;
  -- buffet_inclusion_type: INCLUDED | SURCHARGE | EXCLUDED
```

**说明：** 从 `active_table_order_items` 提交时拷贝。INCLUDED = 套餐内免费，SURCHARGE = 差价，EXCLUDED = 套餐外原价。

---

### 3. kitchen_stations — 加 fallback + health 字段（J06 厨房）

```sql
-- V095: kitchen_stations fallback & health
ALTER TABLE kitchen_stations
  ADD COLUMN fallback_printer_ip VARCHAR(64) NULL AFTER printer_ip,
  ADD COLUMN fallback_mode VARCHAR(32) DEFAULT 'AUTO' AFTER fallback_printer_ip,
  ADD COLUMN kds_health_status VARCHAR(32) DEFAULT 'ONLINE' AFTER fallback_mode,
  ADD COLUMN last_heartbeat_at TIMESTAMP NULL AFTER kds_health_status;
  -- fallback_mode: AUTO | MANUAL | DISABLED
  -- kds_health_status: ONLINE | OFFLINE
```

**说明：** `last_heartbeat_at` 超过 90s → `kds_health_status = OFFLINE`。`fallback_mode = AUTO` 时自动切打印机。

---

### 4. member_coupons — 加 lock_version（J04 券并发 CAS）

```sql
-- V081: member_coupons CAS lock
ALTER TABLE member_coupons
  ADD COLUMN lock_version INT NOT NULL DEFAULT 0 AFTER coupon_status,
  ADD COLUMN locked_at TIMESTAMP NULL AFTER lock_version;
```

**说明：** CAS 乐观锁：`UPDATE member_coupons SET coupon_status='LOCKED', lock_version=lock_version+1 WHERE id=? AND lock_version=? AND coupon_status='AVAILABLE'`。`locked_at` 用于超时释放（15 分钟）。

---

### 5. reservations — 加联系人/来源/日期字段（J12 预约）

```sql
-- V096: reservations enhance
ALTER TABLE reservations
  ADD COLUMN contact_phone VARCHAR(32) NULL AFTER guest_name,
  ADD COLUMN source VARCHAR(32) DEFAULT 'MANUAL' AFTER reservation_status,
  ADD COLUMN reservation_date DATE NULL AFTER reservation_time,
  ADD COLUMN notes VARCHAR(512) NULL AFTER source;
  -- source: MANUAL | QR | PHONE | GOOGLE | PLATFORM
```

**说明：** 现有 `reservation_time VARCHAR(16)` 只存时间不存日期，补 `reservation_date`。`source` 追踪预约来源。

---

### 6. purchase_invoices — 加 OCR 结果字段（J08 库存）

```sql
-- V087: purchase_invoices OCR raw result
-- 注意：ocr_status 和 scan_image_url 已存在于 doc/66 原始 DDL 中
-- 设计决定：保留 scan_image_url（URL 模式），不加 image_asset_id（已记录）
ALTER TABLE purchase_invoices
  ADD COLUMN ocr_raw_result JSON NULL AFTER ocr_status;
```

**说明：** 只补 `ocr_raw_result`。`ocr_status` 和 `scan_image_url` 已存在。

---

### 7. recipes — 加修饰符消耗字段（J08 库存 SOP）

```sql
-- V086: recipes modifier consumption
-- 当前结构：id, sku_id, inventory_item_id, consumption_qty, consumption_unit, created_at
ALTER TABLE recipes
  ADD COLUMN modifier_consumption_rules JSON NULL AFTER consumption_unit,
  ADD COLUMN base_multiplier DECIMAL(5,2) DEFAULT 1.00 AFTER modifier_consumption_rules,
  ADD COLUMN notes VARCHAR(512) NULL AFTER base_multiplier;
```

**说明：** `modifier_consumption_rules` 示例：`{"大份": {"multiplier": 1.5}, "加辣": {"add": [{"item_id": 42, "qty_grams": 10}]}}`。修饰符消耗在 recipe 层面用 JSON 扩展，每行 recipe 可附加修饰符规则。

---

### ~~8. submitted_orders — 加 delivery_status（J03 外卖）~~ ❌ 已删除

> **BUG-1:** `submitted_orders.delivery_status` 已存在于 doc/66 原始 DDL 中（line 711）。V097 冗余，已删除。

---

### 9. queue_tickets — 加叫号追踪（J12 候位）

```sql
-- V098: queue_tickets call tracking
ALTER TABLE queue_tickets
  ADD COLUMN called_count INT DEFAULT 0 AFTER ticket_status,
  ADD COLUMN skipped_at TIMESTAMP NULL AFTER called_count;
```

**说明：** 叫号 3 次未应答 → `ticket_status = SKIPPED`。`called_count` 追踪叫了几次。

---

## 二、CREATE 新表

### 10. settlement_payment_holds — 冻结/确认/释放（J04 会员支付叠加）

```sql
-- V076: settlement_payment_holds
CREATE TABLE settlement_payment_holds (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hold_no VARCHAR(64) NOT NULL,
    settlement_record_id BIGINT NULL,
    member_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    hold_type VARCHAR(32) NOT NULL,
      -- POINTS | CASH | COUPON
    hold_amount BIGINT NOT NULL,
      -- 积分数 or cents
    hold_ref VARCHAR(128) NULL,
      -- coupon_no / points_batch_id 等
    hold_status VARCHAR(32) NOT NULL DEFAULT 'HELD',
      -- HELD → CONFIRMED | RELEASED
    held_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    released_at TIMESTAMP NULL,
    release_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_sph_hold_no UNIQUE (hold_no),
    CONSTRAINT fk_sph_member FOREIGN KEY (member_id) REFERENCES members(id),
    INDEX idx_sph_member (member_id, hold_status),
    INDEX idx_sph_settlement (settlement_record_id),
    INDEX idx_sph_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**状态机：**
```
HELD → CONFIRMED (结账成功)
HELD → RELEASED (结账取消 / 超时 15 分钟)
```

---

### 11. qr_tokens — 二维码令牌（J01 扫码点单）

```sql
-- V071: qr_tokens
CREATE TABLE qr_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL,
    token_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
      -- ACTIVE | EXPIRED | REVOKED
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_qr_token UNIQUE (token),
    CONSTRAINT fk_qr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_qr_table FOREIGN KEY (table_id) REFERENCES store_tables(id),
    INDEX idx_qr_store_table (store_id, table_id, token_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明：** 每次清台刷新 token（旧 token → EXPIRED）。QR URL = `/qr/{storeId}/{tableId}/{token}`。

---

### 12. customer_feedback — 顾客评价（J01, J07）

```sql
-- V091: customer_feedback
CREATE TABLE customer_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    feedback_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL,
    table_session_id BIGINT NULL,
    member_id BIGINT NULL,
    submitted_order_id BIGINT NULL,
    feedback_type VARCHAR(32) NOT NULL DEFAULT 'REVIEW',
      -- REVIEW | COMPLAINT | SUGGESTION
    overall_rating INT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    food_rating INT NULL CHECK (food_rating BETWEEN 1 AND 5),
    service_rating INT NULL CHECK (service_rating BETWEEN 1 AND 5),
    content TEXT NULL,
    feedback_status VARCHAR(32) NOT NULL DEFAULT 'NEW',
      -- NEW → IN_PROGRESS → RESOLVED | DISMISSED
    resolved_by BIGINT NULL,
    resolved_at TIMESTAMP NULL,
    resolution_note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cf_no UNIQUE (feedback_no),
    CONSTRAINT fk_cf_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_cf_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    INDEX idx_cf_store_status (store_id, feedback_status),
    INDEX idx_cf_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 13. external_integration_logs — 外部对接日志（J03, J10, J12）

```sql
-- V092: external_integration_logs
CREATE TABLE external_integration_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    platform VARCHAR(64) NOT NULL,
      -- GRAB | FOODPANDA | GOOGLE | WECHAT | ...
    direction VARCHAR(16) NOT NULL,
      -- INBOUND | OUTBOUND
    endpoint VARCHAR(512) NOT NULL,
    request_body LONGTEXT NULL,
      -- LONGTEXT 而非 JSON：webhook payload 可能很大，与 payment_attempts 保持一致
    response_body LONGTEXT NULL,
    http_status INT NULL,
    result_status VARCHAR(32) NOT NULL,
      -- SUCCESS | FAILED | TIMEOUT
    error_message VARCHAR(1024) NULL,
    correlation_id VARCHAR(128) NULL,
    duration_ms INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eil_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_eil_store_platform (store_id, platform),
    INDEX idx_eil_correlation (correlation_id),
    INDEX idx_eil_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明：** 每次外部 webhook 出入都记录。连续 3 次 FAILED → 告警。

---

### 14. audit_trail — 审计追踪（J05, J07, J09, J10）

```sql
-- V073: audit_trail
CREATE TABLE audit_trail (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trail_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
      -- HUMAN | AI | SYSTEM
    actor_id BIGINT NULL,
    actor_name VARCHAR(128) NULL,
    action VARCHAR(64) NOT NULL,
      -- REFUND | PRICE_CHANGE | VOID_ORDER | DISCOUNT_OVERRIDE | ...
    target_type VARCHAR(64) NOT NULL,
      -- settlement_records | skus | submitted_orders | ...
    target_id VARCHAR(64) NOT NULL,
      -- VARCHAR(64) 而非 BIGINT：兼容 active_order_id 等 VARCHAR 业务 ID
    before_snapshot JSON NULL,
    after_snapshot JSON NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW',
      -- LOW | MEDIUM | HIGH | CRITICAL
    requires_approval BOOLEAN DEFAULT FALSE,
    approval_status VARCHAR(32) NULL,
      -- PENDING | APPROVED | REJECTED
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    approval_note VARCHAR(512) NULL,
    ip_address VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_at_trail_no UNIQUE (trail_no),
    CONSTRAINT fk_at_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_at_store_action (store_id, action),
    INDEX idx_at_approval (store_id, approval_status),
    INDEX idx_at_target (target_type, target_id),
    INDEX idx_at_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**说明：** 高风险操作（退款 > 阈值、改价）需审批。`before_snapshot` / `after_snapshot` 存变更前后的 JSON 快照。

---

### 15. table_merge_records — 并台记录（J11）

```sql
-- V072: table_merge_records
CREATE TABLE table_merge_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    master_table_id BIGINT NOT NULL,
    master_session_id BIGINT NOT NULL,
    merged_table_id BIGINT NOT NULL,
    merged_session_id BIGINT NOT NULL,
    merged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unmerged_at TIMESTAMP NULL,
    unmerged_by BIGINT NULL,
    merge_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
      -- ACTIVE | UNMERGED | SETTLED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tmr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_tmr_master_table FOREIGN KEY (master_table_id) REFERENCES store_tables(id),
    CONSTRAINT fk_tmr_merged_table FOREIGN KEY (merged_table_id) REFERENCES store_tables(id),
    CONSTRAINT fk_tmr_master_session FOREIGN KEY (master_session_id) REFERENCES table_sessions(id),
    CONSTRAINT fk_tmr_merged_session FOREIGN KEY (merged_session_id) REFERENCES table_sessions(id),
    INDEX idx_tmr_master (master_session_id),
    INDEX idx_tmr_store (store_id, merge_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 16. report_snapshots — 报表快照 + AI 摘要（J09）

```sql
-- V094: report_snapshots
CREATE TABLE report_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    report_type VARCHAR(32) NOT NULL,
      -- DAILY_SUMMARY | WEEKLY_SUMMARY | MONTHLY_SUMMARY
    report_date DATE NOT NULL,
    metrics_json JSON NOT NULL,
      -- { revenue_cents, order_count, avg_ticket_cents, table_turnover_rate, ... }
    ai_summary TEXT NULL,
    ai_highlights JSON NULL,
      -- ["营收创周内新高", "自助餐占比30%"]
    ai_warnings JSON NULL,
      -- ["牛腩库存仅剩2天"]
    ai_suggestions JSON NULL,
      -- ["推出午市自助套餐"]
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_rs UNIQUE (store_id, report_type, report_date),
    CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_rs_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_rs_merchant (merchant_id, report_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 17. inventory_driven_promotions — 库存驱动促销草案（J07, J08）

```sql
-- V089: inventory_driven_promotions
CREATE TABLE inventory_driven_promotions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    inventory_batch_id BIGINT NULL,
    trigger_type VARCHAR(32) NOT NULL,
      -- NEAR_EXPIRY | OVERSTOCK | LOW_TURNOVER
    suggested_discount_percent DECIMAL(5,2) NOT NULL,
      -- 支持小数折扣如 12.50%
    suggested_sku_ids JSON NOT NULL,
      -- [101, 102, ...]
    draft_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
      -- DRAFT → APPROVED → CREATED | REJECTED | EXPIRED
    promotion_rule_id BIGINT NULL,
      -- 审批后创建的 promotion_rule
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_idp_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_idp_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    INDEX idx_idp_store_status (store_id, draft_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 18. sop_import_batches — SOP 批量导入（J08）

```sql
-- V088: sop_import_batches
CREATE TABLE sop_import_batches (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_asset_id VARCHAR(64) NULL,
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    error_rows INT NOT NULL DEFAULT 0,
    batch_status VARCHAR(32) NOT NULL DEFAULT 'VALIDATING',
      -- VALIDATING → VALIDATED → IMPORTING → COMPLETED | FAILED
    error_details JSON NULL,
      -- [{"row": 5, "error": "SKU not found: ABC"}]
    imported_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sib_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_sib_store (store_id, batch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 三、已确认无需修改的项

| 项 | 原因 |
|----|------|
| `store_tables` 加 `PENDING_CLEAN` / `MERGED` / `RESERVED` 状态 | `table_status VARCHAR(32)`，枚举值在应用层控制，无需 DDL |
| `payment_attempts` 加 `REPLACED` 状态 | `attempt_status VARCHAR(32)`，枚举值在应用层控制，无需 DDL |
| `member_accounts` 加 `frozen_points` / `frozen_cash_cents` | **已存在**（doc/72 ALTER 已加） |
| `table_sessions` 加 `buffet_started_at` / `buffet_ends_at` / `buffet_status` | **已存在**（doc/73 ALTER 已加） |

---

## 四、Migration 编号规划（FINAL — 含 review 修复）

| 编号 | 内容 | 涉及 Journey |
|------|------|-------------|
| V070 | `table_sessions` ADD `merged_into_session_id` | J11 |
| V071 | CREATE `qr_tokens` | J01 |
| V072 | CREATE `table_merge_records`（含 session FK） | J11 |
| V073 | CREATE `audit_trail`（target_id VARCHAR(64)，含 store FK + target 索引） | J05, J07, J09, J10 |
| V074 | `submitted_order_items` + `active_table_order_items` ADD buffet fields | J02 |
| V076 | CREATE `settlement_payment_holds`（含 member FK + store 索引） | J04 |
| V081 | `member_coupons` ADD `lock_version`, `locked_at` | J04 |
| V086 | `recipes` ADD modifier fields（AFTER consumption_unit） | J08 |
| V087 | `purchase_invoices` ADD `ocr_raw_result`（仅此 1 字段） | J08 |
| V088 | CREATE `sop_import_batches` | J08 |
| V089 | CREATE `inventory_driven_promotions`（discount DECIMAL(5,2)） | J07, J08 |
| V091 | CREATE `customer_feedback`（含 CHECK + order FK） | J01, J07 |
| V092 | CREATE `external_integration_logs`（LONGTEXT body，含 store FK） | J03, J10, J12 |
| V094 | CREATE `report_snapshots`（含 store + merchant FK） | J09 |
| V095 | `kitchen_stations` ADD fallback + health fields | J06 |
| V096 | `reservations` ADD contact/source/date fields | J12 |
| V098 | `queue_tickets` ADD `called_count`, `skipped_at` | J12 |
| V099 | `active_table_orders` RENAME dining_type→dining_mode + status→order_status + 数据迁移 | 全局 |
| V101 | 补充缺失索引（8 个） | 全局 |

> **共 19 个 migration**（8 ALTER + 9 CREATE + 1 RENAME + 1 INDEX）
> 已删除：V097（submitted_orders.delivery_status 已存在）
> 已合并：V099 + V100 → V099（减少 DDL 锁）

---

## 五、更新后的 active_table_order_items 字段

`active_table_order_items` 也需要 buffet 字段（与 submitted_order_items 对齐）：

```sql
-- 包含在 V074 中
ALTER TABLE active_table_order_items
  ADD COLUMN is_buffet_included BOOLEAN DEFAULT FALSE AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT DEFAULT 0 AFTER is_buffet_included,
  ADD COLUMN buffet_inclusion_type VARCHAR(32) NULL AFTER buffet_surcharge_cents;
```

---

*Step 0.3 complete. 下一步：0.4 统一 review DDL → 0.5 生成 Flyway migrations.*
