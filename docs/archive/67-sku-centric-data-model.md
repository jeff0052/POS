# SKU-Centric Data Model — 以 SKU 为核心的餐饮数据架构

**Version:** V20260328003
**Date:** 2026-03-28
**Status:** DRAFT

---

## 1. 为什么 SKU 是核心

餐饮的一切业务动作都围绕 SKU：

```
供应商送货 → 原料入库 → 按 SOP 消耗原料 → 制作 SKU
                                              ↓
顾客点单 → 选 SKU → 选修饰符 → 下单 → 厨房按 SKU 路由到工作站 → 出品
                                              ↓
结账 → 按 SKU 计价（不同渠道/时段/会员可能不同价） → 报表按 SKU 统计
                                              ↓
库存 → 按 SKU 的 SOP 扣减原料 → 低库存预警 → 订货建议
```

**所有链路的交汇点就是 SKU。** 如果 SKU 模型不完整，每个链路都会打补丁。

---

## 2. SKU 三层模型

客户 PRD 明确要求：一道菜的三个层面必须同时建模。

```
┌─────────────────────────────────────────────────┐
│                    SKU                           │
│                                                  │
│  Layer 1: 顾客侧（展示 + 销售）                    │
│  ├── 名称、图片、描述、过敏原                       │
│  ├── 售价（base + 多场景覆盖）                     │
│  ├── 修饰符（辣度、汤底、加料、规格）               │
│  ├── 推荐标签、是否主推                            │
│  ├── 可售渠道（堂食/外卖/自助）                     │
│  └── 可售时段（早茶/午市/晚市）                     │
│                                                  │
│  Layer 2: 厨房侧（制作 + 出品）                    │
│  ├── 归属工作站                                   │
│  ├── 打印路由（打印机 IP / KDS 编号）              │
│  ├── 出品备注模板                                  │
│  ├── 预估制作时间                                  │
│  └── 优先级标记                                    │
│                                                  │
│  Layer 3: 库存侧（成本 + 消耗）                    │
│  ├── 成本价                                       │
│  ├── SOP 配方（消耗哪些原料、多少量）               │
│  ├── 单位换算                                     │
│  └── 是否需要库存扣减                              │
└─────────────────────────────────────────────────┘
```

---

## 3. 当前 SKU 表 vs 目标

### 3.1 当前 skus 表字段

```
id, product_id, sku_code, sku_name, base_price_cents, sku_status,
image_id, station_id, print_route, cost_price_cents, recipe_id,
created_at, updated_at, actor_type, actor_id, decision_source, change_reason
```

### 3.2 缺什么

| 层 | 缺失字段 | 说明 |
|----|---------|------|
| 顾客 | description | 菜品描述（"香辣鲜嫩，配秘制酱料"） |
| 顾客 | allergen_tags | 过敏原标签（JSON: ["nuts","shellfish","gluten"]） |
| 顾客 | spice_level | 辣度等级（0=不辣, 1=微辣, 2=中辣, 3=重辣） |
| 顾客 | is_featured | 是否主推 |
| 顾客 | is_new | 是否新品 |
| 顾客 | tags | 自定义标签（JSON: ["本月爆款","高毛利","招牌"]） |
| 顾客 | min_order_qty | 最小起订量（默认 1） |
| 顾客 | max_order_qty | 最大单次点单量（0=不限） |
| 顾客 | calories | 热量（kcal，可选） |
| 厨房 | prep_time_minutes | 预估制作时间 |
| 厨房 | kitchen_note_template | 出品备注模板 |
| 厨房 | kitchen_priority | 厨房优先级（VIP 菜/慢菜标记） |
| 库存 | requires_stock_deduct | 是否需要库存扣减（饮品可能不走库存） |
| 通用 | sort_order | 排序 |
| 通用 | merchant_id | 商户归属（支持商户级 SKU 共享到多店） |

---

## 4. 设计方案：SKU 主表 + 扩展表

### 原则：SKU 主表只放高频查询字段，低频/大字段用扩展表

不把所有字段塞进一张表。原因：
- 菜单查询（顾客侧）需要极快，只查 Layer 1 字段
- 厨房路由（厨房侧）只查 Layer 2 字段
- 库存扣减（库存侧）只查 Layer 3 字段
- 一张大表 30+ 列会让查询和维护都很重

### 4.1 skus 主表 — ALTER（顾客侧核心）

```sql
-- 顾客侧展示字段
ALTER TABLE skus ADD COLUMN description TEXT NULL AFTER sku_name;
ALTER TABLE skus ADD COLUMN allergen_tags JSON NULL AFTER description;
ALTER TABLE skus ADD COLUMN spice_level INT DEFAULT 0 AFTER allergen_tags;
ALTER TABLE skus ADD COLUMN is_featured BOOLEAN DEFAULT FALSE AFTER spice_level;
ALTER TABLE skus ADD COLUMN is_new BOOLEAN DEFAULT FALSE AFTER is_featured;
ALTER TABLE skus ADD COLUMN tags JSON NULL AFTER is_new;
ALTER TABLE skus ADD COLUMN min_order_qty INT DEFAULT 1 AFTER tags;
ALTER TABLE skus ADD COLUMN max_order_qty INT DEFAULT 0 AFTER min_order_qty;
ALTER TABLE skus ADD COLUMN calories INT NULL AFTER max_order_qty;
ALTER TABLE skus ADD COLUMN sort_order INT DEFAULT 0 AFTER calories;
ALTER TABLE skus ADD COLUMN merchant_id BIGINT NULL AFTER product_id;

-- 厨房侧字段（已有 station_id, print_route）
ALTER TABLE skus ADD COLUMN prep_time_minutes INT NULL AFTER print_route;
ALTER TABLE skus ADD COLUMN kitchen_note_template VARCHAR(512) NULL AFTER prep_time_minutes;
ALTER TABLE skus ADD COLUMN kitchen_priority INT DEFAULT 0 AFTER kitchen_note_template;

-- 库存侧字段（已有 cost_price_cents, recipe_id）
ALTER TABLE skus ADD COLUMN requires_stock_deduct BOOLEAN DEFAULT TRUE AFTER recipe_id;
```

### 4.2 sku_modifiers（修饰符组）— 新建

当前修饰符存在 `products.modifier_config_json`（JSON 字段）。问题：
- 无法跨 product 共享修饰符组（"辣度"每个菜都要配一遍）
- 无法单独统计修饰符的选择频率
- 无法给修饰符加价

改为独立表：

```sql
CREATE TABLE modifier_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    group_code VARCHAR(64) NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    selection_type VARCHAR(16) NOT NULL DEFAULT 'SINGLE',
    is_required BOOLEAN DEFAULT FALSE,
    min_select INT DEFAULT 0,
    max_select INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mg_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_mg UNIQUE (merchant_id, group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**selection_type：** `SINGLE`（单选：辣度）| `MULTI`（多选：加料）

```sql
CREATE TABLE modifier_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    option_code VARCHAR(64) NOT NULL,
    option_name VARCHAR(128) NOT NULL,
    price_adjustment_cents BIGINT DEFAULT 0,
    is_default BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mo_group FOREIGN KEY (group_id) REFERENCES modifier_groups(id) ON DELETE CASCADE,
    CONSTRAINT uk_mo UNIQUE (group_id, option_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**price_adjustment_cents：** 加价/减价（加料加 300 分 = 3 元）

```sql
CREATE TABLE sku_modifier_group_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    modifier_group_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    CONSTRAINT fk_smgb_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT fk_smgb_group FOREIGN KEY (modifier_group_id) REFERENCES modifier_groups(id),
    CONSTRAINT uk_smgb UNIQUE (sku_id, modifier_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**设计优势：**
- 修饰符组是 **merchant 级别**，可跨 product/SKU 共享
- "辣度"组配置一次，绑到 100 个 SKU
- 修饰符可加价（加料 +3 元）
- 支持 SINGLE/MULTI 选择模式
- 修饰符选择结果仍然作为 JSON 快照存入订单项（`option_snapshot_json`）

### 4.3 sku_channel_configs（渠道级 SKU 配置）— 新建

一个 SKU 在不同销售渠道可能有不同配置（不只是价格）：

```sql
CREATE TABLE sku_channel_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    channel_ref VARCHAR(128) NULL,
    channel_sku_name VARCHAR(255) NULL,
    channel_description TEXT NULL,
    channel_image_id VARCHAR(64) NULL,
    is_available BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_scc_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_scc UNIQUE (sku_id, channel_type, channel_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

| channel_type | channel_ref | 含义 |
|-------------|-------------|------|
| `DINE_IN` | null | 堂食配置 |
| `DELIVERY` | `GRAB` | Grab 渠道配置 |
| `DELIVERY` | `FOODPANDA` | Foodpanda 渠道配置 |
| `DELIVERY` | `OWN` | 自有外卖配置 |
| `BUFFET` | `PKG_22` | 22 元自助档位配置 |

**为什么需要这张表：**
- 同一个"宫保鸡丁"在 Grab 上可能叫 "Kung Pao Chicken"（不同名字）
- 在 Grab 上用不同图片（平台要求特定尺寸）
- 在自助档位里可能不展示详细描述
- 在外卖平台上可能不可售（`is_available=false`）

**价格仍然在 `sku_price_overrides`，渠道配置在这里。职责分离。**

### 4.4 sku_faq（SKU 常见问答）— 新建

客户要求：顾客可以看到"是否含牛肉""辣不辣""小孩能吃吗"等问答。

```sql
CREATE TABLE sku_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    question VARCHAR(255) NOT NULL,
    answer TEXT NOT NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sf_sku FOREIGN KEY (sku_id) REFERENCES skus(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 5. SKU 完整关系图

```
merchants
 └── modifier_groups → modifier_options
                           ↓ (bind)
stores
 └── products
      └── skus (主表：三层核心字段)
           ├── sku_modifier_group_bindings → modifier_groups
           ├── sku_price_overrides (多场景价格)
           ├── sku_channel_configs (渠道级配置：名称/图/可售)
           ├── sku_faq (常见问答)
           ├── store_sku_availability (门店可售开关)
           ├── recipes → inventory_items (SOP 配方)
           ├── buffet_package_items (自助档位关联)
           ├── recommendation_slot_items (推荐位)
           └── → kitchen_stations (厨房路由)
                    ↓
               kitchen_tickets → kitchen_ticket_items
                    ↓
               submitted_order_items (SKU 快照)
                    ↓
               settlement → report → GTO
```

---

## 6. SKU 在各业务场景的使用

### 6.1 顾客点单

```
输入：dining_mode + time_slot + member_tier + channel
查询链：
  products (menu_modes 过滤)
  → skus (主表字段)
  → sku_channel_configs (渠道名称/图片/可售性)
  → sku_price_overrides (场景价格)
  → sku_modifier_group_bindings → modifier_groups → modifier_options
  → sku_faq (可选展示)
  → store_sku_availability (门店开关)
输出：可点菜单 + 最终价格 + 修饰符选项
```

### 6.2 厨房出单

```
输入：submitted_order_items
查询链：
  skus.station_id → kitchen_stations (路由到哪个工作站)
  skus.print_route (打印到哪台打印机)
  skus.kitchen_note_template (出品备注)
  skus.prep_time_minutes (预估时间)
输出：按工作站拆分的 kitchen_tickets
```

### 6.3 库存扣减

```
输入：settled order items
查询链：
  skus.recipe_id → recipes (该 SKU 消耗哪些原料)
  recipes.inventory_item_id → inventory_items (原料库存)
  recipes.consumption_qty × order_item.quantity = 扣减量
  skus.requires_stock_deduct (是否需要扣减)
输出：inventory_movements (库存流水) + 库存预警
```

### 6.4 报表分析

```
输入：submitted_order_items + settlement_records
查询链：
  sku_id → skus.cost_price_cents (成本价)
  line_total_cents - (cost_price_cents × quantity) = 毛利
  按 SKU 聚合：销量排行、毛利排行、退菜率
输出：SKU 级别经营分析
```

### 6.5 外卖平台同步

```
输入：delivery_platform_configs (平台配置)
查询链：
  skus → sku_channel_configs (channel_type=DELIVERY, channel_ref=GRAB)
  → channel_sku_name, channel_image_id (平台展示信息)
  → sku_price_overrides (DELIVERY + GRAB) → 平台价格
  → is_available (是否在该平台上架)
输出：推送到平台的商品数据
```

---

## 7. 新增表总结（相比 doc/66 新增 4 张）

| 表 | 用途 |
|----|------|
| modifier_groups | 修饰符组（merchant 级共享） |
| modifier_options | 修饰符选项（含加价） |
| sku_modifier_group_bindings | SKU-修饰符组绑定 |
| sku_channel_configs | 渠道级 SKU 配置 |
| sku_faq | SKU 常见问答 |

**总计：48 原有 + 21 (doc/66) + 5 (本文档) = 74 张表**

---

## 8. 与 doc/66 的关系

doc/66 定义了整体数据模型（自助餐、KDS、库存、推荐、外卖、候位）。

本文档（doc/67）是对 doc/66 中 **SKU 相关部分的深化**：

| doc/66 的设计 | doc/67 的改进 |
|-------------|-------------|
| products.station_id | → 移到 skus.station_id（SKU 级路由） |
| skus.delivery_price_cents | → 删除，改用 sku_price_overrides |
| products.modifier_config_json | → 独立为 modifier_groups + modifier_options + bindings |
| 无渠道配置 | → 新增 sku_channel_configs |
| 无 FAQ | → 新增 sku_faq |
| SKU 缺顾客侧字段 | → ALTER 加 description, allergen_tags, spice_level, is_featured, tags 等 |
| SKU 缺厨房侧字段 | → ALTER 加 prep_time_minutes, kitchen_note_template, kitchen_priority |
| SKU 缺库存控制 | → ALTER 加 requires_stock_deduct |

**doc/66 和 doc/67 合在一起就是完整的数据模型。** 实施时用一个 Flyway migration 把两份文档的改动一次性执行。
