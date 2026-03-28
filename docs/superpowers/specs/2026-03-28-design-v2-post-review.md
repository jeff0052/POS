# FounderPOS V3 — 设计修订版 (Post-Review)

**Version:** V20260328013
**Date:** 2026-03-28
**Status:** DRAFT v2 — 审计修复版
**前置文档:** `2026-03-28-17-gaps-phase1-buffet-design-review.md`（审计报告）
**修改内容:** 修复审计发现的 4 个 P0 + 3 个 P1 + 1 个 P2 问题

---

## 审计修复清单

| # | 问题 | 严重性 | 修复方案 |
|---|------|--------|---------|
| R1 | 并台模型自相矛盾 | P0 | 纯指针模型，不移动 items，结算时聚合 |
| R2 | 支付叠加无持久化 | P0 | 新建 settlement_payment_holds 表，保持 session 结算模型 |
| R3 | 动态 QR 只保护入口 | P0 | 改为 DB lookup + session token 方案 |
| R4 | buffet 价格 submit 后丢失 | P0 | 扩展 submitted_order_items + 修改 toSubmittedItem() |
| R5 | 自助餐 session API 循环依赖 | P1 | buffet/start 更新已有 session，不创建新的 |
| R6 | 支付重试和现有状态机冲突 | P1 | 保留现有 5 种状态，只加 REPLACED |
| R7 | audit log 混合三种关注点 | P1 | 新建 audit_trail 表，action_log 保持不变 |
| R8 | migration 编号漂移 | P2 | 从 V070 开始，严格连续 |

---

## R1 修复：并台模型

### 问题根因

原设计说"指针式不拷贝"，但业务逻辑又写了"移动 order_items"。同时引用了不存在的 `active_table_order_items.session_id`。

### 真实系统结构

```
active_table_orders (draft，per table)
  └── active_table_order_items (FK: active_order_id)
  └── UK: (store_id, table_id) → 每桌只能有一个 draft

submitted_orders (已提交，per session)
  └── submitted_order_items (FK: submitted_order_db_id)
  └── FK: table_session_id → 一个 session 可以有多个 submitted_orders

table_sessions
  └── status: OPEN | CLOSED
  └── FK: table_id
```

### 修复方案：纯指针 + 结算时聚合

**核心规则：并台不动任何 order/item 数据。只在 session 上打标记。结算时通过 session 链聚合。**

```
并台后的数据关系:

masterTable(A01)                    mergedTable(A02)
  └── activeOrder(draft)              └── activeOrder(draft) ← 保留，不动
  └── session(#501)                   └── session(#502)
        │                                   │
        │                                   ├── merged_into_session_id = #501
        │                                   │
        ├── submittedOrder(#1001)           ├── submittedOrder(#1002)
        ├── submittedOrder(#1003)           └── (后续提交的也归这个 session)
        └── ...

结算时:
  sessions = [#501] + findAll(merged_into_session_id = #501) → [#501, #502]
  submittedOrders = findAll(table_session_id IN [#501, #502])
  totalPayable = sum(submittedOrders.payable_amount_cents)
```

**为什么不移动 active_table_order_items：**
1. active_table_orders 有 UK(store_id, table_id)，不能把两张桌的 draft 合并
2. draft 是临时的，提交到厨房后就变成 submitted_order
3. 结算依赖的是 submitted_orders（通过 session），不是 active_table_orders

**并台对 active draft 的影响：**
- 被并桌(A02)如果有未提交的 draft → 正常提交到厨房，submitted_order 归 session #502
- session #502 的 merged_into_session_id = #501
- 结算时自动聚合

### DDL

**V070__alter_table_sessions_merge.sql:**
```sql
ALTER TABLE table_sessions
  ADD COLUMN merged_into_session_id BIGINT NULL
    COMMENT '被并入哪个session的id，NULL=未被并入' AFTER session_status,
  ADD COLUMN guest_count INT NOT NULL DEFAULT 1
    COMMENT '本桌人数' AFTER merged_into_session_id,
  ADD INDEX idx_ts_merged (merged_into_session_id);
```

**V071__alter_store_tables_status.sql:**
```sql
ALTER TABLE store_tables
  ADD COLUMN zone VARCHAR(64) NULL COMMENT '区域' AFTER table_name,
  ADD COLUMN min_guests INT NOT NULL DEFAULT 1 AFTER zone,
  ADD COLUMN max_guests INT NOT NULL DEFAULT 4 AFTER min_guests,
  MODIFY COLUMN table_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE'
    COMMENT 'AVAILABLE|OCCUPIED|RESERVED|PENDING_CLEAN|MERGED|DISABLED';
```

**V072__create_table_merge_records.sql:**
```sql
CREATE TABLE table_merge_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    master_session_id BIGINT NOT NULL,
    merged_session_id BIGINT NOT NULL,
    master_table_id BIGINT NOT NULL,
    merged_table_id BIGINT NOT NULL,
    guest_count_at_merge INT NOT NULL COMMENT '并入时被并桌人数',
    merged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unmerged_at TIMESTAMP NULL COMMENT 'NULL=仍在合并中',
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

### 业务逻辑

```
merge(storeId, masterTableId, mergedTableId, operatorId):
  校验:
    masterTable.table_status == OCCUPIED
    mergedTable.table_status == OCCUPIED
    masterTableId != mergedTableId
    masterSession = sessions.findActiveByTableId(masterTableId)  // status = OPEN
    mergedSession = sessions.findActiveByTableId(mergedTableId)  // status = OPEN
    mergedSession.merged_into_session_id == NULL

  执行 (事务):
    mergedSession.merged_into_session_id = masterSession.id
    mergedTable.table_status = 'MERGED'
    INSERT table_merge_records

  不做的事:
    - 不动 active_table_orders
    - 不动 active_table_order_items
    - 不动 submitted_orders
    - 不改 masterSession 的任何字段（guest_count 聚合在查询层做）

unmerge(mergeRecordId, operatorId):
  校验:
    record.unmerged_at == NULL
    masterSession.session_status == OPEN (未关闭/未结账)

  执行 (事务):
    mergedSession.merged_into_session_id = NULL
    mergedTable.table_status = 'OCCUPIED'
    record.unmerged_at = NOW()

结算聚合 (在 SettlementService 中):
  getSessionChain(masterSessionId):
    chainIds = [masterSessionId]
    mergedIds = sessions.findByMergedIntoSessionId(masterSessionId).map(id)
    chainIds.addAll(mergedIds)
    return chainIds

  getSettlementTotal(masterSessionId):
    chainIds = getSessionChain(masterSessionId)
    orders = submittedOrders.findBySessionIdInAndSettlementStatus(chainIds, 'UNPAID')
    return sum(orders.payableAmountCents)
```

---

## R2 修复：支付叠加持久化

### 问题根因

原设计用 `stackingCalculationId` 缓存计算结果（Redis/内存），但冻结积分/储值没有持久化表。如果服务重启或外部支付回调延迟，冻结状态会丢失。同时 API 设计漂移到了 orderId 模型，和现有 table/session 模型冲突。

### 修复方案

1. 新建 `settlement_payment_holds` 表持久化冻结状态
2. API 保持 table/session 模型（和现有 `TableSettlementV2Controller` 一致）
3. `member_accounts` 加 frozen 字段
4. 支付叠加规则表保持不变

### DDL

**V076__create_payment_stacking_rules.sql:**
```sql
CREATE TABLE payment_stacking_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NULL COMMENT 'NULL=品牌级',
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

**V077__create_settlement_payment_holds.sql:**
```sql
-- 结算冻结记录：每一步扣减的持久化状态
CREATE TABLE settlement_payment_holds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    table_session_id BIGINT NOT NULL COMMENT '结算的 session（主桌）',
    stacking_rule_id BIGINT NULL,

    -- 冻结步骤
    step_order INT NOT NULL COMMENT '扣减顺序 1,2,3,4',
    hold_type VARCHAR(32) NOT NULL COMMENT 'POINTS|COUPON|CASH_BALANCE|EXTERNAL',
    hold_amount_cents BIGINT NOT NULL COMMENT '冻结金额(分)',
    points_held BIGINT NULL COMMENT '冻结积分数(仅POINTS)',
    coupon_id BIGINT NULL COMMENT '使用的券ID(仅COUPON)',
    member_id BIGINT NULL,

    -- 状态
    hold_status VARCHAR(32) NOT NULL DEFAULT 'HELD'
      COMMENT 'HELD|CONFIRMED|RELEASED',
    held_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    released_at TIMESTAMP NULL,
    release_reason VARCHAR(255) NULL,

    -- 关联
    payment_attempt_id BIGINT NULL COMMENT '外部支付attempt(仅EXTERNAL)',
    settlement_record_id BIGINT NULL COMMENT '确认后关联的settlement',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_sph_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_sph_session FOREIGN KEY (table_session_id) REFERENCES table_sessions(id),
    INDEX idx_sph_session (table_session_id, hold_status),
    INDEX idx_sph_member (member_id, hold_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**V078__alter_member_accounts_frozen.sql:**
```sql
ALTER TABLE member_accounts
  ADD COLUMN frozen_points BIGINT NOT NULL DEFAULT 0
    COMMENT '冻结积分(结算中)' AFTER points_balance,
  ADD COLUMN frozen_cash_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '冻结储值(结算中)' AFTER cash_balance_cents;
-- 可用积分 = points_balance - frozen_points
-- 可用储值 = cash_balance_cents - frozen_cash_cents
```

**V079__alter_settlement_records_stacking.sql:**
```sql
ALTER TABLE settlement_records
  ADD COLUMN points_deduct_cents BIGINT NOT NULL DEFAULT 0 AFTER total_amount_cents,
  ADD COLUMN points_deducted BIGINT NOT NULL DEFAULT 0 AFTER points_deduct_cents,
  ADD COLUMN cash_balance_cents BIGINT NOT NULL DEFAULT 0 AFTER points_deducted,
  ADD COLUMN coupon_discount_cents BIGINT NOT NULL DEFAULT 0 AFTER cash_balance_cents,
  ADD COLUMN promotion_discount_cents BIGINT NOT NULL DEFAULT 0 AFTER coupon_discount_cents,
  ADD COLUMN external_payment_cents BIGINT NOT NULL DEFAULT 0 AFTER promotion_discount_cents,
  ADD COLUMN coupon_id BIGINT NULL AFTER external_payment_cents,
  ADD COLUMN stacking_rule_id BIGINT NULL AFTER coupon_id;
```

### API（保持 table/session 模型）

```
-- 和现有 TableSettlementV2Controller 对齐
POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/preview-stacking
  Body: { memberId, usePoints, useCashBalance, couponId }
  Response: {
    sessionId,
    orderTotalCents,
    promotionDiscountCents,
    steps: [
      { order: 1, type: "POINTS", amountCents: 5000, pointsUsed: 500 },
      { order: 2, type: "COUPON", amountCents: 2000, couponName: "满100减20" },
      { order: 3, type: "CASH_BALANCE", amountCents: 3000 },
      { order: 4, type: "EXTERNAL", amountCents: 10000 }
    ],
    externalPaymentCents: 10000
  }

POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/collect-stacking
  Body: {
    memberId, usePoints, useCashBalance, couponId,
    externalPaymentMethod: "CASH" | "VIBECASH" | "CARD"
  }
  执行:
    1. 创建 settlement_payment_holds (status=HELD)
    2. 冻结积分/储值: member_accounts.frozen_points += X
    3. 标记券已使用
    4. 如果 external > 0: 创建 payment_attempt
    5. 外部支付成功回调后: holds → CONFIRMED, settlement_record 写入
    6. 外部支付失败: holds → RELEASED, 解冻积分/储值/券
```

### 冻结/确认/回滚流程

```
冻结:
  member_accounts.frozen_points += pointsHeld
  member_accounts.frozen_cash_cents += cashHeld
  INSERT settlement_payment_holds (hold_status = 'HELD')

确认 (外部支付成功后):
  member_accounts.points_balance -= pointsHeld
  member_accounts.frozen_points -= pointsHeld
  member_accounts.cash_balance_cents -= cashHeld
  member_accounts.frozen_cash_cents -= cashHeld
  UPDATE settlement_payment_holds SET hold_status = 'CONFIRMED'
  INSERT settlement_records (含各项明细)

释放 (外部支付失败/放弃):
  member_accounts.frozen_points -= pointsHeld
  member_accounts.frozen_cash_cents -= cashHeld
  coupon → 恢复 AVAILABLE
  UPDATE settlement_payment_holds SET hold_status = 'RELEASED'

服务重启恢复:
  启动时扫描 hold_status = 'HELD' 且 held_at > 30分钟前 的记录
  → 查询外部支付状态
  → 已成功: 确认
  → 未成功: 释放
```

---

## R3 修复：动态二维码

### 问题根因

原设计用 HMAC 签名但 URL 不传签名输入（timestamp、random），无法重算验证。更重要的是，QR 只保护入口，后续 API 仍然只靠 storeCode + tableCode。

### 修复方案：DB lookup + session token

不用 HMAC。改为：QR 码包含一个随机 token，扫码后服务端验证 token → 颁发 session-scoped JWT → 后续 API 用 JWT。

### DDL

**V073__alter_store_tables_qr.sql:**
```sql
ALTER TABLE store_tables
  ADD COLUMN qr_token VARCHAR(64) NULL COMMENT '当前QR token (UUID)' AFTER max_guests,
  ADD COLUMN qr_generated_at TIMESTAMP NULL AFTER qr_token,
  ADD COLUMN qr_expires_at TIMESTAMP NULL AFTER qr_generated_at;
```

### 流程

```
生成 QR:
  token = UUID.randomUUID()  // 简单随机，不需要 HMAC
  store_tables.qr_token = token
  store_tables.qr_expires_at = NOW() + 24h
  QR URL = https://{domain}/qr/{storeId}/{tableId}/{token}

扫码验证:
  GET /qr/{storeId}/{tableId}/{token}
  1. table = findByIdAndStoreId(tableId, storeId)
  2. table.qr_token != token → 400 "无效的二维码"
  3. table.qr_expires_at < NOW() → 400 "二维码已过期"
  4. 通过 → 生成 ordering_session_token (JWT):
     payload = { storeId, tableId, sessionId, exp: 4h }
     签名用 store.secret_key 或全局密钥
  5. 302 重定向到 /ordering?token={ordering_session_token}

后续 API 校验:
  所有 /api/v2/qr-ordering/** 接口要求 header:
    X-Ordering-Token: {ordering_session_token}
  服务端验证:
    - JWT 签名有效
    - 未过期
    - storeId/tableId 匹配请求参数
  无效 → 403 "请重新扫码"

刷新 QR (旧 token 立即失效):
  token = new UUID
  store_tables.qr_token = token
  // 已扫码的用户不受影响（他们持有的是 JWT，不是 QR token）
  // 新扫码的人必须用新 token
```

**stores 表加密钥：**
```sql
-- 放在 V071 里
ALTER TABLE stores
  ADD COLUMN jwt_secret VARCHAR(128) NULL COMMENT 'JWT签名密钥';
```

### 和现有前端对齐

现有 `qr-ordering-web` 用 `storeCode` + `tableCode` 访问 API。改造：
- URL 从 `/ordering/{storeCode}/{tableCode}` 改为 `/ordering?token={jwt}`
- 前端从 URL 解析 token，存 localStorage
- 所有 API 请求带 `X-Ordering-Token` header
- 后端 `QrOrderingV2Controller` 加 token 校验 filter

---

## R4 修复：buffet 价格语义持久化

### 问题根因

`toSubmittedItem()` (line 446-456) 只拷贝基础字段，不拷贝 buffet 标记。提交到厨房后结账/报表/退款都看不到 buffet 信息。

### 修复方案

1. `active_table_order_items` 加 buffet 字段
2. `submitted_order_items` 也加相同字段
3. `toSubmittedItem()` 拷贝 buffet 字段

### DDL

**V074__alter_order_items_buffet.sql:**
```sql
-- active 端
ALTER TABLE active_table_order_items
  ADD COLUMN is_buffet_included BOOLEAN NOT NULL DEFAULT FALSE
    COMMENT '套餐内免费项' AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '差价(分)' AFTER is_buffet_included,
  ADD COLUMN buffet_package_id BIGINT NULL
    COMMENT '关联的自助餐档位' AFTER buffet_surcharge_cents;

-- submitted 端（同步）
ALTER TABLE submitted_order_items
  ADD COLUMN is_buffet_included BOOLEAN NOT NULL DEFAULT FALSE
    COMMENT '套餐内免费项' AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '差价(分)' AFTER is_buffet_included,
  ADD COLUMN buffet_package_id BIGINT NULL
    COMMENT '关联的自助餐档位' AFTER buffet_surcharge_cents;
```

### Java 改动

```java
// ActiveTableOrderApplicationService.toSubmittedItem() 需要加:
private SubmittedOrderItemEntity toSubmittedItem(ActiveTableOrderItemEntity item) {
    SubmittedOrderItemEntity next = new SubmittedOrderItemEntity();
    next.setSkuId(item.getSkuId());
    next.setSkuCodeSnapshot(item.getSkuCodeSnapshot());
    next.setSkuNameSnapshot(item.getSkuNameSnapshot());
    next.setQuantity(item.getQuantity());
    next.setUnitPriceSnapshotCents(item.getUnitPriceSnapshotCents());
    next.setItemRemark(item.getItemRemark());
    next.setLineTotalCents(item.getLineTotalCents());
    // === 新增 ===
    next.setIsBuffetIncluded(item.getIsBuffetIncluded());
    next.setBuffetSurchargeCents(item.getBuffetSurchargeCents());
    next.setBuffetPackageId(item.getBuffetPackageId());
    return next;
}
```

### 结账时 buffet 金额计算

```
calculateBuffetBill(masterSessionId):
  session = findById(masterSessionId)
  package = findById(session.buffetPackageId)

  // 套餐基础价（从 session 的 guest_count 算，不从 items 算）
  packageTotal = package.priceCents * session.guestCount
              + (package.childPriceCents ?: package.priceCents) * session.childCount

  // 聚合所有 submitted_order_items（含并桌）
  sessionChain = getSessionChain(masterSessionId)
  allItems = submittedOrderItems.findBySessionIdIn(sessionChain)

  // 差价项: is_buffet_included=true AND buffet_surcharge_cents > 0
  surchargeTotal = allItems.stream()
    .filter(i -> i.isBuffetIncluded && i.buffetSurchargeCents > 0)
    .mapToLong(i -> i.buffetSurchargeCents * i.quantity)
    .sum()

  // 套餐外项: is_buffet_included=false
  extraTotal = allItems.stream()
    .filter(i -> !i.isBuffetIncluded)
    .mapToLong(i -> i.lineTotalCents)
    .sum()

  // 超时费
  overtimeFee = session.buffetOvertimeMinutes * package.overtimeFeePerMinuteCents

  return packageTotal + surchargeTotal + extraTotal + overtimeFee
```

---

## R5 修复：自助餐 session API

### 问题根因

设计说 buffet/start "创建 session"，但 API 需要 sessionId → 循环依赖。

### 修复

**实际流程：** session 在开台时已经创建（`findOrCreateOpenSession`）。buffet/start 只是在已有 session 上设置 buffet 字段。

```
开台流程 (已有逻辑):
  1. 服务员选桌 → tableStatus = OCCUPIED
  2. 创建/获取 session (findOrCreateOpenSession) → session.status = OPEN
  3. 到这里 session 已经有 id 了

buffet/start (新逻辑):
  POST /api/v2/stores/{storeId}/tables/{tableId}/buffet/start
  Body: { packageId, guestCount, childCount }

  1. session = findOpenSessionByTableId(tableId)  // 已存在
  2. session.dining_mode = 'BUFFET'
  3. session.buffet_package_id = packageId
  4. session.guest_count = guestCount
  5. session.child_count = childCount
  6. session.buffet_started_at = NOW()
  7. session.buffet_ends_at = NOW() + package.durationMinutes
  8. session.buffet_status = 'ACTIVE'

  注意: 用 tableId 找 session，不需要前端传 sessionId
```

### DDL

**V075__alter_table_sessions_buffet.sql:**
```sql
ALTER TABLE table_sessions
  ADD COLUMN dining_mode VARCHAR(32) NOT NULL DEFAULT 'A_LA_CARTE'
    COMMENT 'A_LA_CARTE|BUFFET|DELIVERY' AFTER guest_count,
  ADD COLUMN child_count INT NOT NULL DEFAULT 0 AFTER dining_mode,
  ADD COLUMN buffet_package_id BIGINT NULL AFTER child_count,
  ADD COLUMN buffet_started_at TIMESTAMP NULL AFTER buffet_package_id,
  ADD COLUMN buffet_ends_at TIMESTAMP NULL AFTER buffet_started_at,
  ADD COLUMN buffet_status VARCHAR(32) NULL
    COMMENT 'NULL(非自助)|ACTIVE|WARNING|OVERTIME|ENDED' AFTER buffet_ends_at,
  ADD COLUMN buffet_overtime_minutes INT NOT NULL DEFAULT 0 AFTER buffet_status;
```

---

## R6 修复：支付重试状态机

### 问题根因

现有 `payment_attempts.attempt_status` 使用：
`PENDING_CUSTOMER | SUCCEEDED | SETTLED | FAILED | EXPIRED`

原设计要改成另一套 `PENDING | PROCESSING | SUCCESS | ...`，但没有交代 webhook 和 settlement 如何迁移。

### 修复：只加不改

保留所有现有状态，只新增 `REPLACED`：

```
现有:
  PENDING_CUSTOMER → SUCCEEDED → SETTLED
  PENDING_CUSTOMER → FAILED
  PENDING_CUSTOMER → EXPIRED

新增:
  FAILED → REPLACED (换支付方式时)
```

### DDL

**V080__alter_payment_attempts_retry.sql:**
```sql
ALTER TABLE payment_attempts
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER attempt_status,
  ADD COLUMN max_retries INT NOT NULL DEFAULT 3 AFTER retry_count,
  ADD COLUMN failure_reason VARCHAR(512) NULL AFTER max_retries,
  ADD COLUMN failure_code VARCHAR(64) NULL AFTER failure_reason,
  ADD COLUMN replaced_by_attempt_id BIGINT NULL AFTER failure_code,
  ADD COLUMN parent_attempt_id BIGINT NULL AFTER replaced_by_attempt_id,
  ADD INDEX idx_pa_parent (parent_attempt_id);

-- attempt_status 不改枚举，只多用一个值 'REPLACED'
-- 现有 webhook 逻辑不受影响
```

### 业务逻辑

```
重试 (同方式):
  oldAttempt.attempt_status 必须是 FAILED
  oldAttempt.retry_count < max_retries
  → retry_count++, 重新调支付 API
  → 成功: SUCCEEDED → webhook 触发 SETTLED
  → 失败: FAILED, 记录 failure_reason

换方式:
  oldAttempt.attempt_status = 'REPLACED'
  oldAttempt.replaced_by_attempt_id = newAttempt.id
  newAttempt = 新建 attempt (新 method)
  newAttempt.parent_attempt_id = oldAttempt.id
  → 走正常支付流程

放弃:
  释放所有 settlement_payment_holds (R2)
  → holds 状态 → RELEASED
  → 解冻积分/储值
```

### 和现有 VibeCashPaymentApplicationService 的兼容

```
handleWebhook 不变:
  "payment.succeeded" → attempt.attemptStatus = "SUCCEEDED" → collectForTable(...)
  "payment.failed"    → attempt.attemptStatus = "FAILED"
  "payment.expired"   → attempt.attemptStatus = "EXPIRED"

新增:
  retry/switch 只在 FAILED 状态的 attempt 上操作
  SUCCEEDED/SETTLED/EXPIRED 的不允许重试
```

---

## R7 修复：审计日志拆表

### 问题根因

`action_log` 是 MCP/AI 工具日志（tool_name, params_json, result_json），审计报告要往里塞人工操作审计和审批流。三个关注点混在一起，连 store_id 都没有。

### 修复：新建 audit_trail 表，action_log 不动

**V081__create_audit_trail.sql:**
```sql
CREATE TABLE audit_trail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,

    -- 操作人
    actor_user_id BIGINT NOT NULL,
    actor_display_name VARCHAR(128) NULL,
    actor_ip VARCHAR(64) NULL,
    actor_device VARCHAR(128) NULL,

    -- 操作
    action VARCHAR(128) NOT NULL COMMENT 'TABLE_MERGE|PRICE_CHANGE|REFUND|...',
    action_category VARCHAR(64) NOT NULL COMMENT 'TABLE|ORDER|SKU|MEMBER|INVENTORY|SETTLEMENT|STAFF',
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW' COMMENT 'LOW|MEDIUM|HIGH|CRITICAL',

    -- 目标
    target_type VARCHAR(64) NOT NULL COMMENT '实体类型',
    target_id VARCHAR(128) NOT NULL COMMENT '实体ID',
    target_description VARCHAR(255) NULL COMMENT '人可读描述',

    -- 变更快照
    before_snapshot JSON NULL,
    after_snapshot JSON NULL,

    -- 审批（高风险操作需要审批）
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
```

**action_log 保持不变** — 继续作为 MCP/AI 工具日志。

### 审计切面

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();
    String actionCategory();
    RiskLevel riskLevel() default RiskLevel.LOW;
    boolean requiresApproval() default false;
    String targetType();
    String targetIdExpression() default "";
}

// 切面写 audit_trail 表，不写 action_log
```

---

## R8 修复：Migration 编号

### 现有 Flyway 版本

V001 到 V065（最大 V065__image_assets.sql）

### 新编号规划（从 V070 开始，预留 V066-V069 给紧急补丁）

| Version | Sprint | 内容 |
|---------|--------|------|
| V070 | S1 | ALTER table_sessions (merge + guest_count) |
| V071 | S1 | ALTER store_tables (zone, guests, status) + ALTER stores (jwt_secret) |
| V072 | S1 | CREATE table_merge_records |
| V073 | S1 | ALTER store_tables (QR token) |
| V074 | S2 | ALTER order_items buffet 字段 (active + submitted 两张表) |
| V075 | S2 | ALTER table_sessions (buffet 字段) |
| V076 | S3 | CREATE payment_stacking_rules |
| V077 | S3 | CREATE settlement_payment_holds |
| V078 | S3 | ALTER member_accounts (frozen 字段) |
| V079 | S3 | ALTER settlement_records (stacking 明细字段) |
| V080 | S3 | ALTER payment_attempts (retry/replace) |
| V081 | S1 | CREATE audit_trail |
| V082 | S2 | CREATE buffet_packages |
| V083 | S2 | CREATE buffet_package_items |
| V084 | S2 | ALTER products (menu_modes) |
| V085 | S2 | CREATE menu_time_slots + menu_time_slot_products |
| V086 | S4 | ALTER recipes (modifier consumption) |
| V087 | S4 | ALTER purchase_invoices (OCR) |
| V088 | S4 | CREATE sop_import_batches |
| V089 | S4 | CREATE inventory_driven_promotions |
| V090 | S5 | CREATE inspection_records + inspection_items |
| V091 | S5 | CREATE customer_feedback |
| V092 | S5 | CREATE external_integration_logs |
| V093 | S5 | CREATE cctv_events |
| V094 | S6 | CREATE report_snapshots |
| V095 | S6 | ALTER kitchen_stations (fallback) |

**共 26 个 migration，V070-V095，无间隔。**

---

## 附录：和原设计的差异摘要

| 原设计 | 修订 | 原因 |
|--------|------|------|
| 并台移动 order_items | 不动任何 items，结算时聚合 | active_table_orders UK(store_id, table_id) 限制 |
| table_sessions.status = SETTLED | 保持 OPEN/CLOSED | 现有代码只用这两个 |
| stackingCalculationId 缓存 | settlement_payment_holds 持久化 | 服务重启不丢冻结状态 |
| POST /settlements/{orderId} | POST /tables/{tableId}/settlement | 保持现有 table-centric 模型 |
| member.available_points | points_balance - frozen_points | 用现有字段 + 新 frozen 字段 |
| HMAC 签名 QR | UUID token + DB lookup + JWT | HMAC 无法从 URL 重算 |
| QR 只保护入口 | JWT 保护全链路 | 防止转发 URL 绕过 |
| buffet 字段只在 active_items | active + submitted 都有 | 结账依赖 submitted_order_items |
| POST /sessions/{id}/buffet/start | POST /tables/{tableId}/buffet/start | 不需要前端传 sessionId |
| 新状态机 PENDING/PROCESSING/SUCCESS | 保留 PENDING_CUSTOMER/SUCCEEDED/SETTLED/FAILED/EXPIRED + REPLACED | 不破坏 webhook |
| 改造 action_log | 新建 audit_trail，action_log 不动 | 三个关注点不混 |
| V070-V090 有间隔 | V070-V095 连续 | 消除编号冲突 |

---

*End of v2 spec. All 8 review issues addressed.*
