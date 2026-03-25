# SKU Configuration Information Architecture

## 1. Purpose

这份文档定义 Merchant Admin 中 SKU / Catalog / Attribute / Combo 配置的后台结构。

目标是让商户后台真正能承接：

- 分类
- Product
- SKU
- Attribute 模板
- Modifier
- Combo
- 门店可售状态

## 2. Top-Level Navigation

建议在 Merchant Admin 中使用统一模块：

- `Catalog`

下分 7 个一级页面：

1. Categories
2. Products
3. SKUs
4. Attributes
5. Modifiers
6. Combos
7. Store Availability

## 3. Categories

### Purpose

维护菜单分类结构和展示顺序。

### Key Functions

- 新建分类
- 编辑名称
- 排序
- 启用 / 停用

## 4. Products

### Purpose

维护顾客看到的菜品主体。

### List View

字段建议：

- Product Name
- Category
- Status
- Default SKU Count
- Image
- Sort Order

### Detail View

区块建议：

- Basic Info
- Merchandising
- Default Attributes
- Related SKUs

## 5. SKUs

### Purpose

维护真正可售卖单元。

### List View

字段建议：

- SKU Name
- Parent Product
- Base Price
- Global Status
- Store Coverage
- Default Flag

### Detail View

区块建议：

- Basic Info
- Pricing
- Attributes
- Modifiers
- Kitchen Settings
- Availability Summary

## 6. Attributes

### Purpose

维护商户自定义的属性模板，不把平台字段写死。

### Attribute Group List

字段建议：

- Group Name
- Type
- Required
- Selection Mode
- Scope
- Active Status

### Attribute Group Detail

区块建议：

- Basic Definition
- Values
- Selection Rules
- Price Delta
- Kitchen Labels
- Applicable Products / SKUs

## 7. Modifiers

### Purpose

维护附加、替换、升级、去料类配置。

### List View

- Modifier Group
- Type
- Scope
- Status

### Detail View

- Values
- Pricing
- Kitchen Rendering
- Allowed Combinations

## 8. Combos

### Purpose

维护套餐和槽位模型。

### List View

- Combo Name
- Base Price
- Slot Count
- Status

### Detail View

区块建议：

- Combo Basic Info
- Slot Definitions
- Allowed SKUs per Slot
- Upgrade Rules
- Default Selections

## 9. Store Availability

### Purpose

维护门店级可售状态。

### Views

- By Store
- By SKU

### Functions

- 标记售罄
- 恢复售卖
- 禁用门店售卖
- 批量更新

## 10. Recommended Admin Flow

推荐商户后台配置流程：

1. 建分类
2. 建 Product
3. 建 SKU
4. 绑定 attribute / modifier
5. 配套餐槽位
6. 配门店可售状态
7. 发布到菜单

## 11. Bulk Operations

一期建议直接支持：

- 批量上下架
- 批量门店启停
- 批量售罄 / 恢复
- 批量调价
- 批量挂 attribute 模板

## 12. Page Priority

### Phase 1 Must Have

- Categories
- Products
- SKUs
- Store Availability

### Phase 2 Should Have

- Attributes
- Modifiers
- Combos

## 13. Final IA Summary

Merchant Admin 不应只提供“菜名 + 价格”配置。

它必须能配置：

- 商品结构
- 可售 SKU
- 属性模板
- Modifier
- 套餐槽位
- 门店可售状态

否则 POS / QR / Kitchen 会长期依赖前端临时逻辑。
