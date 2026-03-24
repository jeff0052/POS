# Delivery Integration Architecture

## 1. Purpose

本文档定义餐饮系统中“外卖接入”的统一架构原则。

目标不是做一套独立的外卖系统，而是把外卖平台订单接入现有统一订单中枢，使其成为与 `POS`、`QR` 并列的第三类订单入口。

---

## 2. Core Principle

外卖不应该被设计成独立交易系统。

正确原则是：

- `POS` 点单
- `QR` 扫码点单
- `Delivery` 外卖平台订单

三者共享同一套订单中枢、结算口径、会员口径、促销口径和报表口径。

也就是说：

- 堂食订单走桌台模型
- 外卖订单不走桌台模型
- 但它们都属于统一订单域

---

## 3. Architectural Position

### 3.1 Existing Order Entry Channels

当前系统已有两类入口：

- `POS`
- `QR`

引入外卖后，统一升级为三类入口：

- `POS`
- `QR`
- `DELIVERY`

### 3.2 Delivery as a New Order Source

外卖应作为订单来源字段的一种取值，而不是新建一套平行订单表模型。

建议扩展订单字段：

- `order_source`
  - `POS`
  - `QR`
  - `DELIVERY`
- `channel_source`
  - `MEITUAN`
  - `ELEME`
  - `DELIVERY_MANUAL`
- `fulfillment_type`
  - `DINE_IN`
  - `TAKEAWAY`
  - `DELIVERY`

---

## 4. Why Delivery Must Share the Unified Order Core

如果外卖单独成系统，会造成：

- 报表口径分裂
- CRM 无法统一识别顾客消费
- 促销无法统一计算
- 财务和 GTO 对账困难
- 门店无法统一看经营情况

因此外卖必须与统一订单中枢共享：

- 商品和 SKU
- 价格与优惠快照
- 支付与结算口径
- 报表事实数据
- 退款与对账链路

---

## 5. Domain Model

## 5.1 Shared Order Core

外卖订单进入统一订单中枢后，至少要共享这些核心对象：

- `Order`
- `OrderItem`
- `SettlementRecord`
- `PaymentRecord`
- `PromotionSnapshot`
- `PricingSnapshot`
- `ReportFact`

## 5.2 Delivery-Specific Extension

外卖订单需要额外扩展的信息：

- `external_platform`
- `external_order_no`
- `delivery_contact_name`
- `delivery_contact_phone`
- `delivery_address`
- `delivery_note`
- `platform_paid_amount`
- `merchant_receivable_amount`
- `platform_commission_amount`
- `delivery_status`
- `platform_status_snapshot`

---

## 6. Recommended Service Boundary

建议新增一个独立域：

## Delivery Integration Domain

该域负责：

- 平台订单拉取
- 平台回调接收
- 平台订单号映射
- 平台状态转换
- 同步失败重试
- 平台日志与审计

但它不负责：

- 自己定义订单核心
- 自己做报表口径
- 自己做会员体系
- 自己做促销结算

这些仍然由共享核心域承担：

- `order`
- `settlement`
- `promotion`
- `report`
- `member`

---

## 7. Fulfillment Model

### 7.1 Dine-In Orders

堂食订单：

- 绑定桌台
- 使用活动桌单
- 由 cashier 最终结账

### 7.2 Delivery Orders

外卖订单：

- 不绑定桌台
- 不进入 `Table Management`
- 不参与“一桌一单”
- 但仍然进入订单、结算、报表与退款链路

---

## 8. Delivery Status Model

外卖履约状态建议独立于堂食订单状态管理。

建议第一期使用：

- `NEW`
- `ACCEPTED`
- `PREPARING`
- `READY_FOR_PICKUP`
- `DISPATCHED`
- `COMPLETED`
- `CANCELLED`

而支付与退款状态仍可共享统一结算域：

- `UNPAID`
- `PAID`
- `REFUNDED`

---

## 9. Data Flow

## 9.1 Inbound Flow

平台订单接入流程：

1. 外卖平台创建订单
2. Delivery Integration Domain 拉取或接收回调
3. 将外部订单映射为内部 `Order`
4. 写入订单、订单项、价格快照、促销快照
5. 门店 POS / 商户后台可查看该订单
6. 进入履约和报表链路

## 9.2 Update Flow

状态同步流程：

1. 外卖平台状态变化
2. Delivery Integration Domain 接收状态变化
3. 映射到内部履约状态
4. 更新内部订单与同步日志
5. 后台和报表同步反映结果

---

## 10. Four-End Impact

## 10.1 Platform Admin

总后台负责：

- 平台接入配置
- 商户与平台账号绑定
- 门店与平台店铺映射
- 平台同步监控
- 平台错误重试与支持

## 10.2 Merchant Admin

商户后台负责：

- 查看外卖订单
- 查看外卖报表
- 查看平台同步状态
- 查看平台门店绑定关系

## 10.3 Store POS

门店 POS 负责：

- 查看门店外卖单
- 查看备餐状态
- 打印外卖小票
- 处理门店侧履约动作

## 10.4 Customer QR

顾客扫码点餐端不直接管理外卖平台订单。

它仍然只负责：

- 扫码点餐
- 堂食订单提交
- 会员识别

---

## 11. Phased Delivery Strategy

### Phase 1

先做：

- 外卖订单导入
- 商户后台查看
- 统一报表统计
- 平台同步日志

### Phase 2

再做：

- 门店 POS 查看和处理外卖单
- 出单与打印
- 履约状态流转

### Phase 3

再做：

- 更深入的平台双向同步
- 自动接单
- 自动取消
- AI 外卖运营分析和建议

---

## 12. AI-Ready Considerations

外卖接入同样应按 AI-ready 原则设计。

后续可扩展能力包括：

- AI 识别高取消风险订单
- AI 推荐外卖专属套餐
- AI 推荐高峰时段备餐策略
- AI 识别高差评 SKU
- AI 生成外卖经营摘要

因此 Delivery Integration Domain 也应逐步支持：

- configuration
- recommendation
- execution
- approval
- audit

---

## 13. Non-Goals for Early Delivery

外卖一期不应一开始就做：

- 深度自动化接单策略
- 全量平台营销能力
- 外卖单独会员体系
- 外卖单独报表体系
- 外卖独立库存系统

这些都应建立在统一订单中枢之上逐步扩展。

---

## 14. Final Definition

外卖接入的正式定义应为：

**Delivery is a new order source and fulfillment scenario inside the unified restaurant order core, not a separate transaction system.**
