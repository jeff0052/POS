# Design Architecture

## 1. Architecture Goal

本设计架构用于定义 Restaurant POS 一期的产品与系统结构，确保：

- 顾客扫码与 POS 点餐共用统一桌单
- 会员、促销、结账、报表和 GTO 同步围绕统一订单展开
- 前端、后台、后端职责边界清晰

## 2. Product Architecture

### 2.1 Frontend Channels

#### Android POS

职责：

- 桌台管理
- POS 点餐
- 订单复核
- cashier 结账
- 日结

#### QR Ordering Web

职责：

- 顾客进入指定桌台
- 浏览菜单
- 加菜并提交到当前桌活动订单

#### Merchant Admin

职责：

- CRM 管理
- 促销规则管理
- 订单与报表管理
- GTO 同步管理

## 3. Core Domain Architecture

### 3.0 Core Managed Objects

系统围绕 6 类核心对象展开：

- `merchants`
- `stores`
- `orders`
- `members`
- `staff`
- `skus`

其中：

- `orders` 是交易核心
- `skus` 是商品核心

其他域大多围绕订单生命周期与 SKU 定价能力服务。

### 3.1 Table Order Domain

核心原则：

- 每张桌同一时刻只有一张活动订单
- QR 和 POS 共用这张活动订单

#### Order States

- `DRAFT`
- `SUBMITTED`
- `PENDING_SETTLEMENT`
- `SETTLED`

#### Table States

- `AVAILABLE`
- `ORDERING`
- `DINING`
- `PENDING_SETTLEMENT`
- `CLEANING`

### 3.2 CRM Domain

核心对象：

- `members`
- `member_accounts`
- `member_tiers`
- `member_points_ledger`
- `member_recharge_orders`
- `member_benefits`
- `member_price_rules`
- `member_upgrade_rules`

### 3.3 SKU / Catalog Domain

核心对象：

- `product_categories`
- `products`
- `skus`
- `sku_option_groups`
- `sku_options`
- `store_sku_availability`
- `sku_price_rules`

核心职责：

- 菜品分类与展示
- SKU 可售卖单元定义
- 规格 / 口味 / 做法 / 加料等选项建模
- 门店可售范围控制
- SKU 价格与会员价基础
- 套餐与组合售卖能力的基础承载

关键原则：

- 订单项保存 `sku_id`
- 同时保存 `sku_name_snapshot`
- 同时保存 `unit_price_snapshot`
- 同时保存 `option_snapshot`

这样商品改名、改价、改配置后，不会污染历史订单与报表。

### 3.4 Promotion Domain

核心对象：

- `promotion_rules`
- `promotion_rule_items`
- `promotion_hits`
- `pricing_snapshot`

规则执行顺序：

1. 商品基础价
2. 会员价 / 等级价
3. 满减
4. 满赠
5. 最终结账价

### 3.5 Settlement and Reporting Domain

核心对象：

- `settlement_records`
- `sales_reports`
- `discount_reports`
- `member_reports`
- `recharge_reports`
- `gto_export_batches`

## 4. System Architecture

```text
Customer QR H5
  -> Backend Order Service
  -> Unified Table Order

Android POS
  -> Backend Order Service
  -> SKU / Catalog Service
  -> CRM Service
  -> Promotion Service
  -> Settlement Service

Merchant Admin
  -> SKU / Catalog Service
  -> CRM Service
  -> Promotion Service
  -> Report Service
  -> GTO Service

Backend
  -> MySQL
```

## 5. Backend Service Modules

### 5.1 order

负责：

- 当前桌活动订单
- QR 与 POS 统一改单
- 订单状态推进
- 订单快照

### 5.2 member / crm

负责：

- 会员资料
- 积分
- 充值
- 余额
- 等级与权益

### 5.3 sku / catalog

负责：

- 分类
- 商品
- SKU
- 规格选项
- 可售范围
- 价格基础能力

### 5.4 promotion

负责：

- 满减
- 满赠
- 会员价
- 等级折扣
- 规则命中

### 5.5 settlement

负责：

- 结账
- 收款记录
- 优惠明细沉淀
- 清桌触发

### 5.6 report

负责：

- 销售汇总
- 商品销售
- 优惠让利
- 会员消费
- 充值报表

### 5.7 gto

负责：

- 日结批次
- 导出文件
- 重试
- 同步状态

## 6. Data Model Principles

### 6.1 Snapshot First

订单必须保存快照：

- SKU 名称快照
- 菜品价格快照
- SKU 选项快照
- 会员优惠快照
- 促销命中快照
- 结账结果快照

### 6.2 One Source of Truth

活动桌单以服务端为准。

前端可以做本地交互状态，但最终订单事实必须以后端为准。

### 6.3 Separation of Concerns

- 订单不直接承担 CRM 全部职责
- CRM 不直接承担订单状态职责
- 报表不靠页面临时计算

## 7. UI Flow Architecture

### 7.1 Table Management

功能：

- 展示桌台状态
- 点击进入该桌当前活动订单

### 7.2 Ordering

功能：

- 编辑当前桌活动订单
- 不区分来源

### 7.3 Order Review

功能：

- 核对订单
- 继续点菜
- 下单到厨房
- 去结账

### 7.4 Payment / Settlement

功能：

- cashier 收款
- 结账完成后改订单状态
- 清桌

## 8. Integration Architecture

### 8.1 Payment

一期仍沿用现有支付 SDK。

### 8.2 Printer

一期仍沿用现有打印 SDK。

### 8.3 GTO

一期采用日结批量同步：

- 生成批次
- 导出文件或批量推送
- 失败重试

## 9. Non-Goals in Current Architecture

当前不纳入一期架构深度设计：

- 多品牌总部中台
- 多组织财务结算
- 高级库存/供应链
- 复杂营销编排引擎
- 后厨工作站系统
