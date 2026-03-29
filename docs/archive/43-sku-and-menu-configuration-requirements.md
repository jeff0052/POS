# SKU and Menu Configuration Requirements

## 1. Purpose

这份文档用于正式定义餐饮 POS 新系统中的 `Catalog / Product / SKU / Attribute / Modifier / Combo / Availability / Fulfillment` 模型。

目标不是只做一个“商品列表”，而是建立一套可扩展的餐饮配置能力，让不同品牌、不同菜系都可以按自己的经营方式定义点单属性，而不是被平台固定字段限制。

系统应支持：

- 商户自定义点单属性 `attributes`
- 平台提供常见餐饮属性模板
- 套餐槽位选择
- 门店级售罄
- 菜品准备中 / 已出菜

这份文档应作为后续以下工作的统一基础：

- Merchant Admin 商品配置
- POS 点菜页
- QR 点菜页
- Order item snapshot
- Kitchen / Fulfillment
- Promotion / Member pricing
- Report / AI analysis

## 2. Why Restaurant SKU Is Not a Standard SKU

标准零售 SKU 通常只需要：

- 商品
- 条码
- 单价
- 库存

餐饮 SKU 需要承接更多维度：

- 同一道菜有多个可售规格
- 顾客会对同一道菜做结构化选择
- 顾客会提出去料、少冰、少辣等个性化要求
- 同一道菜售出后还会进入厨房履约状态
- 同一商品在不同门店的可售状态可能不同
- 同一道菜需要兼顾展示、点单、打印、厨房出品、促销和报表

因此，餐饮系统不能只做“商品名 + 价格”。

## 3. Core Principles

### 3.1 SKU Is the Sellable Unit

订单的交易对象是 `SKU`，不是模糊的菜名。

例如：

- `香辣鸡腿堡（中份）`
- `招牌炒饭（大份）`
- `奶茶（冰 / 50% sugar / large）`

都应由 SKU 或 SKU + attribute snapshot 明确表达。

### 3.2 Product Is for Merchandising, SKU Is for Selling

`Product` 负责：

- 展示
- 归类
- 营销文案
- 图片

`SKU` 负责：

- 实际售卖
- 定价
- 点单
- 促销
- 折扣
- 历史订单快照

### 3.3 Sellability Status and Fulfillment Status Must Be Separated

必须区分两套状态：

#### 商品 / SKU 可售状态

表示“现在能不能卖”。

#### 订单项 / 厨房项履约状态

表示“卖出去之后做到哪一步了”。

这两套状态不能混用。

`OUT_OF_STOCK` 不等于 `PREPARING`。  
`INACTIVE` 不等于 `READY`。

### 3.4 Backend Is the Source of Truth

商品、SKU、选项、门店可售状态、价格规则必须以后端为准。  
前端只保留临时 UI 交互态，不保留业务真相。

### 3.5 Historical Orders Must Use Snapshots

订单项必须保存以下快照：

- `product_name_snapshot`
- `sku_name_snapshot`
- `unit_price_snapshot`
- `attribute_snapshot_json`
- `modifier_snapshot_json`
- `kitchen_label_snapshot`

以后商品改名、改价、改选项，不能污染历史订单。

### 3.6 Attributes Must Be Configurable, Not Hardcoded

平台不应把“大中小、辣度、糖度、加葱、不要香菜”直接设计成固定字段。

更合理的方式是：

- 系统提供通用的 `Attribute Group / Attribute Value` 机制
- 平台提供一组常见餐饮 attribute 模板
- 商户可自行启用、修改、复制、删除或新增 attribute

例如：

- 奶茶品牌需要糖度、冰量、加珍珠
- 川菜品牌需要辣度、麻度、香菜、葱花
- 西餐品牌需要熟度、配菜、酱汁
- 面馆需要汤底、面条软硬、加面

这些都应通过“可配置 attribute 模型”承接，而不是平台写死字段。

## 4. Core Catalog Model

### 4.1 Category

`Category` 用于菜单分组。

示例：

- Rice
- Noodles
- Beverages
- Desserts
- Set Meals

建议字段：

- `category_id`
- `merchant_id`
- `store_scope`
- `category_code`
- `category_name`
- `display_order`
- `status`

### 4.2 Product

`Product` 表示顾客看到的菜品主体。

示例：

- 招牌炒饭
- 牛肉河粉
- 柠檬茶
- 双人套餐

建议字段：

- `product_id`
- `merchant_id`
- `category_id`
- `product_code`
- `product_name`
- `short_description`
- `long_description`
- `image_url`
- `tag_json`
- `display_order`
- `status`

### 4.3 SKU

`SKU` 是真正的可售卖单元。

同一个 `Product` 可以有多个 `SKU`：

- 炒饭小份
- 炒饭中份
- 炒饭大份
- 柠檬茶 cold regular
- 柠檬茶 cold large
- 柠檬茶 hot regular

建议字段：

- `sku_id`
- `product_id`
- `sku_code`
- `sku_name`
- `base_price_cents`
- `currency_code`
- `status`
- `display_order`
- `is_default`
- `tax_category`
- `kitchen_station_code`

### 4.4 Attribute Group

`Attribute Group` 表示顾客在点单时可选择的维度组。

现有代码或数据库实现里可以继续使用 `option_group` 命名，但在产品与架构语义上，推荐统一理解成更通用的 `attribute group`。

示例：

- Size
- Spicy Level
- Sugar Level
- Temperature
- Add-ons
- Exclusions
- Noodle Hardness
- Soup Base
- Side Choice

建议字段：

- `attribute_group_id`
- `scope_type` (`PRODUCT` / `SKU`)
- `scope_id`
- `group_code`
- `group_name`
- `selection_type`
- `min_select`
- `max_select`
- `is_required`
- `display_order`
- `status`

### 4.5 Attribute Value

`Attribute Value` 是组下面的具体可选项。

示例：

- Size:
  - Small
  - Medium
  - Large
- Spicy Level:
  - No Spicy
  - Mild
  - Medium
  - Extra Hot
- Exclusions:
  - No Coriander
  - No Spring Onion
  - No Onion

建议字段：

- `attribute_value_id`
- `attribute_group_id`
- `value_code`
- `value_name`
- `price_delta_cents`
- `is_default`
- `kitchen_label`
- `status`

### 4.6 Modifier

`Modifier` 用于加料、升级、替换等附加收费或结构化调整。

典型场景：

- Add egg
- Add cheese
- Add pearl
- Upgrade fries to truffle fries
- Add rice

Modifier 可以与 attribute value 合并建模，但在餐饮里建议保留“概念上可区分”的设计：

- `Attribute` 更偏选择维度
- `Modifier` 更偏加价、附加、替换

### 4.7 Store SKU Availability

同一个 SKU 在不同门店的状态可能不同。

建议字段：

- `store_sku_availability_id`
- `store_id`
- `sku_id`
- `availability_status`
- `effective_from`
- `effective_to`
- `reason_code`

### 4.8 Combo / Bundle

套餐不能只当一个普通商品名。

套餐需要明确：

- 套餐主 SKU
- 套餐槽位
- 每个槽位允许选什么
- 升级差价

例如：

`Single Value Meal`

槽位：

- Main
- Side
- Drink

每个槽位都有自己的 allowed SKU set。

## 5. Restaurant Attribute Template Library

以下不是平台固定字段，而是平台建议内置的一组常见餐饮 attribute 模板，供商户快速启用或复制修改。

每个模板都应允许商户：

- 改名称
- 改值
- 改默认值
- 改价格差
- 改显示顺序
- 改厨房标签
- 改是否必选
- 改可选数量
- 改适用范围

### 5.1 Portion / Size

- Small
- Medium
- Large
- Extra Large

### 5.2 Spicy Level

- No Spicy
- Mild
- Normal
- Extra Hot

### 5.3 Temperature

- Hot
- Cold
- Less Ice
- No Ice

### 5.4 Sugar Level

- 0%
- 25%
- 50%
- 75%
- 100%

### 5.5 Doneness

- Rare
- Medium Rare
- Medium
- Well Done

### 5.6 Noodle / Rice Texture

- Soft
- Normal
- Firm

### 5.7 Soup / Base

- Clear Soup
- Spicy Soup
- Mala Base
- Curry Base

### 5.8 Protein Choice

- Chicken
- Beef
- Pork
- Seafood
- Tofu

### 5.9 Add-ons

- Add Egg
- Add Cheese
- Add Rice
- Add Pearl
- Add Whipped Cream

### 5.10 Exclusions

- No Coriander
- No Spring Onion
- No Onion
- No Garlic
- No Chili
- No Sauce

### 5.11 Replacement / Upgrade

- Replace fries with salad
- Upgrade drink to large
- Replace white rice with fried rice

### 5.12 Free Text Remarks

系统仍需支持 `remark`，但应明确：

- `remark` 只用于非结构化例外说明
- 常见需求尽量结构化建模，不要长期依赖自由文本

例如：

- “不要香菜” 应优先作为结构化 exclusion attribute
- “分开打包” 可以作为 remark

### 5.13 Merchant-Defined Attributes

除了平台模板，商户必须可以新增自己的 attribute group。

例如：

- Sauce Choice
- Combo Drink Upgrade
- Rice Portion
- Soup Add-On
- Breakfast Set Side Choice
- Festival Limited Add-On

系统不应对 attribute 名称做平台级硬编码假设。

## 6. Selection Rules

### 6.1 Required vs Optional

有些 attribute group 必须选：

- Size
- Main for combo

有些 attribute group 可不选：

- Add-ons
- Exclusions

### 6.2 Single Select vs Multi Select

#### Single Select

示例：

- Size 只能选一个
- Spicy Level 只能选一个

#### Multi Select

示例：

- Add-ons 可多选
- Exclusions 可多选

### 6.3 Min / Max Rules

示例：

- Combo side 至少选 1 个
- Add-ons 最多选 3 个

### 6.4 Default Values

如果用户不改，应有默认值：

- Normal spicy
- Cold drink
- 100% sugar

默认值必须可配置。

### 6.5 Price Delta

Attribute / modifier 可以有价格差。

示例：

- Large +100
- Add cheese +150
- Upgrade side +250

### 6.6 Free Quota Then Paid

示例：

- 前 1 份酱料免费，超过后收费
- 套餐默认带 1 个 side，升级另加价

### 6.7 Incompatible Combinations

系统应支持 attribute 互斥规则。

示例：

- Hot drink 不能选 no ice
- No sugar 不能再选 50% sugar
- 某套餐饮料槽位不允许热饮

## 7. Status Model

## 7.1 Product Status

建议：

- `DRAFT`
- `ACTIVE`
- `INACTIVE`

### 含义

- `DRAFT`
  - 配置中
  - 后台可见
  - 前台不可售
- `ACTIVE`
  - 正常展示和销售
- `INACTIVE`
  - 下架
  - 不再新卖

## 7.2 SKU Global Status

建议：

- `DRAFT`
- `ACTIVE`
- `INACTIVE`

SKU 全局状态表示该 SKU 在商户级别是否可售。

## 7.3 Store-Level Availability Status

建议：

- `AVAILABLE`
- `OUT_OF_STOCK`
- `DISABLED_FOR_STORE`

### 含义

- `AVAILABLE`
  - 当前门店正常可卖
- `OUT_OF_STOCK`
  - 临时售罄
  - 后续可恢复
- `DISABLED_FOR_STORE`
  - 该门店长期不卖这个 SKU

## 7.4 Fulfillment Status

这里不是 SKU 状态，而是 `Order Item / Kitchen Item` 状态。

建议：

- `SUBMITTED`
- `PREPARING`
- `READY`
- `SERVED`
- `VOIDED`

### 含义

- `SUBMITTED`
  - 已送厨，等待厨房处理
- `PREPARING`
  - 制作中
- `READY`
  - 已出菜，可取餐 / 可上桌
- `SERVED`
  - 已上桌 / 已交付
- `VOIDED`
  - 作废 / 取消出品

这套状态必须明确挂在订单项或厨房项上，不应挂在 SKU 本体上。

## 8. POS / QR Menu Behavior

### 8.1 POS

POS 应展示：

- `ACTIVE`
- 且当前门店 `AVAILABLE`
的 SKU

POS 必须支持：

- 结构化选择
- add-on
- exclusion
- 套餐槽位
- remark

### 8.2 QR

QR 必须遵守与 POS 相同的 SKU 规则，不应出现“POS 能点、QR 不能点”的独立配置体系。

QR 页面应支持：

- SKU 展示
- 结构化选择
- 必选项校验
- 价格预览
- 提交前确认

### 8.3 Availability Display

`OUT_OF_STOCK` 的 SKU 应：

- 默认不出现在菜单中
或
- 显示为不可点

这取决于产品策略，但必须统一。

## 9. Kitchen and Ticket Requirements

厨房并不关心完整的商品配置后台结构，它关心：

- 这道菜叫什么
- 要怎么做
- 不要什么
- 加了什么
- 数量是多少

因此系统必须在订单项上保存厨房友好的快照：

- `kitchen_item_name`
- `kitchen_modifier_summary`
- `remark_snapshot`

示例：

`招牌炒饭（大）`

厨房标签：

- Extra hot
- No spring onion
- Add egg

### 9.1 Structured First, Remark Second

厨房显示应优先来自结构化 option / modifier，而不是仅靠自由文本 remark。

## 10. Pricing and Promotion Requirements

### 10.1 Base Price

SKU 有基础价。

### 10.2 Option / Modifier Price Delta

所有选项和加料价格都必须在下单时被展开并快照。

### 10.3 Member Pricing

会员折扣计算应基于：

- SKU 基础价
- option / modifier 价差

### 10.4 Promotion Eligibility

促销命中应基于：

- SKU
- 分类
- 套餐
- option 组合

系统至少要支持以后扩展到：

- exclusive SKU set
- category set
- bundle-specific promotion

## 11. Reporting Requirements

SKU / 菜品体系必须支持报表分析：

- SKU 销量
- Product 销量
- Category 销量
- Add-on 销量
- 辣度 / 糖度等偏好趋势
- `OUT_OF_STOCK` 频率
- 套餐槽位偏好

后续 AI 分析也会依赖这些结构化字段：

- 哪些 SKU 该主推
- 哪些组合最常见
- 哪些 add-on 收益高
- 哪些 SKU 经常售罄

## 12. Merchant Admin Configuration Requirements

商户后台应支持：

- 分类管理
- Product 管理
- SKU 管理
- 图片与文案管理
- option group 管理
- option value 管理
- modifier 管理
- 套餐槽位管理
- 门店可售状态管理
- display order 管理

### 12.1 Bulk Operations

一期就应考虑以下批量能力：

- 批量上下架
- 批量门店启停
- 批量售罄 / 恢复
- 批量调价

## 13. Recommended Data Model Additions

在现有 `products / skus / sku_option_groups / sku_option_values / store_sku_availability` 基础上，建议进一步补齐：

- `product_status`
- `sku_status`
- `store_sku_availability_status`
- `modifier_groups`
- `modifier_values`
- `combo_slots`
- `combo_slot_allowed_skus`
- `order_item_modifier_snapshots`
- `kitchen_item_status`
- `kitchen_item_events`

## 14. V2 Implementation Priority

### Phase 1

先做最关键的：

- Product / SKU distinction
- Global SKU status
- Store availability status
- Basic option groups
- Basic option values
- Snapshot fields

### Phase 2

再做：

- Add-ons
- Exclusions
- Combo slots
- Price delta rules
- Menu display logic

### Phase 3

再做：

- Kitchen item fulfillment integration
- Batch merchant tools
- Advanced reporting fields
- AI-ready recommendation inputs

## 15. Final Definition

最终应明确：

- `Product` 是展示对象
- `SKU` 是交易对象
- `Store Availability` 是门店可售对象
- `Order Item` 是订单快照对象
- `Kitchen Item` 是履约对象

只有这几层拆清楚，餐饮系统里的：

- 大中小
- 辣度
- 糖度
- 加料
- 去料
- 套餐
- 售罄
- 准备中
- 已出菜

才不会混成一团。
