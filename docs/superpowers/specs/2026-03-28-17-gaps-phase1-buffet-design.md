# FounderPOS V3 — 17 Gap 补全 + Phase 1 自助餐设计

**Version:** V20260328011
**Date:** 2026-03-28
**Status:** DRAFT — 待审核
**Scope:** 17 个客户需求 gap 的数据模型 + 业务逻辑设计，以及 Phase 1 自助餐完整实现规划
**审核方式:** 交由独立 agent 做架构审计

---

## 0. 设计原则

1. **最优设计优先** — 不迁就旧表结构，该改就改，该删就删
2. **所有金额 BIGINT cents** — 全局约定不变
3. **审计四件套** — `created_at`, `updated_at`, `created_by`, `updated_by`（新表全部带）
4. **store_id 隔离** — 每张业务表必须有 `store_id`
5. **状态枚举用 VARCHAR(32)** — 不用 MySQL ENUM，方便扩展
6. **SKU 三层模型** — 顾客侧/厨房侧/库存侧，所有链路交汇于 SKU
7. **价格四级 fallback** — 门店+场景 > 品牌+场景 > 门店基础价 > SKU 默认价
8. **积分分批过期 FIFO** — `points_batches`，不是一个总数
9. **库存分批追踪 FIFO** — `inventory_batches`，按 expiry_date ASC 扣减

---

## 1. Gap 总览

| # | Gap | 优先级 | 类型 | 涉及模块 |
|---|-----|--------|------|---------|
| G01 | 并台 | P0 | 新功能 | 桌台/订单 |
| G02 | 支付叠加规则 | P0 | 新功能 | 结算/支付 |
| G03 | 清台中间态 | P1 | 改造 | 桌台 |
| G04 | 支付失败重试+换支付方式 | P1 | 改造 | 结算/支付 |
| G05 | 库存驱动促销 | P1 | 新功能 | 库存/促销 |
| G06 | 送货单 OCR 流程 | P1 | 新功能 | 库存 |
| G07 | SOP 批量导入 | P1 | 新功能 | 库存 |
| G08 | 巡店记录 | P1 | 新功能 | 运营 |
| G09 | 顾客反馈 / Wish List | P1 | 新功能 | CRM |
| G10 | 动态二维码 | P1 | 改造 | 桌台 |
| G11 | 报表自动摘要 | P1 | 新功能 | 报表/AI |
| G12 | 第三方对接日志 | P1 | 新功能 | 集成 |
| G13 | CCTV 事件表 | P1 | 新功能 | 安防 |
| G14 | 不同规格 SOP 消耗差异 | P1 | 改造 | 库存 |
| G15 | 多店对比报表 | P1 | 新功能 | 报表 |
| G16 | KDS 回退打印机 | P1 | 改造 | 厨房 |
| G17 | 审计日志统一覆盖人工操作 | P1 | 改造 | 审计 |

---

## 2. G01 — 并台（P0）

### 问题
两桌客人要合并到一桌消费、统一结账。当前 table_session 和 table 是 1:1，无法表达合并关系。

### 表改动

**store_tables — 加状态**
```sql
-- table_status 扩展枚举：AVAILABLE | OCCUPIED | RESERVED | PENDING_CLEAN | MERGED | DISABLED
ALTER TABLE store_tables
  MODIFY COLUMN table_status VARCHAR(32) DEFAULT 'AVAILABLE'
    COMMENT 'AVAILABLE|OCCUPIED|RESERVED|PENDING_CLEAN|MERGED|DISABLED';
```

**table_sessions — 加并台字段**
```sql
ALTER TABLE table_sessions
  ADD COLUMN merged_into_session_id BIGINT NULL COMMENT '被并入哪个session，NULL=未被并',
  ADD COLUMN total_guest_count INT DEFAULT 1 COMMENT '含并桌后总人数',
  ADD INDEX idx_ts_merged (merged_into_session_id);
```

**新表：table_merge_records**
```sql
CREATE TABLE table_merge_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    master_session_id BIGINT NOT NULL COMMENT '主桌session',
    merged_session_id BIGINT NOT NULL COMMENT '被合并的session',
    master_table_id BIGINT NOT NULL,
    merged_table_id BIGINT NOT NULL,
    merged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unmerged_at TIMESTAMP NULL COMMENT 'NULL=仍在合并中',
    operated_by BIGINT NOT NULL COMMENT '操作人user_id',
    reason VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tmr_store (store_id),
    INDEX idx_tmr_master (master_session_id),
    INDEX idx_tmr_merged (merged_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 状态机

```
store_tables.table_status:
  AVAILABLE → OCCUPIED (开台)
  AVAILABLE → RESERVED (预留)
  RESERVED → OCCUPIED (入座)
  OCCUPIED → PENDING_CLEAN (结账后)
  OCCUPIED → MERGED (被并入其他桌)
  PENDING_CLEAN → AVAILABLE (清台完成)
  MERGED → AVAILABLE (拆台还原)
  DISABLED ↔ AVAILABLE (启用/禁用)
```

### 业务逻辑

```
并台(masterTableId, mergedTableId, operatorId):
  前置校验:
    - 两桌 table_status == OCCUPIED
    - 同一 store_id
    - mergedSession.merged_into_session_id == NULL (未被并过)

  执行:
    1. mergedSession.merged_into_session_id = masterSession.id
    2. masterSession.total_guest_count += mergedSession.guest_count
    3. mergedTable.table_status = 'MERGED'
    4. 把 mergedSession 下所有未结账 active_table_order_items
       的 session_id 指向 masterSession
       (或者结账时按 merged_into_session_id 递归聚合)
    5. INSERT table_merge_records
    6. 发事件 TABLE_MERGED

拆台(mergeRecordId, operatorId):
  前置校验:
    - masterSession 未结账 (session_status != SETTLED)
    - mergeRecord.unmerged_at == NULL

  执行:
    1. 还原 order_items 到原 session（按 snapshot）
    2. mergedSession.merged_into_session_id = NULL
    3. mergedTable.table_status = 'OCCUPIED'
    4. masterSession.total_guest_count -= 原 guest_count
    5. UPDATE table_merge_records SET unmerged_at = NOW()
    6. 发事件 TABLE_UNMERGED

结账时并台处理:
  - 查 masterSession + 所有 merged_into_session_id == masterSession.id 的子 session
  - 汇总所有 order_items 计算总金额
  - 结账完成后：
    - masterTable → PENDING_CLEAN
    - 所有 mergedTable → AVAILABLE (已无 session)
    - 所有 mergedSession.session_status = SETTLED
```

### API

```
POST   /api/v2/stores/{storeId}/tables/merge
  Body: { masterTableId, mergedTableId }
  Response: { mergeRecordId, masterSession, mergedSession }

POST   /api/v2/stores/{storeId}/tables/unmerge
  Body: { mergeRecordId }
  Response: { masterSession, restoredSession }

GET    /api/v2/stores/{storeId}/tables/{tableId}/merge-info
  Response: { isMerged, mergeRecords[], masterTable?, mergedTables[] }
```

---

## 3. G02 — 支付叠加规则（P0）

### 问题
积分+储值+券+现金/卡能不能同时用？优先扣哪个？规则可配置。

### 新表：payment_stacking_rules

```sql
CREATE TABLE payment_stacking_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NULL COMMENT 'NULL=品牌级规则',
    rule_name VARCHAR(128) NOT NULL,

    -- 叠加开关
    allow_points_deduct BOOLEAN DEFAULT TRUE COMMENT '允许积分抵扣',
    allow_cash_balance BOOLEAN DEFAULT TRUE COMMENT '允许储值余额',
    allow_coupon BOOLEAN DEFAULT TRUE COMMENT '允许优惠券',
    allow_mixed_payment BOOLEAN DEFAULT TRUE COMMENT '允许多种支付方式组合',

    -- 扣减顺序（数字越小越先扣）
    points_priority INT DEFAULT 1 COMMENT '积分抵扣顺序',
    coupon_priority INT DEFAULT 2 COMMENT '优惠券顺序',
    cash_balance_priority INT DEFAULT 3 COMMENT '储值余额顺序',
    external_payment_priority INT DEFAULT 4 COMMENT '外部支付(现金/卡/QR)顺序',

    -- 积分抵扣限制
    max_points_deduct_percent INT DEFAULT 50 COMMENT '积分最多抵扣订单金额的百分比',
    points_to_cents_rate INT DEFAULT 100 COMMENT '多少积分=1分钱（默认100积分=1元=100分）',
    min_points_deduct BIGINT DEFAULT 0 COMMENT '最低使用积分数',

    -- 储值限制
    max_cash_balance_percent INT DEFAULT 100 COMMENT '储值最多抵扣订单金额的百分比',

    -- 优惠券限制
    max_coupons_per_order INT DEFAULT 1 COMMENT '每单最多用几张券',
    coupon_stackable_with_promotion BOOLEAN DEFAULT FALSE COMMENT '券和促销能不能叠加',

    -- 适用场景
    applicable_dining_modes JSON DEFAULT '["A_LA_CARTE","BUFFET","DELIVERY"]',
    applicable_order_min_cents BIGINT DEFAULT 0 COMMENT '最低订单金额才能叠加',

    -- 状态
    is_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0 COMMENT '多条规则时按priority取最高',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_psr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_psr_store (merchant_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 改造：settlement_records — 加分项明细

```sql
ALTER TABLE settlement_records
  ADD COLUMN points_deduct_amount_cents BIGINT DEFAULT 0 COMMENT '积分抵扣金额(分)',
  ADD COLUMN points_deducted BIGINT DEFAULT 0 COMMENT '使用的积分数',
  ADD COLUMN cash_balance_amount_cents BIGINT DEFAULT 0 COMMENT '储值扣款(分)',
  ADD COLUMN coupon_discount_cents BIGINT DEFAULT 0 COMMENT '优惠券减免(分)',
  ADD COLUMN promotion_discount_cents BIGINT DEFAULT 0 COMMENT '促销减免(分)',
  ADD COLUMN external_payment_cents BIGINT DEFAULT 0 COMMENT '外部支付金额(分)',
  ADD COLUMN coupon_id BIGINT NULL COMMENT '使用的优惠券ID',
  ADD COLUMN stacking_rule_id BIGINT NULL COMMENT '使用的叠加规则ID';
```

### 业务逻辑

```
结账叠加计算(orderId, memberPaymentRequest):
  输入:
    - orderTotalCents: 订单原始总金额
    - promotionDiscountCents: 促销引擎已算好的减免
    - memberPaymentRequest: {
        usePoints: boolean,
        useCashBalance: boolean,
        couponId: ?Long,
        externalPaymentMethod: String  // CASH | VISA | QR_VIBECASH | ...
      }

  执行:
    1. 加载 stacking_rule（store_id 匹配 > merchant_id 匹配，取 priority 最高的 active 规则）
    2. payableAmount = orderTotalCents - promotionDiscountCents
    3. 如果 coupon_stackable_with_promotion == false && promotionDiscountCents > 0:
         → 禁止使用优惠券

    按 priority 顺序逐项扣减:
    4. 积分抵扣 (如果 allow_points_deduct && usePoints):
         maxPointsDeduct = payableAmount * max_points_deduct_percent / 100
         availablePoints = member.available_points
         pointsValue = availablePoints / points_to_cents_rate
         actualDeduct = min(maxPointsDeduct, pointsValue, payableAmount)
         → 冻结积分，payableAmount -= actualDeduct

    5. 优惠券 (如果 allow_coupon && couponId != null):
         校验: 券有效期、适用门店、适用SKU、min_spend
         couponValue = 计算券面值
         actualCouponDiscount = min(couponValue, payableAmount)
         → 标记券已使用，payableAmount -= actualCouponDiscount

    6. 储值余额 (如果 allow_cash_balance && useCashBalance):
         maxCashDeduct = payableAmount * max_cash_balance_percent / 100
         availableCash = member.available_cash_cents
         actualCashDeduct = min(maxCashDeduct, availableCash, payableAmount)
         → 冻结储值，payableAmount -= actualCashDeduct

    7. 剩余金额走外部支付:
         externalPaymentCents = payableAmount
         → 调用支付适配器（现金/卡/QR）

    8. 写 settlement_record（含各项明细字段）
    9. 确认后：积分/储值从冻结转为实扣，生成 ledger 记录

  异常处理:
    - 任何一步失败 → 回滚前面步骤（解冻积分/储值）
    - 外部支付失败 → 见 G04（重试/换方式）
```

### API

```
POST /api/v2/settlements/{orderId}/calculate-stacking
  Body: { usePoints, useCashBalance, couponId }
  Response: {
    orderTotalCents,
    promotionDiscountCents,
    pointsDeductCents,
    couponDiscountCents,
    cashBalanceCents,
    externalPaymentCents,
    breakdownDetails[]
  }

POST /api/v2/settlements/{orderId}/confirm
  Body: { stackingCalculationId, externalPaymentMethod }
```

---

## 4. G03 — 清台中间态（P1）

### 问题
结账后桌台直接变 AVAILABLE，但实际需要清台（收餐具、擦桌子）。

### 改动
G01 已经把 `table_status` 扩展为包含 `PENDING_CLEAN`。

### 业务逻辑

```
结账完成后:
  table.table_status = 'PENDING_CLEAN'（不是直接 AVAILABLE）

清台:
  服务员在 POS 确认清台完成
  → table.table_status = 'AVAILABLE'
  → 发事件 TABLE_CLEANED

超时自动清台（可选配置）:
  store_configs 加配置项: auto_clean_timeout_minutes (默认 0=不自动)
  如果 > 0: PENDING_CLEAN 状态超过 N 分钟后自动变 AVAILABLE
```

### API

```
POST /api/v2/stores/{storeId}/tables/{tableId}/mark-clean
  Response: { tableId, newStatus: "AVAILABLE" }
```

---

## 5. G04 — 支付失败重试 + 换支付方式（P1）

### 问题
外部支付（刷卡/QR）失败后，当前流程直接中断。需要支持重试或换一种支付方式。

### 改造：payment_attempts

```sql
ALTER TABLE payment_attempts
  ADD COLUMN retry_count INT DEFAULT 0 COMMENT '重试次数',
  ADD COLUMN max_retries INT DEFAULT 3 COMMENT '最大重试次数',
  ADD COLUMN failure_reason VARCHAR(255) NULL COMMENT '失败原因',
  ADD COLUMN replaced_by_attempt_id BIGINT NULL COMMENT '被哪个新attempt替代',
  MODIFY COLUMN attempt_status VARCHAR(32) DEFAULT 'PENDING'
    COMMENT 'PENDING|PROCESSING|SUCCESS|FAILED|CANCELLED|REPLACED';
```

### 业务逻辑

```
支付失败处理(attemptId):
  1. 标记 attempt_status = 'FAILED', 记录 failure_reason
  2. 如果 retry_count < max_retries:
       → 前端提示"支付失败，是否重试？"
       → 重试: retry_count++, 重新调支付接口
  3. 如果不想重试 或 超过上限:
       → 前端提示"是否更换支付方式？"
       → 换方式: 标记当前 attempt_status = 'REPLACED'
       → 创建新 payment_attempt (新 method)，replaced_by_attempt_id 指向新的
       → 重新走支付流程

  4. 如果完全放弃:
       → 标记 attempt_status = 'CANCELLED'
       → 回滚 G02 的积分/储值冻结
       → settlement_record 保持 PENDING 状态

超时处理:
  payment_attempts 超过 5 分钟无响应 → 自动查询支付状态
  如果支付方已扣款但回调丢失 → 补偿确认
  如果支付方未扣款 → 标记 FAILED
```

### API

```
POST /api/v2/payments/{attemptId}/retry
  Response: { newAttemptId, status }

POST /api/v2/payments/{attemptId}/switch-method
  Body: { newPaymentMethod }
  Response: { newAttemptId, status }

POST /api/v2/payments/{attemptId}/cancel
  Response: { status, refundedPoints, refundedCashBalance }
```

---

## 6. G05 — 库存驱动促销（P1）

### 问题
临期原料自动生成促销草案，减少浪费。

### 新表：inventory_driven_promotions

```sql
CREATE TABLE inventory_driven_promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,

    -- 触发源
    trigger_type VARCHAR(32) NOT NULL COMMENT 'EXPIRING_BATCH|OVERSTOCK|SLOW_MOVING',
    inventory_item_id BIGINT NOT NULL,
    batch_id BIGINT NULL COMMENT '临期触发时关联的批次',

    -- 触发数据快照
    trigger_data JSON NOT NULL COMMENT '{"daysToExpiry":3,"remainingQty":50,"unit":"kg"}',

    -- 关联的 SKU（哪些菜品用到这个原料）
    affected_sku_ids JSON NOT NULL COMMENT '[101,102,105]',

    -- 生成的促销草案
    suggested_promotion_type VARCHAR(32) NOT NULL COMMENT 'PERCENT_DISCOUNT|AMOUNT_DISCOUNT|GIFT_SKU',
    suggested_discount_value INT NULL COMMENT '折扣百分比 或 减免金额(cents)',
    suggested_start_at TIMESTAMP NOT NULL,
    suggested_end_at TIMESTAMP NOT NULL,

    -- 审批流
    draft_status VARCHAR(32) DEFAULT 'DRAFT' COMMENT 'DRAFT|APPROVED|REJECTED|EXPIRED',
    promotion_rule_id BIGINT NULL COMMENT '审批通过后生成的 promotion_rules.id',
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    review_notes VARCHAR(255) NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_idp_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_idp_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    INDEX idx_idp_status (store_id, draft_status),
    INDEX idx_idp_trigger (store_id, trigger_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 业务逻辑

```
定时扫描（每天凌晨 + 每次入库后）:
  1. 查询 inventory_batches WHERE expiry_date <= NOW() + expiry_warning_days
     → trigger_type = 'EXPIRING_BATCH'

  2. 查询 inventory_items WHERE current_stock > safety_stock * 3
     → trigger_type = 'OVERSTOCK'

  3. 查询 过去 7 天销量为 0 的 SKU 关联的原料
     → trigger_type = 'SLOW_MOVING'

  对每个触发:
    4. 通过 recipes 反查 SKU（哪些菜品用到这个原料）
    5. 生成促销草案:
       - 临期 3 天内: 建议 30% 折扣
       - 临期 7 天内: 建议 20% 折扣
       - 积压: 建议 15% 折扣
       - 滞销: 建议推荐位+10% 折扣
    6. INSERT inventory_driven_promotions (draft_status = 'DRAFT')
    7. 通知店长/AI Operator 审批

  审批通过后:
    8. 调用 PromotionService 创建 promotion_rules
    9. 更新 promotion_rule_id
    10. draft_status = 'APPROVED'
```

### API

```
GET  /api/v2/stores/{storeId}/inventory-promotions?status=DRAFT
POST /api/v2/stores/{storeId}/inventory-promotions/{id}/approve
POST /api/v2/stores/{storeId}/inventory-promotions/{id}/reject
```

---

## 7. G06 — 送货单 OCR 流程（P1）

### 问题
手工录入送货单效率低，需要拍照 OCR 自动识别。

### 改造：purchase_invoices — 已有 scan_image_url 和 ocr_status

```sql
ALTER TABLE purchase_invoices
  ADD COLUMN ocr_raw_result JSON NULL COMMENT 'OCR原始识别结果',
  ADD COLUMN ocr_confidence DECIMAL(3,2) NULL COMMENT '识别置信度 0-1',
  ADD COLUMN ocr_reviewed BOOLEAN DEFAULT FALSE COMMENT '人工是否已复核',
  ADD COLUMN ocr_reviewed_by BIGINT NULL,
  ADD COLUMN ocr_reviewed_at TIMESTAMP NULL;
```

### 业务逻辑

```
OCR 流程:
  1. 员工拍照上传 → 存 image_assets → purchase_invoices.scan_image_url
  2. ocr_status = 'PROCESSING'
  3. 调用 OCR 服务（外部 API 或本地 Tesseract）:
     - 识别供应商名
     - 识别日期
     - 识别每行: 品名、数量、单位、单价、金额
  4. 结果存 ocr_raw_result (JSON):
     {
       "supplier": "鲜美食品",
       "date": "2026-03-28",
       "items": [
         {"name": "牛腩", "qty": 10, "unit": "kg", "unitPrice": 2500, "total": 25000},
         {"name": "洋葱", "qty": 5, "unit": "kg", "unitPrice": 300, "total": 1500}
       ],
       "total": 26500
     }
  5. ocr_status = 'COMPLETED', ocr_confidence = 0.85
  6. 自动匹配 inventory_items（按名称模糊匹配 + 商户历史）
  7. 生成 purchase_invoice_items 草稿
  8. 前端展示: 左边原图，右边识别结果，员工逐行确认/修正
  9. 确认后 ocr_reviewed = true，走正常入库流程
```

### API

```
POST /api/v2/stores/{storeId}/invoices/{id}/ocr-scan
  Body: { imageAssetId }
  Response: { ocrStatus, rawResult, matchedItems[] }

POST /api/v2/stores/{storeId}/invoices/{id}/ocr-confirm
  Body: { confirmedItems[] }
```

---

## 8. G07 — SOP 批量导入（P1）

### 问题
一个个录 SOP 配方太慢，需要 Excel/CSV 批量导入。

### 新表：sop_import_batches

```sql
CREATE TABLE sop_import_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(512) NOT NULL,

    -- 导入统计
    total_rows INT DEFAULT 0,
    success_rows INT DEFAULT 0,
    error_rows INT DEFAULT 0,
    error_details JSON NULL COMMENT '[{"row":3,"error":"SKU not found: ABC"}]',

    -- 状态
    import_status VARCHAR(32) DEFAULT 'UPLOADED'
      COMMENT 'UPLOADED|VALIDATING|VALIDATED|IMPORTING|COMPLETED|FAILED',

    imported_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,

    CONSTRAINT fk_sib_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_sib UNIQUE (store_id, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 业务逻辑

```
CSV/Excel 格式:
  sku_code | ingredient_code | consumption_qty | consumption_unit
  BEEF001  | RAW_BEEF        | 200             | g
  BEEF001  | RAW_ONION       | 50              | g
  BEEF001  | SAUCE_SOY       | 30              | ml

导入流程:
  1. 上传文件 → 存储 → INSERT sop_import_batches (UPLOADED)
  2. 异步校验:
     - 每行 sku_code 是否存在
     - 每行 ingredient_code 是否存在
     - consumption_qty > 0
     - consumption_unit 是否合法
     - 重复行检测
     → import_status = 'VALIDATED' 或记录 error_details
  3. 前端展示校验结果，用户确认导入
  4. 批量 UPSERT recipes 表:
     - 已有记录 → UPDATE consumption_qty
     - 新记录 → INSERT
  5. import_status = 'COMPLETED'
```

### API

```
POST /api/v2/stores/{storeId}/sop/import
  Body: multipart file
  Response: { batchId, status }

GET  /api/v2/stores/{storeId}/sop/import/{batchId}
  Response: { status, totalRows, successRows, errors[] }

POST /api/v2/stores/{storeId}/sop/import/{batchId}/confirm
```

---

## 9. G08 — 巡店记录（P1）

### 问题
区域经理巡店需要打卡、登记问题、拍照，总部要看汇总。

### 新表

**inspection_records（巡店记录）**
```sql
CREATE TABLE inspection_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    inspector_user_id BIGINT NOT NULL,

    -- 时间
    inspection_date DATE NOT NULL,
    check_in_at TIMESTAMP NULL,
    check_out_at TIMESTAMP NULL,
    check_in_lat DECIMAL(10,7) NULL,
    check_in_lng DECIMAL(10,7) NULL,

    -- 评分
    overall_score DECIMAL(3,1) NULL COMMENT '总分0-10',
    hygiene_score DECIMAL(3,1) NULL,
    service_score DECIMAL(3,1) NULL,
    food_quality_score DECIMAL(3,1) NULL,
    compliance_score DECIMAL(3,1) NULL,

    -- 状态
    inspection_status VARCHAR(32) DEFAULT 'IN_PROGRESS'
      COMMENT 'IN_PROGRESS|COMPLETED|SUBMITTED',
    summary TEXT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ir_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ir_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_ir_date (store_id, inspection_date),
    INDEX idx_ir_inspector (inspector_user_id, inspection_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**inspection_items（巡店检查项）**
```sql
CREATE TABLE inspection_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL COMMENT 'HYGIENE|SERVICE|FOOD|COMPLIANCE|OTHER',
    item_description VARCHAR(512) NOT NULL,
    severity VARCHAR(32) DEFAULT 'INFO' COMMENT 'INFO|WARNING|CRITICAL',

    -- 记录
    is_passed BOOLEAN NULL COMMENT 'NULL=未检查, true=通过, false=不通过',
    finding_notes TEXT NULL,
    photo_urls JSON NULL COMMENT '["url1","url2"]',

    -- 跟进
    requires_followup BOOLEAN DEFAULT FALSE,
    followup_deadline DATE NULL,
    followup_status VARCHAR(32) NULL COMMENT 'PENDING|IN_PROGRESS|RESOLVED',
    resolved_by BIGINT NULL,
    resolved_at TIMESTAMP NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ii_inspection FOREIGN KEY (inspection_id) REFERENCES inspection_records(id) ON DELETE CASCADE,
    INDEX idx_ii_followup (requires_followup, followup_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### API

```
POST /api/v2/inspections/check-in
  Body: { storeId, lat, lng }

POST /api/v2/inspections/{id}/items
  Body: { category, description, isPassed, notes, photoUrls[] }

POST /api/v2/inspections/{id}/check-out
  Body: { scores, summary }

GET  /api/v2/merchants/{merchantId}/inspections?dateFrom&dateTo&storeId
```

---

## 10. G09 — 顾客反馈 / Wish List（P1）

### 新表：customer_feedback

```sql
CREATE TABLE customer_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    member_id BIGINT NULL COMMENT '匿名反馈时为NULL',
    order_id BIGINT NULL COMMENT '关联订单（可选）',

    -- 反馈类型
    feedback_type VARCHAR(32) NOT NULL COMMENT 'REVIEW|COMPLAINT|SUGGESTION|WISH_LIST',

    -- 评分（REVIEW类型）
    overall_rating INT NULL COMMENT '1-5星',
    food_rating INT NULL,
    service_rating INT NULL,
    ambience_rating INT NULL,

    -- 内容
    content TEXT NULL,
    photo_urls JSON NULL,
    tags JSON NULL COMMENT '["太咸","上菜慢","推荐新品"]',

    -- Wish List 专属
    wished_item_name VARCHAR(255) NULL COMMENT '顾客想要的菜品',
    wish_vote_count INT DEFAULT 1 COMMENT '同类wish的投票数',

    -- 处理
    feedback_status VARCHAR(32) DEFAULT 'NEW'
      COMMENT 'NEW|ACKNOWLEDGED|IN_PROGRESS|RESOLVED|CLOSED',
    response_text TEXT NULL COMMENT '商家回复',
    responded_by BIGINT NULL,
    responded_at TIMESTAMP NULL,

    -- 来源
    source VARCHAR(32) DEFAULT 'QR_ORDER' COMMENT 'QR_ORDER|POS|APP|GOOGLE|MANUAL',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_cf_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_cf_type (store_id, feedback_type),
    INDEX idx_cf_status (store_id, feedback_status),
    INDEX idx_cf_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 业务逻辑

```
结账后触发:
  QR 点单结账后 → 弹出评价入口（可跳过）
  → 顾客提交评分+文字+照片
  → INSERT customer_feedback (feedback_type = 'REVIEW')
  → 如果会员: 赠送积分（参考 points_rules rule_type = 'REVIEW'）

Wish List 聚合:
  顾客提交 wished_item_name
  → 模糊匹配已有 wish: 如果相似度>80%，wish_vote_count++
  → 否则 INSERT 新记录
  → 定期汇总: vote_count > N 的推给 AI Operator 建议上新

反馈处理:
  COMPLAINT 类型 → 通知值班经理
  severity 升级规则: 1-2星自动标记为 COMPLAINT + 通知
```

### API

```
POST /api/v2/stores/{storeId}/feedback
  Body: { feedbackType, ratings, content, photoUrls[], wishedItemName? }

GET  /api/v2/stores/{storeId}/feedback?type&status&dateFrom&dateTo
GET  /api/v2/stores/{storeId}/wish-list?sortBy=votes

POST /api/v2/stores/{storeId}/feedback/{id}/respond
  Body: { responseText }
```

---

## 11. G10 — 动态二维码（P1）

### 问题
静态 QR 码可被复制/截屏滥用。

### 改造：store_tables

```sql
ALTER TABLE store_tables
  ADD COLUMN qr_token VARCHAR(128) NULL COMMENT '当前有效token',
  ADD COLUMN qr_generated_at TIMESTAMP NULL,
  ADD COLUMN qr_expires_at TIMESTAMP NULL;
```

### 业务逻辑

```
生成/刷新 QR:
  qr_token = HMAC-SHA256(storeSecret, tableId + timestamp + random)
  qr_expires_at = NOW() + 24h (可配: store_configs.qr_expiry_hours)
  QR 内容 = https://{domain}/qr/{storeId}/{tableId}?t={qr_token}

扫码验证:
  1. 解析 URL 参数
  2. 校验 qr_expires_at > NOW()
  3. 校验 HMAC 签名（用 storeSecret 重算对比）
  4. 通过 → 进入点单页，识别桌号
  5. 失败 → 提示"二维码已过期，请联系服务员"

自动刷新时机:
  - 每日凌晨 cron 刷新所有桌台
  - 清台完成(PENDING_CLEAN → AVAILABLE)时自动刷新
  - 手动刷新: 店长在后台点击刷新
```

### API

```
POST /api/v2/stores/{storeId}/tables/{tableId}/qr/refresh
GET  /api/v2/stores/{storeId}/tables/{tableId}/qr
  Response: { qrUrl, expiresAt }

GET  /qr/{storeId}/{tableId}?t={token}  — 公开接口，扫码入口
```

---

## 12. G11 — 报表自动摘要（P1）

### 问题
报表数据多，老板看不懂。需要 AI 生成自然语言摘要。

### 新表：report_snapshots

```sql
CREATE TABLE report_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    report_type VARCHAR(64) NOT NULL
      COMMENT 'DAILY_SUMMARY|WEEKLY_SUMMARY|MONTHLY_SUMMARY|CUSTOM',
    report_date DATE NOT NULL,

    -- 数据快照
    metrics_json JSON NOT NULL COMMENT '所有指标的结构化数据',

    -- AI 摘要
    ai_summary TEXT NULL COMMENT 'AI生成的自然语言摘要',
    ai_highlights JSON NULL COMMENT '亮点: ["营收环比+15%","牛肉菜品毛利下降"]',
    ai_warnings JSON NULL COMMENT '警告: ["库存低于安全线3项","差评增加"]',
    ai_suggestions JSON NULL COMMENT '建议: ["考虑推出午市套餐","补货牛腩"]',
    ai_generated_at TIMESTAMP NULL,
    ai_model_version VARCHAR(64) NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_rs UNIQUE (store_id, report_type, report_date),
    INDEX idx_rs_merchant (merchant_id, report_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 业务逻辑

```
每日凌晨（或手动触发）:
  1. 聚合当天数据:
     - 营收/订单数/客单价/翻台率
     - 分时段销售额
     - 堂食/外卖/自助占比
     - 库存预警数
     - 会员新增/消费
     - 员工工时/人效
  2. 存 metrics_json
  3. 调 AI Operator（经营顾问角色）:
     prompt = "分析以下餐厅经营数据，生成摘要、亮点、警告、建议。数据: {metrics_json}"
  4. 存 ai_summary, ai_highlights, ai_warnings, ai_suggestions
  5. 推送给老板/店长（WhatsApp/应用内通知）

环比/同比:
  加载前一天/前一周/去年同期的 report_snapshots
  → metrics_json 对比计算变化率
  → 纳入 AI prompt 上下文
```

### API

```
GET /api/v2/stores/{storeId}/reports/snapshot?type=DAILY_SUMMARY&date=2026-03-28
GET /api/v2/stores/{storeId}/reports/snapshot/{id}/ai-summary
POST /api/v2/stores/{storeId}/reports/snapshot/generate
  Body: { reportType, reportDate }
```

---

## 13. G12 — 第三方对接日志（P1）

### 问题
对接 Grab/Foodpanda/Google/商场 CRM 等外部系统，需要完整的请求/响应日志用于排查问题。

### 新表：external_integration_logs

```sql
CREATE TABLE external_integration_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NULL COMMENT '可为空（平台级调用）',
    merchant_id BIGINT NOT NULL,

    -- 调用信息
    integration_type VARCHAR(64) NOT NULL
      COMMENT 'GRAB|FOODPANDA|GOOGLE_RESERVATION|GOOGLE_REVIEW|MALL_CRM|GTO|PAYMENT|OCR',
    direction VARCHAR(16) NOT NULL COMMENT 'OUTBOUND|INBOUND',
    http_method VARCHAR(16) NULL,
    request_url VARCHAR(1024) NULL,
    request_headers JSON NULL,
    request_body TEXT NULL,

    -- 响应
    response_status INT NULL,
    response_headers JSON NULL,
    response_body TEXT NULL,
    latency_ms INT NULL,

    -- 结果
    result_status VARCHAR(32) NOT NULL COMMENT 'SUCCESS|FAILED|TIMEOUT|ERROR',
    error_message VARCHAR(512) NULL,

    -- 关联
    business_type VARCHAR(64) NULL COMMENT 'ORDER_SYNC|MENU_PUSH|REVIEW_FETCH|...',
    business_ref VARCHAR(128) NULL COMMENT '关联的业务ID',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_eil_type (merchant_id, integration_type, created_at),
    INDEX idx_eil_result (merchant_id, result_status, created_at),
    INDEX idx_eil_biz (business_type, business_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 业务逻辑

```
AOP 切面自动记录:
  所有 ExternalApiClient 的调用自动写日志
  → 请求前记录 request
  → 响应后记录 response + latency
  → 异常记录 error_message

清理策略:
  - SUCCESS 日志保留 30 天
  - FAILED/ERROR 日志保留 90 天
  - 定时任务清理过期日志

告警:
  连续 3 次 FAILED → 通知运维
  latency_ms > 5000 → 标记慢调用
```

### API

```
GET /api/v2/merchants/{merchantId}/integration-logs
  ?type=GRAB&result=FAILED&dateFrom&dateTo&page&size

GET /api/v2/merchants/{merchantId}/integration-logs/{id}
  Response: { 完整请求+响应详情 }

GET /api/v2/merchants/{merchantId}/integration-logs/stats
  Response: { byType: {GRAB: {total, success, failed, avgLatency}} }
```

---

## 14. G13 — CCTV 事件表（P1）

### 问题
CCTV AI 检测到事件（顾客滑倒、员工违规、异常聚集等）需要记录和告警。

### 新表：cctv_events

```sql
CREATE TABLE cctv_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    camera_id VARCHAR(128) NOT NULL COMMENT '摄像头标识',
    camera_location VARCHAR(255) NULL COMMENT '摄像头位置描述',

    -- 事件
    event_type VARCHAR(64) NOT NULL
      COMMENT 'SLIP_FALL|STAFF_VIOLATION|CROWD_ANOMALY|FIRE_SMOKE|THEFT_SUSPECT|HYGIENE_VIOLATION|CUSTOM',
    severity VARCHAR(32) DEFAULT 'INFO' COMMENT 'INFO|WARNING|CRITICAL|EMERGENCY',
    event_at TIMESTAMP NOT NULL COMMENT '事件发生时间',

    -- 证据
    snapshot_url VARCHAR(512) NULL COMMENT '事件截图',
    video_clip_url VARCHAR(512) NULL COMMENT '事件视频片段',
    ai_confidence DECIMAL(3,2) NULL COMMENT 'AI识别置信度 0-1',
    ai_description TEXT NULL COMMENT 'AI生成的事件描述',

    -- 处理
    event_status VARCHAR(32) DEFAULT 'NEW'
      COMMENT 'NEW|ACKNOWLEDGED|INVESTIGATING|RESOLVED|FALSE_ALARM',
    handled_by BIGINT NULL,
    handled_at TIMESTAMP NULL,
    handling_notes TEXT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ce_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_ce_type (store_id, event_type, event_at),
    INDEX idx_ce_severity (store_id, severity, event_status),
    INDEX idx_ce_time (store_id, event_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 业务逻辑

```
CCTV AI 检测到事件:
  1. AI 服务推送事件到 webhook
  2. INSERT cctv_events
  3. 按 severity 告警:
     - EMERGENCY: 立即推送所有在线管理员 + 拨打紧急联系人
     - CRITICAL: 推送值班经理
     - WARNING: 推送店长（非实时，汇总推送）
     - INFO: 仅记录

  误报处理:
  - 管理员标记 FALSE_ALARM
  - 累计误报率纳入 AI 模型优化
```

### API

```
POST /api/v2/stores/{storeId}/cctv-events  — webhook 接收
GET  /api/v2/stores/{storeId}/cctv-events?severity&status&dateFrom
POST /api/v2/stores/{storeId}/cctv-events/{id}/acknowledge
POST /api/v2/stores/{storeId}/cctv-events/{id}/resolve
  Body: { notes, isFalseAlarm }
```

---

## 15. G14 — 不同规格 SOP 消耗差异（P1）

### 问题
同一个 product 下，大份和小份的原料消耗不同。当前 recipes 表是 SKU 级别的，但没有处理修饰符对消耗的影响。

### 改造：recipes 表

```sql
ALTER TABLE recipes
  ADD COLUMN modifier_option_id BIGINT NULL
    COMMENT '关联的修饰符选项，NULL=基础消耗',
  ADD COLUMN consumption_multiplier DECIMAL(5,2) DEFAULT 1.00
    COMMENT '消耗倍率（相对基础量）',
  DROP INDEX uk_recipe,
  ADD UNIQUE INDEX uk_recipe_v2 (sku_id, inventory_item_id, modifier_option_id);
```

### 业务逻辑

```
消耗计算:
  基础消耗 = recipes WHERE sku_id = X AND modifier_option_id IS NULL
  修饰符消耗 = recipes WHERE sku_id = X AND modifier_option_id = 选中的选项

  场景举例 — "牛腩饭" (SKU):
    基础配方: 牛腩 200g, 米 150g, 酱油 30ml
    修饰符 "大份": modifier_option_id = 501, consumption_multiplier = 1.5
    修饰符 "加辣": modifier_option_id = 502, 额外消耗辣椒 10g

  最终消耗:
    如果点了"大份牛腩饭":
      牛腩 = 200g × 1.5 = 300g
      米 = 150g × 1.5 = 225g
      酱油 = 30ml × 1.5 = 45ml

    如果点了"大份牛腩饭 + 加辣":
      以上 + 辣椒 10g

  计算伪代码:
    baseRecipes = getRecipes(skuId, modifierOptionId = NULL)
    for each selectedModifier:
      modRecipe = getRecipes(skuId, modifierOptionId)
      if modRecipe.consumptionMultiplier != 1.0:
        // 倍率型: 所有基础消耗 × multiplier
        baseRecipes.forEach(r -> r.qty *= modRecipe.consumptionMultiplier)
      if modRecipe has extra items:
        // 额外型: 追加消耗
        baseRecipes.addAll(modRecipe.extraItems)
```

---

## 16. G15 — 多店对比报表（P1）

### 问题
区域经理/总部需要横向对比多家门店的经营数据。

### 复用 report_snapshots 表

G11 的 `report_snapshots` 已经按 store_id + report_date 存了每家店的数据。多店对比只需要查询层聚合。

### 业务逻辑

```
多店对比查询:
  输入: storeIds[], reportType, dateRange
  1. SELECT * FROM report_snapshots
     WHERE store_id IN (storeIds) AND report_type = X AND report_date BETWEEN A AND B
  2. 按 store_id 分组
  3. 对比维度:
     - 营收排名
     - 客单价排名
     - 翻台率排名
     - 人效排名
     - 库存损耗率
     - 好评率
  4. 生成对比矩阵 JSON
  5. AI 生成对比摘要（哪家店好在哪、差在哪）
```

### API

```
POST /api/v2/merchants/{merchantId}/reports/multi-store-compare
  Body: { storeIds[], reportType, dateFrom, dateTo, metrics[] }
  Response: {
    stores: [{ storeId, storeName, metrics: {} }],
    rankings: { revenue: [...], avgSpend: [...] },
    aiSummary: "..."
  }
```

---

## 17. G16 — KDS 回退打印机（P1）

### 问题
KDS 设备故障时，厨房无法接单。需要自动回退到打印机出单。

### 改造：kitchen_stations

```sql
ALTER TABLE kitchen_stations
  ADD COLUMN fallback_printer_ip VARCHAR(64) NULL COMMENT 'KDS故障时回退的打印机IP',
  ADD COLUMN kds_health_status VARCHAR(32) DEFAULT 'ONLINE'
    COMMENT 'ONLINE|OFFLINE|DEGRADED',
  ADD COLUMN last_heartbeat_at TIMESTAMP NULL COMMENT 'KDS最后心跳时间',
  ADD COLUMN fallback_mode VARCHAR(32) DEFAULT 'AUTO'
    COMMENT 'AUTO|MANUAL|DISABLED — AUTO=检测到离线自动切换';
```

### 业务逻辑

```
KDS 心跳监测:
  KDS 设备每 30s 发心跳 → 更新 last_heartbeat_at
  后端定时检查（每 60s）:
    如果 NOW() - last_heartbeat_at > 90s:
      kds_health_status = 'OFFLINE'
      如果 fallback_mode = 'AUTO' && fallback_printer_ip != NULL:
        → 该 station 后续 ticket 自动走打印机
        → 通知值班经理 "工作站 {name} KDS 离线，已切换打印机"

  KDS 恢复:
    收到心跳 → kds_health_status = 'ONLINE'
    → 自动切回 KDS
    → 通知 "KDS 已恢复"

出票逻辑改造:
  原: 直接推 KDS
  新:
    if station.kds_health_status == 'ONLINE':
      推 KDS WebSocket
    else if station.fallback_printer_ip != null:
      调打印服务
    else:
      标记 ticket 为 DELIVERY_FAILED, 告警
```

### API

```
POST /api/v2/stations/{stationId}/heartbeat
  Body: { deviceId, timestamp }

PUT  /api/v2/stations/{stationId}/fallback-config
  Body: { fallbackPrinterIp, fallbackMode }

GET  /api/v2/stores/{storeId}/stations/health
  Response: [{ stationId, name, kdsStatus, lastHeartbeat, fallbackActive }]
```

---

## 18. G17 — 审计日志统一覆盖人工操作（P1）

### 问题
当前 `action_log` 主要记录系统操作。人工操作（改价、手动调库存、退款审批等）覆盖不全。

### 改造：action_log

```sql
ALTER TABLE action_log
  ADD COLUMN actor_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM'
    COMMENT 'SYSTEM|USER|AI|WEBHOOK',
  ADD COLUMN actor_user_id BIGINT NULL,
  ADD COLUMN actor_ip VARCHAR(64) NULL,
  ADD COLUMN actor_device VARCHAR(128) NULL COMMENT '操作设备标识',
  ADD COLUMN risk_level VARCHAR(16) DEFAULT 'LOW' COMMENT 'LOW|MEDIUM|HIGH|CRITICAL',
  ADD COLUMN before_snapshot JSON NULL COMMENT '变更前数据快照',
  ADD COLUMN after_snapshot JSON NULL COMMENT '变更后数据快照',
  ADD COLUMN requires_approval BOOLEAN DEFAULT FALSE,
  ADD COLUMN approved_by BIGINT NULL,
  ADD COLUMN approved_at TIMESTAMP NULL;
```

### 需要强制审计的操作清单

| 操作 | risk_level | requires_approval |
|------|-----------|-------------------|
| 修改 SKU 价格 | HIGH | true (店长+) |
| 手动调整库存 | HIGH | true (店长+) |
| 大额退款 (> 阈值) | CRITICAL | true (经理+) |
| 撤销结账 | CRITICAL | true (经理+) |
| 修改会员积分/储值 | HIGH | true (经理+) |
| 删除订单 | CRITICAL | true (老板) |
| 修改员工薪资 | HIGH | true (老板) |
| 修改权限/角色 | HIGH | true (老板) |
| 打折超过 50% | MEDIUM | true (值班经理+) |
| 日终报表修正 | HIGH | true (财务+) |
| 普通点单 | LOW | false |
| 查看报表 | LOW | false |
| 打卡签到 | LOW | false |

### 业务逻辑

```
AOP 审计切面:
  @Audited(riskLevel = HIGH, requiresApproval = true)
  在 Service 方法上标注

  切面执行:
    1. 捕获方法参数 → before_snapshot (查 DB 当前值)
    2. 执行方法
    3. 捕获方法结果 → after_snapshot (查 DB 新值)
    4. INSERT action_log

  需要审批的操作:
    1. 方法执行前检查当前用户权限
    2. 如果权限不足 → 生成审批请求
       action_log.requires_approval = true
       通知有权限的人审批
    3. 审批通过后才实际执行
```

### API

```
GET /api/v2/stores/{storeId}/audit-logs
  ?riskLevel=HIGH&actorType=USER&dateFrom&dateTo&page&size

GET /api/v2/stores/{storeId}/audit-logs/pending-approval
POST /api/v2/stores/{storeId}/audit-logs/{id}/approve
POST /api/v2/stores/{storeId}/audit-logs/{id}/reject
```

---

## 19. Phase 1 自助餐实现设计

### 19.1 涉及的表（已存在于 docs/66）

| 表 | 状态 | 说明 |
|----|------|------|
| buffet_packages | 新建 | 自助档位 |
| buffet_package_items | 新建 | 档位-SKU 关联 |
| table_sessions | 改造 | 加 dining_mode, buffet_* 字段 |
| products | 改造 | 加 menu_modes JSON |
| menu_time_slots | 新建 | 时段菜单 |
| menu_time_slot_products | 新建 | 时段-商品关联 |
| sku_price_overrides | 新建 | 多场景价格 |

### 19.2 table_sessions 改造

```sql
ALTER TABLE table_sessions
  ADD COLUMN dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE'
    COMMENT 'A_LA_CARTE|BUFFET|DELIVERY',
  ADD COLUMN guest_count INT DEFAULT 1,
  ADD COLUMN buffet_package_id BIGINT NULL,
  ADD COLUMN buffet_started_at TIMESTAMP NULL,
  ADD COLUMN buffet_ends_at TIMESTAMP NULL,
  ADD COLUMN buffet_status VARCHAR(32) NULL
    COMMENT 'ACTIVE|WARNING|OVERTIME|ENDED';
```

### 19.3 自助餐完整流程

```
1. 开台选模式:
   服务员在 POS 或顾客扫码后选择 dining_mode = 'BUFFET'
   → 选择 buffet_package (档位)
   → 输入 guest_count (人数)
   → table_session 创建:
     dining_mode = 'BUFFET'
     buffet_package_id = 选中的档位
     guest_count = 人数
     buffet_started_at = NOW()
     buffet_ends_at = NOW() + package.duration_minutes
     buffet_status = 'ACTIVE'

2. 菜单过滤:
   前端请求菜单时带 dining_mode = 'BUFFET'
   → 只返回 products.menu_modes 包含 'BUFFET' 的商品
   → 每个 SKU 标注:
     - is_included = true (套餐内免费)
     - surcharge_cents > 0 (套餐内有差价)
     - is_included = false (套餐外，按原价)

3. 点单:
   顾客点 套餐内商品 → 数量不限（合理范围），不产生额外费用
   顾客点 有差价商品 → 差价计入 extra_charges
   顾客点 套餐外商品 → 原价计入 extra_charges
   → active_table_order_items 照常记录，但标注 is_buffet_included

4. 计时器:
   前端轮询 / WebSocket 推送 buffet 状态:
   - buffet_status = 'ACTIVE': 正常，绿色
   - 剩余时间 < warning_before_minutes:
     buffet_status = 'WARNING', 黄色
   - 超时:
     buffet_status = 'OVERTIME', 红色
     开始计算超时费: (elapsed - duration) * overtime_fee_per_minute_cents

5. 结账:
   总金额 = (package.price_cents × guest_count)
          + sum(surcharge_items)
          + sum(extra_items_at_original_price)
          + overtime_fee
          - promotion_discount (如果有)
   → 走 G02 的支付叠加流程

6. 单据:
   打印单据分区显示:
   ┌─────────────────────────┐
   │ 自助餐 - 豪华海鲜档     │
   │ 人数: 4  时长: 90min     │
   │ 套餐: 4 × $68 = $272    │
   ├─────────────────────────┤
   │ 加点项:                  │
   │ 三文鱼刺身(差价) × 2  $12│
   │ 啤酒(套餐外) × 3    $27 │
   ├─────────────────────────┤
   │ 超时费: 15min × $2 = $30 │
   ├─────────────────────────┤
   │ 小计: $341               │
   │ 会员折扣: -$34           │
   │ 应付: $307               │
   └─────────────────────────┘
```

### 19.4 active_table_order_items 改造

```sql
ALTER TABLE active_table_order_items
  ADD COLUMN is_buffet_included BOOLEAN DEFAULT FALSE
    COMMENT 'true=套餐内免费项',
  ADD COLUMN buffet_surcharge_cents BIGINT DEFAULT 0
    COMMENT '套餐内差价（非0时有差价）';
```

### 19.5 Flyway Migration 规划

| Migration | 内容 |
|-----------|------|
| V070 | buffet_packages + buffet_package_items |
| V071 | ALTER table_sessions (dining_mode, buffet_*) |
| V072 | ALTER products (menu_modes) |
| V073 | menu_time_slots + menu_time_slot_products |
| V074 | sku_price_overrides |
| V075 | ALTER active_table_order_items (buffet 字段) |

### 19.6 后端模块设计

```
com.developer.pos.v2.buffet/
├── application/
│   ├── BuffetPackageApplicationService.java
│   │   — CRUD 档位 + 商品绑定
│   ├── BuffetSessionApplicationService.java
│   │   — 开台选档、计时、超时、结账金额计算
│   └── BuffetMenuApplicationService.java
│       — 自助餐菜单过滤、价格标注
├── domain/
│   ├── BuffetPackage.java (Entity)
│   ├── BuffetPackageItem.java (Entity)
│   ├── BuffetSession.java (Value Object, 从 table_session 提取)
│   ├── BuffetPricingCalculator.java
│   │   — 套餐价 + 差价 + 超时费计算
│   └── BuffetTimerService.java
│       — 定时检查超时，更新 buffet_status
├── infrastructure/
│   ├── BuffetPackageRepository.java
│   └── BuffetPackageItemRepository.java
└── interfaces/
    └── rest/
        ├── BuffetPackageController.java
        │   — /api/v2/stores/{storeId}/buffet-packages
        └── BuffetSessionController.java
            — /api/v2/sessions/{sessionId}/buffet
```

### 19.7 前端改动

**qr-ordering-web (顾客端):**
```
扫码后:
  → 新增模式选择页（单点 / 自助）
  → 自助餐: 选档位 → 显示套餐菜单（标注免费/差价/套餐外）
  → 计时器悬浮组件（倒计时）
  → 结账页: 分区显示金额
```

**android-preview-web (POS 端):**
```
开台流程:
  → 新增 dining_mode 选择
  → 自助餐: 选档位 + 输入人数
  → 桌台列表: 显示自助餐倒计时、超时标红
  → 结账: 自助餐专属结账逻辑
```

**pc-admin (后台):**
```
新增页面:
  → 自助餐档位管理（CRUD）
  → 档位-商品绑定管理
  → 时段菜单配置
```

### 19.8 API 汇总

```
-- 档位管理
GET    /api/v2/stores/{storeId}/buffet-packages
POST   /api/v2/stores/{storeId}/buffet-packages
PUT    /api/v2/stores/{storeId}/buffet-packages/{id}
DELETE /api/v2/stores/{storeId}/buffet-packages/{id}

-- 档位商品
GET    /api/v2/buffet-packages/{pkgId}/items
POST   /api/v2/buffet-packages/{pkgId}/items/batch
DELETE /api/v2/buffet-packages/{pkgId}/items/{itemId}

-- 自助餐 session
POST   /api/v2/sessions/{sessionId}/buffet/start
  Body: { packageId, guestCount }
GET    /api/v2/sessions/{sessionId}/buffet/status
  Response: { buffetStatus, remainingMinutes, overtimeFee }

-- 自助餐菜单
GET    /api/v2/stores/{storeId}/menu?diningMode=BUFFET&packageId={pkgId}
  Response: { categories: [{ products: [{ skus: [{ isIncluded, surcharge }] }] }] }

-- 自助餐结账
POST   /api/v2/sessions/{sessionId}/buffet/calculate
  Response: { packageTotal, surchargeTotal, extraTotal, overtimeFee, grandTotal }
```

---

## 20. 改动汇总

### 新表（10 张）

| # | 表 | 所属 Gap |
|---|---|---------|
| 1 | table_merge_records | G01 并台 |
| 2 | payment_stacking_rules | G02 支付叠加 |
| 3 | sop_import_batches | G07 SOP导入 |
| 4 | inspection_records | G08 巡店 |
| 5 | inspection_items | G08 巡店 |
| 6 | customer_feedback | G09 反馈 |
| 7 | report_snapshots | G11 报表摘要 |
| 8 | external_integration_logs | G12 对接日志 |
| 9 | cctv_events | G13 CCTV |
| 10 | inventory_driven_promotions | G05 库存促销 |

### 改造表（8 张）

| 表 | 改动 | 所属 Gap |
|----|------|---------|
| store_tables | +qr_token, +qr_expires_at, table_status 扩展 | G01/G03/G10 |
| table_sessions | +merged_into_session_id, +dining_mode, +buffet_* | G01/Phase1 |
| settlement_records | +各项叠加金额明细字段 | G02 |
| payment_attempts | +retry_count, +failure_reason, +replaced_by | G04 |
| purchase_invoices | +ocr_* 字段 | G06 |
| recipes | +modifier_option_id, +consumption_multiplier | G14 |
| kitchen_stations | +fallback_printer_ip, +kds_health_status | G16 |
| action_log | +actor_*, +risk_level, +snapshots, +approval | G17 |
| active_table_order_items | +is_buffet_included, +buffet_surcharge | Phase1 |
| products | +menu_modes | Phase1 |

### Phase 1 新表（4 张，来自 docs/66，确认纳入）

| 表 | 说明 |
|----|------|
| buffet_packages | 自助档位 |
| buffet_package_items | 档位-SKU 关联 |
| menu_time_slots | 时段定义 |
| menu_time_slot_products | 时段-商品关联 |

### Flyway Migration 规划

| Version | 内容 | 依赖 |
|---------|------|------|
| V070 | buffet_packages + buffet_package_items | - |
| V071 | ALTER table_sessions (dining_mode, buffet_*, merged_*) | - |
| V072 | ALTER products (menu_modes), ALTER active_table_order_items (buffet) | - |
| V073 | menu_time_slots + menu_time_slot_products | - |
| V074 | sku_price_overrides | - |
| V075 | ALTER store_tables (qr_*, table_status 扩展) | - |
| V076 | table_merge_records | V071 |
| V077 | payment_stacking_rules | - |
| V078 | ALTER settlement_records (叠加明细) | V077 |
| V079 | ALTER payment_attempts (retry/replace) | - |
| V080 | inventory_driven_promotions | - |
| V081 | ALTER purchase_invoices (OCR 字段) | - |
| V082 | sop_import_batches | - |
| V083 | ALTER recipes (modifier 消耗差异) | - |
| V084 | inspection_records + inspection_items | - |
| V085 | customer_feedback | - |
| V086 | report_snapshots | - |
| V087 | external_integration_logs | - |
| V088 | cctv_events | - |
| V089 | ALTER kitchen_stations (fallback) | - |
| V090 | ALTER action_log (统一审计) | - |

---

## 21. 架构决策记录

| # | 决策 | 原因 |
|---|------|------|
| D01 | 并台用 merge_records 表 + session 指针，不拷贝 order_items | 拷贝会导致数据不一致；指针方式结账时聚合即可 |
| D02 | 支付叠加规则独立表，不硬编码 | 不同商户/门店可能有不同策略 |
| D03 | 扣减顺序用 priority 字段，不用固定枚举 | 商户可能想先扣储值再扣积分 |
| D04 | 清台是 table_status 的一个状态，不是独立流程 | 最简单，不需要新表 |
| D05 | OCR 结果存 JSON，不是独立表 | 一次性使用，确认后数据进 invoice_items |
| D06 | SOP 批量导入有独立 batch 表 | 需要追踪导入历史和错误 |
| D07 | 库存驱动促销生成草案，不自动创建促销 | 避免误操作，需要人工/AI 审批 |
| D08 | KDS 回退用心跳检测，不是手动切换 | 自动化更可靠，但保留 MANUAL 模式选项 |
| D09 | 审计日志改造 action_log，不新建表 | 统一审计入口，不分散 |
| D10 | 修饰符消耗用 multiplier + 额外行双模式 | "大份"是倍率型，"加辣"是追加型，需要两种 |
| D11 | 多店对比复用 report_snapshots | 已有每店每日数据，无需新表 |
| D12 | 动态 QR 用 HMAC 签名 + 过期时间 | 简单有效，不需要额外的 token 表 |

---

## 22. 已知的旧表优化建议

在做 17 gap 设计时发现的现有表问题，**建议在这轮一起改**：

| 表 | 问题 | 建议 |
|----|------|------|
| store_tables | 缺 zone/min_guests/max_guests | V075 一起加 |
| recipes | uk_recipe 不含 modifier_option_id | V083 改为 uk_recipe_v2 |
| action_log | 缺 actor 信息，无法区分人/系统/AI | V090 统一改 |
| settlement_records | 只有总金额，看不出积分/储值/券各扣了多少 | V078 加明细字段 |
| payment_attempts | 无重试/替换链路 | V079 加字段 |

---

*End of spec. Ready for external agent review.*
