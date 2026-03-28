# Aurora Data Model Design — 数据结构调整方案

**Version:** V20260328002
**Date:** 2026-03-28
**Status:** DRAFT — 待 Review

---

## 1. 设计原则

1. **扩展现有表，不推翻** — 48 张表是资产，只加不删
2. **新模块独立建表** — 自助餐、库存、KDS、推荐各自独立，不污染现有订单核心
3. **用 `dining_type` 区分业务模式** — 不为自助餐/外卖单独建订单表
4. **三层 SKU** — 顾客侧（展示）+ 厨房侧（路由）+ 库存侧（扣减）

---

## 2. 现有表需要的改动

### 2.1 store_tables — 加字段

```sql
ALTER TABLE store_tables ADD COLUMN area VARCHAR(64) NULL AFTER table_name;
ALTER TABLE store_tables ADD COLUMN qr_code_url VARCHAR(512) NULL AFTER sort_order;
```

**原因：** 客户需求要求桌台分区域、绑定二维码。

### 2.2 table_sessions — 加自助餐字段

```sql
ALTER TABLE table_sessions ADD COLUMN dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE' AFTER session_status;
ALTER TABLE table_sessions ADD COLUMN guest_count INT DEFAULT 1 AFTER dining_mode;
ALTER TABLE table_sessions ADD COLUMN buffet_package_id BIGINT NULL AFTER guest_count;
ALTER TABLE table_sessions ADD COLUMN buffet_started_at TIMESTAMP NULL AFTER buffet_package_id;
ALTER TABLE table_sessions ADD COLUMN buffet_ends_at TIMESTAMP NULL AFTER buffet_started_at;
ALTER TABLE table_sessions ADD COLUMN buffet_status VARCHAR(32) NULL AFTER buffet_ends_at;
```

**dining_mode 枚举：** `A_LA_CARTE`（单点）| `BUFFET`（自助餐）| `DELIVERY`（外卖）

**buffet_status 枚举：** `ACTIVE` | `WARNING` | `OVERTIME` | `ENDED`

### 2.3 products — 加厨房路由和库存关联

```sql
ALTER TABLE products ADD COLUMN station_id BIGINT NULL AFTER image_id;
ALTER TABLE products ADD COLUMN print_route VARCHAR(64) NULL AFTER station_id;
ALTER TABLE products ADD COLUMN menu_modes JSON NULL AFTER print_route;
ALTER TABLE products ADD COLUMN is_featured BOOLEAN DEFAULT FALSE AFTER menu_modes;
ALTER TABLE products ADD COLUMN is_recommended BOOLEAN DEFAULT FALSE AFTER is_featured;
ALTER TABLE products ADD COLUMN allergen_info VARCHAR(512) NULL AFTER is_recommended;
ALTER TABLE products ADD COLUMN sort_order INT DEFAULT 0 AFTER allergen_info;
```

**menu_modes** 示例：`["A_LA_CARTE", "BUFFET", "DELIVERY"]` — 控制该商品在哪些模式下可见。

### 2.4 skus — 加外卖价和库存关联

```sql
ALTER TABLE skus ADD COLUMN delivery_price_cents BIGINT NULL AFTER base_price_cents;
ALTER TABLE skus ADD COLUMN cost_price_cents BIGINT NULL AFTER delivery_price_cents;
ALTER TABLE skus ADD COLUMN recipe_id BIGINT NULL AFTER cost_price_cents;
```

**delivery_price_cents：** 外卖价格（可与堂食不同）。
**cost_price_cents：** 成本价（用于毛利计算）。
**recipe_id：** 关联 SOP 配方（库存扣减用）。

### 2.5 submitted_orders — 加配送相关

```sql
ALTER TABLE submitted_orders ADD COLUMN dining_mode VARCHAR(32) DEFAULT 'A_LA_CARTE' AFTER source_order_type;
ALTER TABLE submitted_orders ADD COLUMN external_platform VARCHAR(64) NULL AFTER dining_mode;
ALTER TABLE submitted_orders ADD COLUMN external_order_no VARCHAR(128) NULL AFTER external_platform;
ALTER TABLE submitted_orders ADD COLUMN delivery_status VARCHAR(32) NULL AFTER external_order_no;
ALTER TABLE submitted_orders ADD COLUMN delivery_address TEXT NULL AFTER delivery_status;
ALTER TABLE submitted_orders ADD COLUMN delivery_contact_name VARCHAR(128) NULL AFTER delivery_address;
ALTER TABLE submitted_orders ADD COLUMN delivery_contact_phone VARCHAR(64) NULL AFTER delivery_contact_name;
```

---

## 3. 新增表 — 自助餐模块

### 3.1 buffet_packages（自助档位）

```sql
CREATE TABLE buffet_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    package_code VARCHAR(64) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    price_cents BIGINT NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 90,
    warning_before_minutes INT NOT NULL DEFAULT 10,
    overtime_fee_per_minute_cents BIGINT DEFAULT 0,
    package_status VARCHAR(32) DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_buffet_pkg_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_buffet_pkg UNIQUE (store_id, package_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.2 buffet_package_items（档位包含的商品）

```sql
CREATE TABLE buffet_package_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    is_included BOOLEAN DEFAULT TRUE,
    surcharge_cents BIGINT DEFAULT 0,
    CONSTRAINT fk_bpi_package FOREIGN KEY (package_id) REFERENCES buffet_packages(id) ON DELETE CASCADE,
    CONSTRAINT fk_bpi_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_bpi UNIQUE (package_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**is_included=true + surcharge=0：** 套餐内免费。
**is_included=true + surcharge>0：** 套餐内但有差价。
**is_included=false：** 套餐外商品（额外收费，按 SKU 原价）。

---

## 4. 新增表 — 厨房 KDS 模块

### 4.1 kitchen_stations（工作站）

```sql
CREATE TABLE kitchen_stations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    station_code VARCHAR(64) NOT NULL,
    station_name VARCHAR(128) NOT NULL,
    station_type VARCHAR(32) DEFAULT 'KITCHEN',
    printer_ip VARCHAR(64) NULL,
    kds_display_id VARCHAR(64) NULL,
    station_status VARCHAR(32) DEFAULT 'ACTIVE',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_station_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_station UNIQUE (store_id, station_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.2 kitchen_tickets（厨房票）

```sql
CREATE TABLE kitchen_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    table_code VARCHAR(64) NOT NULL,
    station_id BIGINT NOT NULL,
    submitted_order_id BIGINT NOT NULL,
    round_number INT DEFAULT 1,
    ticket_status VARCHAR(32) DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    ready_at TIMESTAMP NULL,
    served_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kt_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_kt_station FOREIGN KEY (station_id) REFERENCES kitchen_stations(id),
    CONSTRAINT fk_kt_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    CONSTRAINT uk_kt UNIQUE (ticket_no),
    INDEX idx_kt_station_status (station_id, ticket_status),
    INDEX idx_kt_store_status (store_id, ticket_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**ticket_status 枚举：** `SUBMITTED` → `PREPARING` → `READY` → `SERVED` | `CANCELLED`

### 4.3 kitchen_ticket_items（厨房票明细）

```sql
CREATE TABLE kitchen_ticket_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_name_snapshot VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    item_remark VARCHAR(255) NULL,
    option_snapshot_json JSON NULL,
    CONSTRAINT fk_kti_ticket FOREIGN KEY (ticket_id) REFERENCES kitchen_tickets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 5. 新增表 — 库存模块

### 5.1 inventory_items（原料主数据）

```sql
CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NULL,
    unit VARCHAR(32) NOT NULL,
    stock_unit VARCHAR(32) NULL,
    unit_conversion_factor DECIMAL(10,4) DEFAULT 1.0,
    current_stock DECIMAL(14,4) DEFAULT 0,
    safety_stock DECIMAL(14,4) DEFAULT 0,
    expiry_warning_days INT DEFAULT 3,
    item_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_inv UNIQUE (store_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.2 recipes（SOP 配方）

```sql
CREATE TABLE recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    consumption_qty DECIMAL(10,4) NOT NULL,
    consumption_unit VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipe_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT fk_recipe_inv FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT uk_recipe UNIQUE (sku_id, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.3 purchase_invoices（送货单/进货单）

```sql
CREATE TABLE purchase_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    invoice_no VARCHAR(128) NOT NULL,
    supplier_id BIGINT NULL,
    supplier_name VARCHAR(255) NULL,
    invoice_date DATE NOT NULL,
    total_amount_cents BIGINT DEFAULT 0,
    invoice_status VARCHAR(32) DEFAULT 'PENDING',
    scan_image_url VARCHAR(512) NULL,
    ocr_status VARCHAR(32) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pi_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_pi UNIQUE (store_id, invoice_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.4 purchase_invoice_items（进货单明细）

```sql
CREATE TABLE purchase_invoice_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    quantity DECIMAL(10,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    line_total_cents BIGINT NOT NULL,
    CONSTRAINT fk_pii_invoice FOREIGN KEY (invoice_id) REFERENCES purchase_invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_pii_inv FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.5 inventory_movements（库存变动流水）

```sql
CREATE TABLE inventory_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    movement_type VARCHAR(32) NOT NULL,
    quantity_change DECIMAL(14,4) NOT NULL,
    balance_after DECIMAL(14,4) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_ref VARCHAR(128) NULL,
    notes VARCHAR(512) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_im_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_im_inv FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    INDEX idx_im_item (inventory_item_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**movement_type 枚举：** `PURCHASE`（进货）| `SALE_DEDUCT`（销售扣减）| `ADJUSTMENT`（盘点调整）| `WASTE`（报损）| `TRANSFER`（调拨）

### 5.6 suppliers（供应商）

```sql
CREATE TABLE suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    supplier_code VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(128) NULL,
    contact_phone VARCHAR(64) NULL,
    email VARCHAR(255) NULL,
    address TEXT NULL,
    supplier_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_supplier_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_supplier UNIQUE (merchant_id, supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.7 order_suggestions（订货建议）

```sql
CREATE TABLE order_suggestions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    suggestion_date DATE NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    current_stock DECIMAL(14,4) NOT NULL,
    avg_daily_usage DECIMAL(14,4) NOT NULL,
    suggested_qty DECIMAL(14,4) NOT NULL,
    suggestion_status VARCHAR(32) DEFAULT 'PENDING',
    approved_qty DECIMAL(14,4) NULL,
    approved_by VARCHAR(128) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_os_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_os_inv FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT uk_os UNIQUE (store_id, suggestion_date, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 6. 新增表 — 推荐系统

### 6.1 recommendation_slots（推荐位）

```sql
CREATE TABLE recommendation_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    slot_code VARCHAR(64) NOT NULL,
    slot_name VARCHAR(128) NOT NULL,
    slot_type VARCHAR(32) NOT NULL,
    position VARCHAR(64) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_rs UNIQUE (store_id, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**slot_type 枚举：** `FEATURED`（首屏主推）| `UPSELL`（加购推荐）| `TRENDING`（本月爆款）| `HIGH_MARGIN`（高毛利推荐）| `PERSONALIZED`（会员偏好）

**position 枚举：** `MENU_TOP`（菜单首屏）| `ITEM_DETAIL`（商品详情页）| `CART`（购物车页）| `CHECKOUT`（结账页）

### 6.2 recommendation_slot_items（推荐位内容）

```sql
CREATE TABLE recommendation_slot_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slot_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    display_label VARCHAR(255) NULL,
    sort_order INT DEFAULT 0,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    CONSTRAINT fk_rsi_slot FOREIGN KEY (slot_id) REFERENCES recommendation_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_rsi_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_rsi UNIQUE (slot_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 7. 新增表 — 外卖平台对接

### 7.1 delivery_platform_configs（外卖平台配置）

```sql
CREATE TABLE delivery_platform_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    platform_code VARCHAR(64) NOT NULL,
    platform_name VARCHAR(128) NOT NULL,
    api_key VARCHAR(512) NULL,
    api_secret VARCHAR(512) NULL,
    store_mapping_id VARCHAR(128) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    config_json JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dpc_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_dpc UNIQUE (store_id, platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**platform_code 枚举：** `GRAB` | `FOODPANDA` | `DELIVEROO` | `OWN_DELIVERY`

### 7.2 delivery_orders（外卖订单扩展）

```sql
CREATE TABLE delivery_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submitted_order_id BIGINT NOT NULL,
    platform_code VARCHAR(64) NOT NULL,
    platform_order_no VARCHAR(128) NOT NULL,
    platform_status VARCHAR(64) NULL,
    rider_name VARCHAR(128) NULL,
    rider_phone VARCHAR(64) NULL,
    estimated_pickup_at TIMESTAMP NULL,
    actual_pickup_at TIMESTAMP NULL,
    estimated_delivery_at TIMESTAMP NULL,
    actual_delivery_at TIMESTAMP NULL,
    delivery_fee_cents BIGINT DEFAULT 0,
    platform_commission_cents BIGINT DEFAULT 0,
    merchant_receivable_cents BIGINT DEFAULT 0,
    platform_status_snapshot JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_do_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    CONSTRAINT uk_do_platform UNIQUE (platform_code, platform_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 8. 新增表 — 候位系统

### 8.1 queue_tickets（候位票）

```sql
CREATE TABLE queue_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    ticket_no VARCHAR(64) NOT NULL,
    guest_phone VARCHAR(64) NULL,
    member_id BIGINT NULL,
    party_size INT NOT NULL DEFAULT 2,
    queue_position INT NOT NULL,
    ticket_status VARCHAR(32) DEFAULT 'WAITING',
    estimated_wait_minutes INT NULL,
    called_at TIMESTAMP NULL,
    seated_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    table_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_qt_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_qt UNIQUE (store_id, ticket_no),
    INDEX idx_qt_status (store_id, ticket_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**ticket_status 枚举：** `WAITING` → `CALLED` → `SEATED` → `NO_SHOW` | `CANCELLED`

---

## 9. 数据模型总结

### 新增表清单（18 张）

| 模块 | 表 | 用途 |
|------|-----|------|
| 自助餐 | buffet_packages | 档位配置 |
| 自助餐 | buffet_package_items | 档位-商品关联 |
| KDS | kitchen_stations | 工作站 |
| KDS | kitchen_tickets | 厨房票 |
| KDS | kitchen_ticket_items | 厨房票明细 |
| 库存 | inventory_items | 原料主数据 |
| 库存 | recipes | SOP 配方 |
| 库存 | purchase_invoices | 送货单 |
| 库存 | purchase_invoice_items | 送货单明细 |
| 库存 | inventory_movements | 库存流水 |
| 库存 | suppliers | 供应商 |
| 库存 | order_suggestions | 订货建议 |
| 推荐 | recommendation_slots | 推荐位 |
| 推荐 | recommendation_slot_items | 推荐位内容 |
| 外卖 | delivery_platform_configs | 平台配置 |
| 外卖 | delivery_orders | 外卖订单扩展 |
| 候位 | queue_tickets | 候位票 |

### 改动表清单（5 张）

| 表 | 改动 |
|----|------|
| store_tables | +area, +qr_code_url |
| table_sessions | +dining_mode, +guest_count, +buffet_* 字段 |
| products | +station_id, +print_route, +menu_modes, +is_featured, +is_recommended, +allergen_info, +sort_order |
| skus | +delivery_price_cents, +cost_price_cents, +recipe_id |
| submitted_orders | +dining_mode, +external_platform, +external_order_no, +delivery_* 字段 |

### 最终总表数

**原有 48 张 + 新增 18 张 = 66 张表**

---

## 10. ER 关系要点

```
Store
 ├── store_tables (area, qr_code)
 ├── kitchen_stations
 ├── buffet_packages → buffet_package_items → skus
 ├── inventory_items ← recipes → skus
 ├── recommendation_slots → recommendation_slot_items → skus
 ├── delivery_platform_configs
 └── queue_tickets

table_sessions (dining_mode, buffet_*)
 └── submitted_orders (dining_mode, external_*)
      ├── kitchen_tickets → kitchen_ticket_items
      └── delivery_orders

purchase_invoices → purchase_invoice_items → inventory_items
inventory_movements → inventory_items

order_suggestions → inventory_items
suppliers → purchase_invoices
```
