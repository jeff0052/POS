# SKU Status and Execution Rules

## 1. Purpose

这份文档用于把 SKU 的状态模型从“有几个状态”推进到“状态如何执行、前后端怎么表现、优先级怎么判定”。

## 2. Status Layers

SKU 相关状态必须拆成 3 层：

1. Product Status
2. SKU Global Status
3. Store-Level Availability Status

厨房履约状态不属于 SKU 状态，单独归订单项 / 厨房项。

## 3. Product Status

- `DRAFT`
- `ACTIVE`
- `INACTIVE`

### Rules

- `DRAFT`
  - 只在后台可见
  - POS / QR 不可见
- `ACTIVE`
  - 允许进入菜单
- `INACTIVE`
  - 不允许新卖
  - 历史订单继续展示

## 4. SKU Global Status

- `DRAFT`
- `ACTIVE`
- `INACTIVE`

### Rules

- Product `ACTIVE` 且 SKU `ACTIVE` 才有资格进入门店菜单候选集
- SKU `INACTIVE` 后：
  - 新订单不可再卖
  - 历史订单按 snapshot 展示

## 5. Store Availability Status

- `AVAILABLE`
- `OUT_OF_STOCK`
- `DISABLED_FOR_STORE`

### Meanings

- `AVAILABLE`
  - 当前门店可卖
- `OUT_OF_STOCK`
  - 临时售罄
  - 可恢复
- `DISABLED_FOR_STORE`
  - 当前门店长期不卖

## 6. Resolution Priority

菜单展示资格按以下优先级判断：

1. Product 必须 `ACTIVE`
2. SKU 必须 `ACTIVE`
3. Store availability 必须 `AVAILABLE`

只要任一层不满足，都不能被正常售卖。

## 7. Frontend Behavior Rules

## 7.1 POS

### DRAFT / INACTIVE

- 默认不展示

### OUT_OF_STOCK

推荐策略：

- POS 默认展示为灰态
- 允许门店人员看到但不可点

原因：

- 门店员工需要知道这个菜存在，只是当前卖完

## 7.2 QR

### DRAFT / INACTIVE / DISABLED_FOR_STORE

- 不展示

### OUT_OF_STOCK

推荐策略：

- 默认不展示
或
- 展示但明确不可点

一期建议默认不展示，减少顾客困惑。

## 8. Historical Order Rules

历史订单不回读商品表判断显示内容。  
历史订单应完全依赖快照。

因此：

- SKU 改名不影响历史单
- SKU 下架不影响历史单
- SKU 售罄不影响历史单

后台查看历史订单时，可以额外显示“该 SKU 当前已下架/售罄”，但不能覆盖快照名称。

## 9. Availability Change Rules

## 9.1 Global Deactivation

当 SKU 全局变为 `INACTIVE`：

- 所有门店立即不可新卖
- 当前购物车中的该 SKU 提交时应被拒绝
- 返回明确错误：
  - `SKU_NOT_ACTIVE`

## 9.2 Store Out of Stock

当门店级变为 `OUT_OF_STOCK`：

- 当前门店不可新卖
- 其他门店不受影响
- 当前购物车提交时应被拒绝
- 返回：
  - `SKU_NOT_AVAILABLE`

## 9.3 Existing Unsubmitted Drafts

如果顾客或 POS 已把 SKU 加进草稿，此时 SKU 状态变化：

- 在最终提交时重新校验
- 不保证草稿中旧内容天然有效

这是为了避免“购物车里有，但实际已经卖不了”。

## 10. Fulfillment Status Rules

以下状态不属于 SKU，而属于订单项或厨房项：

- `SUBMITTED`
- `PREPARING`
- `READY`
- `SERVED`
- `VOIDED`

### Execution Ownership

- `SUBMITTED`
  - Order / POS 发出
- `PREPARING`
  - Kitchen 进入制作
- `READY`
  - Kitchen 已出菜
- `SERVED`
  - Front-of-house / runner / waiter 确认已上桌
- `VOIDED`
  - 有权限角色作废

## 11. Visibility Rules for Fulfillment

### POS

应能看到：

- 已送厨
- 制作中
- 已出菜
- 已上桌

### QR

可根据产品策略选择：

- 仅显示“制作中 / 已出菜”
或
- 不展示厨房细状态

一期可简化，但模型必须预留。

## 12. Error and Validation Rules

推荐最小错误码：

- `PRODUCT_NOT_ACTIVE`
- `SKU_NOT_ACTIVE`
- `SKU_NOT_AVAILABLE`
- `ATTRIBUTE_VALUE_INVALID`
- `REQUIRED_ATTRIBUTE_MISSING`
- `ATTRIBUTE_SELECTION_EXCEEDED`
- `ATTRIBUTE_COMBINATION_NOT_ALLOWED`

## 13. Execution Summary

- Product / SKU / Store availability 三层状态必须分开
- 前台是否可卖，按三层联合判断
- 历史订单只看快照
- 厨房履约状态不属于 SKU
- 状态变化在提交时必须重新校验
