# V2 Backend Module and Package Structure

## Goal

定义 Restaurant POS `v2 foundation` 的后端模块结构与包结构，作为正式重建时的统一工程基线。

本文件解决的问题是：
- V2 后端应该按什么模块拆
- 每个模块内部怎么分层
- 代码应该放在哪里
- 哪些依赖关系允许，哪些不允许

---

## Design Position

V2 后端采用：
- 单仓库
- 模块化单体
- 按未来微服务边界设计

也就是说：
- 当前先不强拆独立部署
- 但代码组织必须从第一天就具备可拆分性

---

## Top-Level Backend Modules

V2 后端建议按以下领域模块组织：

1. `merchant`
2. `store`
3. `catalog`
4. `order`
5. `member`
6. `staff`
7. `promotion`
8. `settlement`
9. `report`
10. `gto`
11. `platform`
12. `common`

---

## Module Responsibilities

### 1. merchant
管理：
- 商户主体
- 商户状态
- 商户级配置

### 2. store
管理：
- 门店
- 桌台
- 门店配置
- 门店设备绑定

### 3. catalog
管理：
- 分类
- 商品
- SKU
- SKU 选项与规格
- 门店可售范围

### 4. order
管理：
- 活动桌单
- 订单
- 订单项
- QR / POS 共用订单模型
- 厨房提交状态
- 订单生命周期

### 5. member
管理：
- 会员
- 会员账户
- 积分
- 余额
- 充值
- 等级
- 权益

### 6. staff
管理：
- 门店员工
- 平台员工
- 角色权限
- cashier 班次

### 7. promotion
管理：
- 满减
- 满赠
- 会员价
- 等级折扣
- 命中结果

### 8. settlement
管理：
- cashier 结账
- 支付记录
- 退款
- 打印记录
- 结账流水

### 9. report
管理：
- 销售汇总
- 商品销售
- 会员报表
- 优惠报表
- 桌台周转

### 10. gto
管理：
- GTO 批次
- 导出
- 重试
- 同步状态

### 11. platform
管理：
- 总后台账号与权限
- 平台级配置
- 商户/门店管控能力

### 12. common
管理：
- 通用异常
- 通用响应
- 安全
- 配置
- 基础工具

---

## Recommended Package Structure

每个模块内部建议统一使用以下结构：

```text
com.developer.pos.<module>
  ├── application
  │   ├── service
  │   ├── command
  │   ├── query
  │   └── dto
  ├── domain
  │   ├── model
  │   ├── repository
  │   ├── enum
  │   └── policy
  ├── infrastructure
  │   ├── persistence
  │   │   ├── entity
  │   │   ├── mapper
  │   │   └── repository
  │   ├── integration
  │   └── config
  └── interfaces
      ├── rest
      ├── event
      └── scheduler
```

---

## Layer Responsibilities

### application
负责：
- 业务用例编排
- 事务边界
- command / query handler
- 返回 DTO

不负责：
- ORM 细节
- HTTP 层细节
- 领域对象持久化实现

### domain
负责：
- 领域模型
- 领域规则
- 领域枚举
- repository interface
- policy

不负责：
- Controller
- Entity 注解持久化细节
- 第三方依赖调用

### infrastructure
负责：
- JPA entity
- repository implementation
- 外部系统对接
- 第三方 SDK 适配
- 持久化和集成配置

### interfaces
负责：
- REST API
- 定时任务入口
- 事件消费入口

---

## Example Package Mapping

### order module example

```text
com.developer.pos.order
  ├── application
  │   ├── service
  │   │   ├── ActiveTableOrderApplicationService
  │   │   ├── KitchenSubmissionApplicationService
  │   │   └── OrderQueryApplicationService
  │   ├── command
  │   │   ├── CreateOrMergeTableOrderCommand
  │   │   ├── UpdateActiveTableOrderCommand
  │   │   ├── SendOrderToKitchenCommand
  │   │   └── SettleActiveTableOrderCommand
  │   └── dto
  ├── domain
  │   ├── model
  │   │   ├── ActiveTableOrder
  │   │   ├── Order
  │   │   └── OrderItem
  │   ├── enum
  │   │   ├── OrderStatus
  │   │   ├── OrderSource
  │   │   └── DiningType
  │   ├── repository
  │   │   ├── ActiveTableOrderRepository
  │   │   └── OrderRepository
  │   └── policy
  │       └── OrderMergePolicy
  ├── infrastructure
  │   └── persistence
  │       ├── entity
  │       ├── mapper
  │       └── repository
  └── interfaces
      └── rest
          └── OrderController
```

---

## Dependency Rules

### Allowed
- `interfaces -> application`
- `application -> domain`
- `application -> infrastructure` only through explicit adapters or Spring wiring
- `infrastructure -> domain`

### Not Allowed
- `interfaces -> infrastructure` directly
- `domain -> interfaces`
- `domain -> controller / entity / third-party sdk`
- random cross-domain entity access

---

## Cross-Domain Collaboration Rules

跨域协作应通过：
- application service orchestration
- domain repository interface
- clear DTO / command contract

不建议：
- 一个域直接读写另一个域的 JPA entity
- 一个 controller 同时编排多个域的持久化细节

例如：
- `order` 需要会员价时，可调用 `promotion/member` 的应用服务获取 pricing result
- `settlement` 结账成功后更新订单状态，应通过 `order` 的应用服务入口，而不是直接改 `order` 表 entity

---

## V2 Folder Baseline

建议 V2 后端初始目录先落成：

```text
src/main/java/com/developer/pos
  ├── common
  ├── merchant
  ├── store
  ├── catalog
  ├── order
  ├── member
  ├── staff
  ├── promotion
  ├── settlement
  ├── report
  ├── gto
  └── platform
```

数据库迁移也建议同步按模块命名：

```text
src/main/resources/db/migration
  ├── V001__merchant_base.sql
  ├── V002__store_and_table.sql
  ├── V003__catalog_and_sku.sql
  ├── V004__active_table_order.sql
  ├── V005__member_crm.sql
  ├── V006__promotion.sql
  ├── V007__settlement.sql
  ├── V008__report_foundation.sql
  └── V009__gto_batches.sql
```

---

## Rebuild Order for V2 Backend

建议 V2 后端按这个顺序建：

1. `common`
2. `store`
3. `catalog`
4. `order`
5. `settlement`
6. `staff`
7. `member`
8. `promotion`
9. `report`
10. `gto`
11. `merchant`
12. `platform`

原因：
- 先交易
- 再门店运营
- 再经营能力
- 最后平台能力

---

## Immediate Next Step

在进入真正 V2 重建之前，建议立即补以下两份基线：

1. V2 database migration baseline
2. V2 API contract baseline for:
   - active table order
   - qr ordering
   - cashier settlement

---

## Final Position

V2 后端不应再从“按页面需求生长的 controller/service”起步。

应从一开始就采用：
- domain-based modules
- layered package structure
- service-boundary-oriented organization

这样后续不论是扩四端、扩 CRM、扩 GTO，还是进入 AI 层，都有足够稳的后端基础。
