# Backend Domain Breakdown

## 1. Goal

本文档用于把 Restaurant POS 的后端能力正式拆成领域模块，明确：

- 后端应该有哪些核心域
- 每个域负责什么
- 数据归谁管理
- 域之间如何协作

本文件是“按微服务边界设计”的具体落地版本。

## 2. Design Principle

当前阶段后端采用：

- 单仓库
- 模块化单体

但领域拆分必须按未来可服务化边界来设计。

目标是：

- 现在保持开发效率
- 后续保持可拆分性

## 3. Core Backend Domains

### 3.1 Merchant Domain

职责：

- 商户基础信息
- 商户状态
- 商户级配置

主要对象：

- `Merchant`
- `MerchantStatus`
- `MerchantConfig`

### 3.2 Store Domain

职责：

- 门店基础信息
- 桌台
- 门店配置
- 终端绑定

主要对象：

- `Store`
- `Table`
- `StoreConfig`
- `StoreTerminal`

### 3.3 Catalog / SKU Domain

职责：

- 分类
- 商品
- SKU
- 规格 / 选项
- 门店可售范围
- 基础价格

主要对象：

- `Category`
- `Product`
- `Sku`
- `SkuOptionGroup`
- `SkuOption`
- `StoreSkuAvailability`

### 3.4 Order Domain

职责：

- 活动桌单
- 订单项
- POS 与 QR 共用活动桌单
- 一桌一单
- 订单状态流转
- 下单到厨房
- 待结账

主要对象：

- `ActiveTableOrder`
- `Order`
- `OrderItem`
- `OrderSource`
- `OrderStatus`

这是整个系统最核心的后端域。

### 3.5 Member / CRM Domain

职责：

- 会员资料
- 会员账户
- 积分
- 余额
- 充值
- 等级
- 权益

主要对象：

- `Member`
- `MemberAccount`
- `MemberPointsLedger`
- `MemberRechargeOrder`
- `MemberTier`
- `MemberBenefit`

### 3.6 Staff Domain

职责：

- 员工账号
- 门店员工
- 平台员工
- 角色与权限
- cashier 班次

主要对象：

- `Staff`
- `Role`
- `Permission`
- `CashierSession`

### 3.7 Promotion Domain

职责：

- 满减
- 满赠
- 会员价
- 等级折扣
- 规则命中结果

主要对象：

- `PromotionRule`
- `PromotionRuleItem`
- `PromotionHit`
- `PricingBreakdown`

### 3.8 Settlement Domain

职责：

- cashier 结账
- 支付记录
- 退款
- 结账结果
- 清桌触发
- 打印结算侧记录

主要对象：

- `SettlementRecord`
- `PaymentRecord`
- `RefundRecord`
- `PrintRecord`

### 3.9 Report Domain

职责：

- 销售汇总
- 商品销售
- 优惠让利
- 会员消费
- 充值报表
- 单桌表现

主要对象：

- `SalesSummary`
- `ProductSalesReport`
- `DiscountReport`
- `MemberSalesReport`
- `RechargeReport`

### 3.10 GTO Domain

职责：

- GTO 批次生成
- 导出文件
- 重试
- 同步状态记录

主要对象：

- `GtoExportBatch`
- `GtoExportItem`
- `GtoSyncStatus`

### 3.11 Platform Admin Domain

职责：

- 平台管理视角能力
- 平台账号与权限
- 商户与门店管理入口
- 配置模板管理

主要对象：

- `PlatformUser`
- `PlatformRole`
- `ConfigurationTemplate`

## 4. Domain Ownership Rules

### 4.1 Order Is the Transaction Core

订单域是交易中枢，但不应吞掉所有职责。

它负责：

- 订单生命周期
- 桌台活动订单
- 订单项快照

它不负责直接承载：

- 会员完整模型
- 促销完整规则中心
- 报表计算
- GTO 导出

### 4.2 SKU Is the Catalog Core

商品与可售卖单元必须由 Catalog / SKU 域负责。

订单项只保存 SKU 快照，不直接负责商品配置管理。

### 4.3 CRM and Promotion Must Stay Independent

CRM 和 Promotion 可以影响订单价格，但：

- 规则归 CRM / Promotion 自己管理
- 订单只保存命中快照

### 4.4 Settlement Is Not the Same as Order

订单域表示“这单是什么”，结账域表示“这单怎么收的钱、怎么退款、怎么打印”。

## 5. Domain Collaboration

### QR / POS Ordering

- Store Domain 提供桌台上下文
- Catalog / SKU Domain 提供商品与 SKU
- Order Domain 维护活动桌单
- Member / CRM Domain 提供会员身份
- Promotion Domain 提供优惠命中

### Cashier Settlement

- Order Domain 提供订单主体
- Promotion Domain 提供最终优惠拆解
- Settlement Domain 负责收款与退款
- Staff Domain 提供 cashier / 班次归属
- Report Domain 读取结果生成报表

### GTO Export

- Report Domain 提供统计口径
- Settlement Domain 提供收款事实
- GTO Domain 负责导出、重试和状态

## 6. Recommended Package / Module Shape

```text
backend
  merchant
  store
  catalog
  order
  member
  staff
  promotion
  settlement
  report
  gto
  platform
  common
```

每个域建议都有：

- controller
- service
- dto
- entity
- repository

## 7. Evolution Strategy

### Phase 1

先以模块化单体实现。

### Phase 2

优先可独立演进的域：

- member
- promotion
- report
- gto

### Phase 3

随着规模上升，再考虑：

- 独立部署
- 异步事件流
- 更清晰的跨域通信

## 8. Final Position

Restaurant POS 后端不应围绕页面拆，也不应围绕“一个大订单服务”堆所有能力。

正确方式是：

**围绕核心领域拆分后端，让每个域都具备未来独立服务化的可能。**
