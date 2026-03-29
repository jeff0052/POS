# Step 0.4 — DDL 统一 Review

**Version:** V20260328019
**Date:** 2026-03-28
**Status:** FINAL (all fixes applied to docs/80, agent review incorporated 2026-03-28)
**范围：** 120 张现有表 + 9 张新表 + 8 个 ALTER（Step 0.3，V097 已删除）

---

## ��、Step 0.3 DDL 中的 BUG（必须修）

### BUG-1: `submitted_orders.delivery_status` 已存在 — V097 冗余

```
现状 (line 711): delivery_status VARCHAR(32) NULL  ← doc/66 已加
Step 0.3 V097: ALTER TABLE submitted_orders ADD delivery_status  ← 重复！
```

**修复：删除 V097。** `submitted_orders` 已有 `delivery_status`。

---

### BUG-2: `purchase_invoices.ocr_status` 已存在 — V087 部分冗余

```
现状 (line 1013): ocr_status VARCHAR(32) NULL       ← doc/66 已加
现状 (line 1012): scan_image_url VARCHAR(512) NULL   ← 已有图片字段
Step 0.3 V087: ADD image_asset_id, ocr_status, ocr_raw_result  ← ocr_status 重复！
```

**修复 V087：**
```sql
-- V087 修正版：只加 ocr_raw_result
ALTER TABLE purchase_invoices
  ADD COLUMN ocr_raw_result JSON NULL AFTER ocr_status;
-- scan_image_url 保留（向后兼容），image_asset_id 不需要加
```

---

### BUG-3: `recipes` ALTER 引用不存在的字段 `recipe_id`

```
Step 0.3 V086: ADD COLUMN modifier_consumption_rules JSON NULL AFTER recipe_id
recipes 表实际字段：id, sku_id, inventory_item_id, consumption_qty, consumption_unit, created_at
→ 没有 recipe_id 字段！
```

**修复 V086：**
```sql
ALTER TABLE recipes
  ADD COLUMN modifier_consumption_rules JSON NULL AFTER consumption_unit,
  ADD COLUMN base_multiplier DECIMAL(5,2) DEFAULT 1.00 AFTER modifier_consumption_rules,
  ADD COLUMN notes VARCHAR(512) NULL AFTER base_multiplier;
```

---

## 二、命名不一致（应修）

### NAMING-1: `dining_type` vs `dining_mode`

| 表 | 字段 | 值 |
|---|------|---|
| active_table_orders | `dining_type` | DINE_IN |
| table_sessions | `dining_mode` | A_LA_CARTE |
| submitted_orders | `dining_mode` | A_LA_CARTE |
| buffet_packages | (隐含) | BUFFET |

**问题：** `active_table_orders.dining_type` 用 `DINE_IN`，其他地方用 `dining_mode` + `A_LA_CARTE`。两套命名。

**修复：** 统一为 `dining_mode`，值统一用 `A_LA_CARTE | BUFFET | DELIVERY | MIXED`。
```sql
-- V099: 统一 dining 命名
ALTER TABLE active_table_orders
  CHANGE COLUMN dining_type dining_mode VARCHAR(32) NOT NULL DEFAULT 'A_LA_CARTE';
```

---

### NAMING-2: `_status` 字段命名不统一

| 模式 | 表 | 字段 |
|------|----|------|
| `xxx_status` | store_tables | `table_status` |
| `xxx_status` | table_sessions | `session_status` |
| `status` | active_table_orders | `status` ← 太泛 |
| `xxx_status` | submitted_orders | `settlement_status` |
| `xxx_status` | kitchen_tickets | `ticket_status` |

**问题：** `active_table_orders.status` 缺前缀，和其他表风格不一致。

**修复：** 改为 `order_status`。
```sql
-- V100: 统一 status 命名
ALTER TABLE active_table_orders
  CHANGE COLUMN status order_status VARCHAR(32) NOT NULL;
```

> **注意：** 这两个 RENAME 会影响 Java 代码。记录下来在 Step 1 写代码时一并处理。

---

## 三、技术风格不一致（建议修）

### STYLE-1: `DATETIME(6)` vs `TIMESTAMP`

| 表 | 用法 |
|----|------|
| payment_attempts | `DATETIME(6)` (微秒精度) |
| action_log | `DATETIME(6)` |
| ai_proposal | `DATETIME(6)` |
| 其他 115+ 张表 | `TIMESTAMP` |

**建议：** 保持现状。`payment_attempts` 和 AI 表来自已上线的 Flyway migration，改动风险大。新表统一用 `TIMESTAMP`。
在 CLAUDE.md 或 coding convention 记录：**新表一律用 TIMESTAMP，不用 DATETIME(6)**。

---

### STYLE-2: `LONGTEXT` vs `JSON`

| 表 | 字段 | 类型 |
|----|------|------|
| payment_attempts | last_webhook_payload_json | `LONGTEXT` |
| 其他所有 JSON 字段 | | `JSON` |

**建议：** 保持现状。LONGTEXT 是因为 webhook payload 可能超过 JSON 类型的内部限制。新表用 `JSON`。

---

### STYLE-3: 缺少 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`

Flyway 来源的表（#1-#33, #88-#97, #108-#119）没写 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`。
doc/66+ 来源的表全部都有。

**建议：** MySQL 默认就是 InnoDB + utf8mb4（如果 my.cnf 配了的话）。但为了显式声明，**新表和 Step 0.3 新表全部加上**。已有表不改。

---

## 四、FK 关系问题

### FK-1: `settlement_records.active_order_id` — VARCHAR 引用，无 FK

```sql
settlement_records.active_order_id VARCHAR(64) NOT NULL
-- 引用 active_table_orders.active_order_id VARCHAR(64)
-- 但没有 FK 约束！且 active_table_orders 结账后会被删除
```

**问题：** 这是设计决策——settlement_records 用 VARCHAR 业务 ID 而非 BIGINT PK 引用，因为 active_table_orders 结账后会被删除。这实际上是正确的：settlement 需要在 active_order 删除后仍然可查。

**结论：保持现状。** 这是有意为之的非 FK 引用。

---

### FK-2: `cashier_shifts.cashier_staff_id` — 引用旧 staff 模式

```sql
cashier_shifts.cashier_staff_id VARCHAR(64) NOT NULL  -- 旧 staff.staff_id
cashier_shifts.employee_id BIGINT NULL                 -- 新 employees.id
```

**问题：** `cashier_staff_id` 是旧的 VARCHAR 引用，`employee_id` 是新的 BIGINT 引用。过渡期两个都存在。

**建议：** Step 1 RBAC 迁移时，把 `cashier_staff_id` 改为从 `users.user_code` 取值，最终删除 `cashier_staff_id`。现阶段不动。

---

### FK-3: 各种 `approved_by` / `operated_by` / `recorded_by` 没有 FK

涉及表：refund_records, stocktake_tasks, waste_records, purchase_orders, leave_requests, payroll_records, inventory_transfers, ...

**结论：保持现状。** 这些字段引用 `users.id`，但不加 FK 是常见做法（避免 user 删除时级联问题）。应用层保证一致性。

---

## 五、索引覆盖缺口

### INDEX-1: 高频查询缺索引

| 表 | 缺失索引 | 查询场景 |
|----|---------|---------|
| submitted_order_items | `sku_id` | 按 SKU 查销量 |
| active_table_order_items | `sku_id` | 按 SKU 查当前在制 |
| kitchen_ticket_items | `sku_id` | 按 SKU 查厨房票 |
| settlement_records | `store_id, created_at` | 按门店查结算 |
| settlement_records | `cashier_id` | 按收银员查结算 |

**修复：** 在 Flyway migration 中加索引。
```sql
-- V101: 补充缺失索引
CREATE INDEX idx_soi_sku ON submitted_order_items (sku_id);
CREATE INDEX idx_atoi_sku ON active_table_order_items (sku_id);
CREATE INDEX idx_kti_sku ON kitchen_ticket_items (sku_id);
CREATE INDEX idx_sr_store ON settlement_records (store_id, created_at);
CREATE INDEX idx_sr_cashier ON settlement_records (cashier_id);
```

---

## 六、Step 0.3 新表 Review

### 逐表检查清单

| 新表 | ENGINE/CHARSET | FK 完整 | 索引覆盖 | 命名一致 | 问题 |
|------|---------------|---------|---------|---------|------|
| settlement_payment_holds | ✅ | ⚠️ 缺 FK to members | ✅ | ✅ | 加 FK |
| qr_tokens | ✅ | ✅ | ✅ | ✅ | OK |
| customer_feedback | ✅ | ✅ | ✅ | ✅ | OK |
| external_integration_logs | ✅ | ⚠️ 缺 FK to stores | ✅ | ✅ | 加 FK |
| audit_trail | ✅ | ⚠️ 缺 FK to stores | ✅ | ✅ | 加 FK |
| table_merge_records | ✅ | ✅ | ✅ | ✅ | OK |
| report_snapshots | ✅ | ⚠️ 缺 FK to stores/merchants | ✅ | ✅ | 加 FK |
| inventory_driven_promotions | ✅ | ✅ | ✅ | ✅ | OK |
| sop_import_batches | ✅ | ✅ | ✅ | ✅ | OK |

### 修复：补充缺失 FK

```sql
-- settlement_payment_holds: 加 members FK
ALTER TABLE settlement_payment_holds
  ADD CONSTRAINT fk_sph_member FOREIGN KEY (member_id) REFERENCES members(id);

-- external_integration_logs: 加 stores FK
ALTER TABLE external_integration_logs
  ADD CONSTRAINT fk_eil_store FOREIGN KEY (store_id) REFERENCES stores(id);

-- audit_trail: 加 stores FK
ALTER TABLE audit_trail
  ADD CONSTRAINT fk_at_store FOREIGN KEY (store_id) REFERENCES stores(id);

-- report_snapshots: 加 stores + merchants FK
ALTER TABLE report_snapshots
  ADD CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores(id),
  ADD CONSTRAINT fk_rs_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id);
```

> **注：** 这些 FK 应该直接写进 Step 0.3 的 CREATE TABLE 里，不需要单独 ALTER。

---

## 七、总结：需要动作的项

### 必须修（阻塞 Step 0.5）

| # | 类型 | 描述 | 动作 |
|---|------|------|------|
| BUG-1 | Step 0.3 冗余 | V097 `submitted_orders.delivery_status` 已存在 | 删除 V097 |
| BUG-2 | Step 0.3 冗余 | V087 `purchase_invoices.ocr_status` 已存在 | V087 只加 `ocr_raw_result` |
| BUG-3 | Step 0.3 错误 | V086 AFTER `recipe_id` 不存在 | 改为 AFTER `consumption_unit` |
| FK-NEW | Step 0.3 遗漏 | 4 张新表缺 FK | 直接写进 CREATE TABLE |

### 应该修（Step 1 前完成）

| # | 类型 | 描述 | 动作 |
|---|------|------|------|
| NAMING-1 | 命名不一致 | `dining_type` vs `dining_mode` | V099 RENAME |
| NAMING-2 | 命名不一致 | `status` 太泛 | V100 RENAME |
| INDEX-1 | 索引缺失 | 5 个高频查询缺索引 | V101 CREATE INDEX |

### 记录但不改

| # | 类型 | 描述 | 理由 |
|---|------|------|------|
| STYLE-1 | DATETIME vs TIMESTAMP | 3 张旧表用 DATETIME(6) | 已上线，风险大 |
| STYLE-2 | LONGTEXT vs JSON | 1 个字段 | 有功能原因 |
| STYLE-3 | 缺 ENGINE 声明 | Flyway 旧表 | MySQL 默认即 InnoDB |
| FK-1 | VARCHAR 引用无 FK | settlement_records | 有意设计 |
| FK-2 | 旧 staff 引用 | cashier_shifts | Step 1 迁移处理 |
| FK-3 | approved_by 无 FK | 多表 | 常见做法 |

---

## 八、修正后的 Migration 编号规划

见 `docs/80-step-0.3-data-model-gaps.md` 第四节的最终 migration 规划（19 个 migration）。

---

## 九、Agent Review 额外发现（2026-03-28 补充）

以下问题由 code-reviewer agent 发现并已修复：

### 已修复 ✅

| # | 问题 | 修复 |
|---|------|------|
| C1 | V099 改名漏数据迁移（DINE_IN→A_LA_CARTE） | V099 加 UPDATE 语句 |
| C2 | `audit_trail.target_id` BIGINT 存不了 VARCHAR 业务 ID | 改为 VARCHAR(64) |
| I1 | `customer_feedback` 漏 `submitted_order_id` FK | 加 FK |
| I2 | `table_merge_records` 漏 session FK | 加 master/merged session FK |
| I3 | V099+V100 分开改同一张表 | 合并为 V099 |
| I4 | 缺 `submitted_orders(member_id)` 等索引 | 加到 V101 |
| I5 | `audit_trail` 缺 `(target_type, target_id)` 索引 | 已加 |
| I6 | BUG-2 修复丢掉 image_asset_id 未说明 | 已记录设计决定 |
| S1 | customer_feedback 评分无 CHECK | 加 CHECK (BETWEEN 1 AND 5) |
| S2 | external_integration_logs body 用 JSON 太小 | 改 LONGTEXT |
| S3 | discount_percent INT 不支持小数 | 改 DECIMAL(5,2) |

### 待确认 ⚠️

| # | 问题 | 建议 | 决定 |
|---|------|------|------|
| S4 | `recipes` 表无 `store_id`，多门店不同配方无法区分 | 如果所有门店用相同配方则不需要加 | **暂不加**，recipes 通过 sku_id 关联，SKU 本身已有 store 维度（via products.store_id）。如果未来需要门店级配方定制，再加 store_id + 改 UK |
| S5 | migration 编号有跳号（V074→V076, V081→V086） | 记录跳号原因 | **保持现状**，编号对应 Step 1-8 的 journey 分组，跳号是为了给同组 migration 预留空间 |

---

*Step 0.4 DDL Review FINAL. 下一步：0.5 按此生成最终 Flyway migration SQL 文件。*
