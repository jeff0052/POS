# SKU Model Detail Decisions

## 1. Purpose

这份文档用于把 SKU 需求从“方向正确”推进到“模型边界明确”。

重点回答以下问题：

- `Attribute Group` 和 `Modifier Group` 是否分开建模
- `Product-level attribute` 和 `SKU-level attribute` 的边界
- 套餐槽位和普通 attribute 的关系
- `Remark` 和结构化 attribute 的边界
- 厨房显示信息应该来自哪里

## 2. Final Modeling Decision

最终建议采用：

- `Product`
- `SKU`
- `Attribute Group`
- `Attribute Value`
- `Modifier Group`
- `Modifier Value`
- `Combo Slot`
- `Store SKU Availability`
- `Order Item Snapshot`
- `Kitchen Item`

这是比“一个 option 表走天下”更清晰的做法。

## 3. Attribute vs Modifier

## 3.1 Why They Should Be Separate in Domain Semantics

两者都可能影响价格，但业务语义不同：

### Attribute

更像“顾客必须或通常会做的选择维度”。

示例：

- Size
- Spicy Level
- Temperature
- Sugar Level
- Soup Base
- Main Choice

### Modifier

更像“附加、替换、升级、去料”的动作。

示例：

- Add egg
- Add cheese
- Extra rice
- No coriander
- Replace fries with salad

## 3.2 Implementation Suggestion

需求层建议分开表达。  
实现层可以：

- 使用两张表
或
- 使用一套通用表 + `kind` 字段

但无论实现如何，产品和 API 语义上都必须区分：

- `attributes`
- `modifiers`

## 4. Product-Level vs SKU-Level Configuration

## 4.1 Product-Level

适合定义：

- 所有规格共用的图片与文案
- 默认适用的 attribute 模板
- 默认厨房标签模板

示例：

`Milk Tea`

产品层共用：

- Temperature
- Sugar Level
- Add-ons

## 4.2 SKU-Level

适合定义：

- 真正可卖的规格
- 基础价
- 税类
- 厨房工位
- 特定 SKU 才有的 attribute / modifier

示例：

- `Milk Tea Small`
- `Milk Tea Large`
- `Milk Tea Hot Small`

## 4.3 Recommended Rule

推荐规则：

- `Product` 挂默认可选配置
- `SKU` 可继承，也可覆写

也就是：

- `product_attributes` 负责模板默认分配
- `sku_attributes` 负责最终实际可选项

前端最终只看 SKU 级展开后的结果。

## 5. Combo Slot vs Normal Attribute

## 5.1 Why Combo Slot Is Not Just Another Attribute

套餐槽位虽然也是“选择”，但比普通 attribute 更强：

- 它决定套餐结构
- 它有 allowed SKU set
- 它有免费额度和升级差价
- 它影响厨房拆单显示

所以不建议把套餐槽位简单并入普通 attribute。

## 5.2 Combo Slot Should Be a First-Class Model

推荐：

- `combo_slots`
- `combo_slot_allowed_skus`
- `combo_slot_rules`

示例：

套餐：

- Main
- Side
- Drink

每个槽位都有：

- required / optional
- single / multi
- allowed SKUs
- upgrade price rules

## 5.3 UI Interpretation

POS / QR 点套餐时，应优先显示“槽位选择”而不是一堆普通 attribute。

## 6. Remark Boundary

## 6.1 Structured First

系统原则：

- 高频、可预测、可统计的需求必须结构化
- 低频、例外、一次性要求才走 remark

## 6.2 Should Be Structured

以下应优先结构化：

- 不要香菜
- 不要葱
- 少冰
- 去辣
- 加蛋
- 加饭
- 熟度
- 甜度
- 温度

## 6.3 Can Stay as Remark

以下可先作为 remark：

- 分开打包
- 赶时间先上
- 生日蜡烛
- 请写贺词
- 先上小孩那份

## 6.4 Practical Rule

如果一个 remark 在门店里反复出现，就应升级为结构化 attribute 或 modifier。

## 7. Kitchen Display Source

厨房显示不应该直接读后台配置对象，而应读订单快照。

厨房需要的不是完整 schema，而是：

- 菜名
- 数量
- 关键 attribute
- 关键 modifier
- remark

因此推荐订单项保存：

- `kitchen_item_name_snapshot`
- `attribute_summary_snapshot`
- `modifier_summary_snapshot`
- `remark_snapshot`

## 8. Final Domain Boundary

### Catalog Domain Owns

- Category
- Product
- SKU
- Attribute definitions
- Modifier definitions
- Combo slot definitions
- Store availability

### Order Domain Owns

- Order item snapshot
- Applied attributes
- Applied modifiers
- Item pricing expansion

### Kitchen Domain Owns

- Kitchen item lifecycle
- Preparing / Ready / Served
- Station routing

## 9. Final Decision Summary

- `Attribute` 和 `Modifier` 在语义上分开
- `Product` 和 `SKU` 继续分层
- `Product` 可给默认模板，`SKU` 决定最终可售配置
- 套餐槽位作为 first-class model
- `Remark` 不替代结构化属性
- 厨房和订单只读快照，不回读商品配置表
