# 库存管理完整数据模型

**Version:** V20260328006
**Date:** 2026-03-28
**Status:** DRAFT

---

## 1. 库存全链路

```
供应商报价 → 采购订单 → 送货入库（批次+保质期）→ 库存在库
                                                    ↓
                              销售扣减（SKU→SOP→原料）← 订单结账
                                                    ↓
                              库存预警 → 订货建议 → 采购订单（循环）
                                                    ↓
                              月度盘点 → 账实差异 → 损耗报告
```

---

## 2. doc/66 已有的表（保留不变）

| 表 | 用途 | 状态 |
|----|------|------|
| inventory_items | 原料主数据 | ✅ 保留 |
| recipes | SOP 配方 | ✅ 保留 |
| purchase_invoices | 送货单 | ✅ 保留 |
| purchase_invoice_items | 送货单明细 | ✅ 保留 |
| inventory_movements | 库存变动流水 | ✅ 保留，加字段 |
| suppliers | 供应商 | ✅ 保留，加字段 |
| order_suggestions | 订货建议 | ✅ 保留，加字段 |

---

## 3. 现有表改动

### 3.1 inventory_items — 加批次和换算字段

```sql
-- 多级单位换算
ALTER TABLE inventory_items ADD COLUMN purchase_unit VARCHAR(32) NULL AFTER unit;
ALTER TABLE inventory_items ADD COLUMN purchase_to_stock_ratio DECIMAL(10,4) DEFAULT 1.0 AFTER purchase_unit;
ALTER TABLE inventory_items ADD COLUMN usage_unit VARCHAR(32) NULL AFTER purchase_to_stock_ratio;
ALTER TABLE inventory_items ADD COLUMN stock_to_usage_ratio DECIMAL(10,4) DEFAULT 1.0 AFTER usage_unit;

-- 保质期管理
ALTER TABLE inventory_items ADD COLUMN shelf_life_days INT NULL AFTER expiry_warning_days;
ALTER TABLE inventory_items ADD COLUMN requires_batch_tracking BOOLEAN DEFAULT FALSE AFTER shelf_life_days;

-- 采购参考
ALTER TABLE inventory_items ADD COLUMN default_supplier_id BIGINT NULL AFTER item_status;
ALTER TABLE inventory_items ADD COLUMN last_purchase_price_cents BIGINT NULL AFTER default_supplier_id;
ALTER TABLE inventory_items ADD COLUMN avg_daily_usage DECIMAL(14,4) DEFAULT 0 AFTER last_purchase_price_cents;
```

**三级单位换算示例：**

| 原料 | purchase_unit | stock_unit | usage_unit | 换算 |
|------|--------------|------------|------------|------|
| 牛肉 | 箱 | kg | 克 | 1箱=10kg, 1kg=1000克 |
| 食用油 | 桶 | 升 | 毫升 | 1桶=5升, 1升=1000毫升 |
| 鸡蛋 | 箱 | 个 | 个 | 1箱=360个, 1个=1个 |

```
采购 2 箱牛肉
→ purchase_to_stock_ratio = 10 → 入库 20 kg
→ SOP 消耗 500 克牛肉
→ stock_to_usage_ratio = 1000 → 扣减 0.5 kg
```

### 3.2 suppliers — 加联系和评价字段

```sql
ALTER TABLE suppliers ADD COLUMN payment_terms VARCHAR(64) NULL AFTER address;
ALTER TABLE suppliers ADD COLUMN lead_time_days INT DEFAULT 1 AFTER payment_terms;
ALTER TABLE suppliers ADD COLUMN rating DECIMAL(2,1) NULL AFTER lead_time_days;
ALTER TABLE suppliers ADD COLUMN notes TEXT NULL AFTER rating;
```

### 3.3 inventory_movements — 加批次关联

```sql
ALTER TABLE inventory_movements ADD COLUMN batch_id BIGINT NULL AFTER inventory_item_id;
ALTER TABLE inventory_movements ADD COLUMN unit_cost_cents BIGINT NULL AFTER quantity_change;
```

### 3.4 order_suggestions — 加供应商和采购单关联

```sql
ALTER TABLE order_suggestions ADD COLUMN supplier_id BIGINT NULL AFTER inventory_item_id;
ALTER TABLE order_suggestions ADD COLUMN purchase_order_id BIGINT NULL AFTER approved_by;
ALTER TABLE order_suggestions ADD COLUMN estimated_cost_cents BIGINT NULL AFTER suggested_qty;
```

---

## 4. 新增表

### 4.1 inventory_batches（原料批次）

追踪每批原料的来源、保质期、剩余量，支持先进先出(FIFO)：

```sql
CREATE TABLE inventory_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,

    -- 来源
    source_type VARCHAR(32) NOT NULL,
    source_ref VARCHAR(128) NULL,
    supplier_id BIGINT NULL,

    -- 数量
    received_qty DECIMAL(14,4) NOT NULL,
    remaining_qty DECIMAL(14,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,

    -- 成本
    unit_cost_cents BIGINT NULL,
    total_cost_cents BIGINT NULL,

    -- 保质期
    production_date DATE NULL,
    expiry_date DATE NULL,
    received_date DATE NOT NULL,

    -- 状态
    batch_status VARCHAR(32) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ib_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ib_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_ib_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT uk_ib UNIQUE (store_id, batch_no),
    INDEX idx_ib_item_expiry (inventory_item_id, expiry_date),
    INDEX idx_ib_status (store_id, batch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**source_type：** `PURCHASE`（采购入库）| `TRANSFER`（调拨）| `ADJUSTMENT`（盘点调整）| `RETURN`（退货入库）

**batch_status：** `ACTIVE` | `DEPLETED`（用完）| `EXPIRED`（过期）| `DISPOSED`（报损处理）

**FIFO 逻辑：** 扣减时按 `expiry_date ASC` 排序，先过期的先扣。

### 4.2 purchase_orders（采购订单）

从订货建议生成正式采购单，发给供应商：

```sql
CREATE TABLE purchase_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    po_no VARCHAR(64) NOT NULL,
    supplier_id BIGINT NOT NULL,

    -- 金额
    total_amount_cents BIGINT DEFAULT 0,
    currency_code VARCHAR(16) DEFAULT 'SGD',

    -- 状态流
    po_status VARCHAR(32) DEFAULT 'DRAFT',
    submitted_at TIMESTAMP NULL,
    confirmed_at TIMESTAMP NULL,
    received_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,

    -- 操作人
    created_by BIGINT NULL,
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,

    -- 期望交付
    expected_delivery_date DATE NULL,
    actual_delivery_date DATE NULL,
    delivery_notes TEXT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_po_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT uk_po UNIQUE (store_id, po_no),
    INDEX idx_po_status (store_id, po_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**po_status 状态流：**

```
DRAFT → SUBMITTED → CONFIRMED → PARTIALLY_RECEIVED → RECEIVED → CLOSED
                  → REJECTED
                  → CANCELLED
```

### 4.3 purchase_order_items（采购订单明细）

```sql
CREATE TABLE purchase_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    ordered_qty DECIMAL(10,4) NOT NULL,
    ordered_unit VARCHAR(32) NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    line_total_cents BIGINT NOT NULL,
    received_qty DECIMAL(10,4) DEFAULT 0,
    item_status VARCHAR(32) DEFAULT 'PENDING',

    CONSTRAINT fk_poi_po FOREIGN KEY (po_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_poi_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**item_status：** `PENDING` | `PARTIALLY_RECEIVED` | `RECEIVED` | `CANCELLED`

### 4.4 supplier_price_history（供应商报价历史）

```sql
CREATE TABLE supplier_price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    unit VARCHAR(32) NOT NULL,
    price_cents BIGINT NOT NULL,
    effective_date DATE NOT NULL,
    source_type VARCHAR(32) DEFAULT 'INVOICE',
    source_ref VARCHAR(128) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sph_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_sph_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    INDEX idx_sph_item_date (inventory_item_id, effective_date DESC),
    INDEX idx_sph_supplier_item (supplier_id, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**用途：**
- 进货单录入时自动记录价格
- 长周期比价：同一原料不同供应商的价格趋势
- AI 建议：推荐最低价供应商

### 4.5 stocktake_tasks（盘点任务）

```sql
CREATE TABLE stocktake_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    stocktake_type VARCHAR(32) NOT NULL DEFAULT 'FULL',
    stocktake_date DATE NOT NULL,
    task_status VARCHAR(32) DEFAULT 'PENDING',

    -- 统计
    total_items INT DEFAULT 0,
    counted_items INT DEFAULT 0,
    variance_items INT DEFAULT 0,
    total_variance_cost_cents BIGINT DEFAULT 0,

    -- 操作人
    created_by BIGINT NULL,
    completed_by BIGINT NULL,
    approved_by BIGINT NULL,
    completed_at TIMESTAMP NULL,
    approved_at TIMESTAMP NULL,

    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stt_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_stt UNIQUE (store_id, task_no),
    INDEX idx_stt_date (store_id, stocktake_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**stocktake_type：** `FULL`（全量盘点）| `PARTIAL`（部分盘点）| `SPOT_CHECK`（抽查）

**task_status：** `PENDING` → `IN_PROGRESS` → `COMPLETED` → `APPROVED` | `VOID`

### 4.6 stocktake_items（盘点明细）

```sql
CREATE TABLE stocktake_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,

    -- 账面 vs 实盘
    system_qty DECIMAL(14,4) NOT NULL,
    counted_qty DECIMAL(14,4) NULL,
    variance_qty DECIMAL(14,4) NULL,
    unit VARCHAR(32) NOT NULL,

    -- 差异成本
    unit_cost_cents BIGINT NULL,
    variance_cost_cents BIGINT NULL,

    -- 差异原因
    variance_reason VARCHAR(32) NULL,
    notes VARCHAR(255) NULL,

    counted_at TIMESTAMP NULL,
    counted_by BIGINT NULL,

    CONSTRAINT fk_sti_task FOREIGN KEY (task_id) REFERENCES stocktake_tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_sti_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT uk_sti UNIQUE (task_id, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**variance_reason：** `NORMAL_LOSS`（正常损耗）| `WASTE`（报损）| `THEFT`（盗损）| `COUNTING_ERROR`（计数错误）| `SPILLAGE`（洒漏）| `UNKNOWN`（不明）

**盘点流程：**

```
1. 生成盘点任务（stocktake_tasks）
2. 系统自动填入每个原料的 system_qty（账面库存）
3. 员工录入 counted_qty（实盘数量）
4. 系统计算 variance_qty = counted_qty - system_qty
5. 员工填写 variance_reason
6. 店长审批
7. 审批后 → 自动生成 inventory_movements (ADJUSTMENT) 修正库存
8. 生成损耗报告
```

### 4.7 waste_records（报损记录）

日常报损（不用等月度盘点）：

```sql
CREATE TABLE waste_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    waste_no VARCHAR(64) NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    batch_id BIGINT NULL,
    waste_qty DECIMAL(14,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    unit_cost_cents BIGINT NULL,
    total_cost_cents BIGINT NULL,
    waste_reason VARCHAR(32) NOT NULL,
    waste_detail VARCHAR(255) NULL,
    recorded_by BIGINT NULL,
    approved_by BIGINT NULL,
    waste_status VARCHAR(32) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_wr_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_wr_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches(id),
    CONSTRAINT uk_wr UNIQUE (store_id, waste_no),
    INDEX idx_wr_date (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**waste_reason：** `EXPIRED`（过期）| `SPOILED`（变质）| `DAMAGED`（损坏）| `OVERPRODUCTION`（多做了）| `SPILLED`（洒漏）| `OTHER`

### 4.8 inventory_transfers（门店间调拨）

多店场景下，门店间原料调拨：

```sql
CREATE TABLE inventory_transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_no VARCHAR(64) NOT NULL,
    from_store_id BIGINT NOT NULL,
    to_store_id BIGINT NOT NULL,
    transfer_status VARCHAR(32) DEFAULT 'PENDING',

    requested_by BIGINT NULL,
    approved_by BIGINT NULL,
    shipped_at TIMESTAMP NULL,
    received_at TIMESTAMP NULL,

    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_it_from FOREIGN KEY (from_store_id) REFERENCES stores(id),
    CONSTRAINT fk_it_to FOREIGN KEY (to_store_id) REFERENCES stores(id),
    CONSTRAINT uk_it UNIQUE (transfer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**transfer_status：** `PENDING` → `APPROVED` → `SHIPPED` → `RECEIVED` | `REJECTED` | `CANCELLED`

### 4.9 inventory_transfer_items（调拨明细）

```sql
CREATE TABLE inventory_transfer_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    batch_id BIGINT NULL,
    transfer_qty DECIMAL(14,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    received_qty DECIMAL(14,4) NULL,

    CONSTRAINT fk_iti_transfer FOREIGN KEY (transfer_id) REFERENCES inventory_transfers(id) ON DELETE CASCADE,
    CONSTRAINT fk_iti_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_iti_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 5. 完整库存 ER 关系

```
suppliers
 ├── supplier_price_history → inventory_items (报价历史)
 └── purchase_orders → purchase_order_items → inventory_items (采购)
                                                   ↓ (入库)
                                          inventory_batches (批次，FIFO)
                                                   ↓
                                          inventory_items (主数据，当前库存)
                                           ├── inventory_movements (流水)
                                           ├── recipes → skus (SOP，销售扣减)
                                           ├── waste_records (日常报损)
                                           └── stocktake_items → stocktake_tasks (盘点)

inventory_transfers → inventory_transfer_items → inventory_items (门店调拨)

order_suggestions → inventory_items + suppliers (订货建议)
  → purchase_orders (建议转采购单)
```

---

## 6. 库存扣减流程（完整）

```
结账成功
  ↓
Settlement Hook
  ↓
遍历 submitted_order_items
  ↓
对每个 SKU:
  skus.requires_stock_deduct == true ?
    ↓ yes
  recipes WHERE sku_id = :skuId
    ↓
  对每个 recipe:
    consumption = recipe.consumption_qty × order_item.quantity
    换算到 stock_unit: consumption × stock_to_usage_ratio
      ↓
    FIFO 扣减:
      SELECT * FROM inventory_batches
      WHERE inventory_item_id = :id AND batch_status = 'ACTIVE'
      ORDER BY expiry_date ASC
        ↓
      逐批扣减直到总量满足
      如果某批扣完 → batch_status = 'DEPLETED'
        ↓
    更新 inventory_items.current_stock
    写入 inventory_movements (SALE_DEDUCT)
      ↓
    current_stock < safety_stock ?
      → 触发库存预警
```

---

## 7. 订货建议流程（完整）

```
每日营业结束后（或手动触发）
  ↓
对每个 inventory_item:
  avg_daily_usage = 过去 7 天 movements(SALE_DEDUCT) 的日均值
  days_remaining = current_stock / avg_daily_usage
  lead_time = supplier.lead_time_days
    ↓
  days_remaining <= lead_time + safety_buffer ?
    → 生成 order_suggestion
    suggested_qty = (lead_time + safety_buffer) × avg_daily_usage - current_stock
    estimated_cost = suggested_qty × last_purchase_price
    supplier_id = default_supplier_id
      ↓
  店长审核 → 批量转为 purchase_order
```

---

## 8. 新增表清单

| 表 | 用途 |
|----|------|
| inventory_batches | 原料批次（FIFO、保质期追踪） |
| purchase_orders | 采购订单 |
| purchase_order_items | 采购订单明细 |
| supplier_price_history | 供应商报价历史（比价） |
| stocktake_tasks | 盘点任务 |
| stocktake_items | 盘点明细（账实差异） |
| waste_records | 日常报损记录 |
| inventory_transfers | 门店间调拨 |
| inventory_transfer_items | 调拨明细 |

**9 张新表 + 4 张改动（inventory_items, suppliers, inventory_movements, order_suggestions）**

**累计：85 (doc/69) + 9 = 94 张表**
