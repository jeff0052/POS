# Service-Oriented Architecture Principle

## 1. Goal

本文档用于明确 Restaurant POS 在架构上的一个核心原则：

**按微服务边界设计，按模块化单体推进交付。**

这条原则用于解决两个常见问题：

- 如果从一开始就硬拆大量微服务，系统会过早复杂化
- 如果一开始完全按大单体堆功能，后续扩展会非常痛苦

因此本项目采用的不是“立刻全面微服务化”，而是：

**先按服务域设计，再按阶段决定是否物理拆分。**

## 2. Principle Statement

Restaurant POS 的产品能力应按服务边界定义清楚，使每个业务域都具备未来独立服务化的可能。

当前交付阶段可以采用模块化单体实现，但必须确保：

- 服务边界清晰
- 数据职责清晰
- 接口边界清晰
- 跨域依赖可控

## 3. Why This Principle Is Needed

本项目最终不是一个简单 POS，而是一个不断扩展的系统，未来会包含：

- 总后台
- 商户后台
- 门店 POS
- 顾客扫码点餐
- CRM
- Promotion
- GTO
- AI 分析与执行层

如果不从一开始就考虑服务边界，后续系统变大后会出现：

- 模块互相缠绕
- 订单、会员、促销之间改动牵一发动全身
- 后端越来越难迭代
- 多端需求越来越难协调

## 4. Recommended Service Boundaries

### 4.1 Merchant Service

负责：

- 商户
- 商户状态
- 商户级基础配置

### 4.2 Store Service

负责：

- 门店
- 桌台
- 门店配置
- 终端绑定

### 4.3 Catalog / SKU Service

负责：

- 分类
- 商品
- SKU
- 规格
- 价格基础

### 4.4 Order Service

负责：

- 活动桌单
- 订单项
- 状态流
- QR / POS 合单
- 下单到厨房
- 待结账 / 已结账

### 4.5 Member / CRM Service

负责：

- 会员
- 积分
- 余额
- 充值
- 等级
- 权益

### 4.6 Promotion Service

负责：

- 满减
- 满赠
- 会员价
- 促销规则命中

### 4.7 Settlement Service

负责：

- 收银结账
- 支付记录
- 退款
- 打印结算侧状态
- cashier 班次归属

### 4.8 Report Service

负责：

- 销售报表
- 会员报表
- 优惠报表
- 单桌表现
- 经营摘要

### 4.9 GTO Service

负责：

- GTO 批次
- 导出
- 重试
- 同步记录

### 4.10 Platform Admin Service

负责：

- 平台账号
- 平台配置
- 平台控制台能力

## 5. Implementation Strategy

当前阶段的建议实现方式：

### 5.1 Short Term

采用：

- 单仓库
- 模块化单体
- 按服务域拆包 / 拆模块

要求：

- 每个域有自己的 controller / service / dto / entity / repository
- 域之间通过清晰接口协作
- 禁止任意跨域写数据

### 5.2 Medium Term

当系统复杂度上升后，可逐步拆分：

- CRM
- Promotion
- GTO
- Report

这些更容易成为独立服务。

### 5.3 Long Term

在平台规模扩大后，再评估：

- 真正独立部署
- 异步事件流
- 独立扩容
- 多租户隔离增强

## 6. Decision Standard

后续新增能力时，应先问：

1. 这个需求属于哪个服务域？
2. 它的数据归谁管理？
3. 谁是这个域的 source of truth？
4. 它是否会不合理地跨域写数据？
5. 它未来是否适合独立演进？

如果这些问题答不清，说明边界设计还不够成熟。

## 7. What We Should Avoid

### 不建议：

- 一开始就把所有模块都拆成独立部署单元
- 为了“微服务”而微服务
- 把早期复杂度推高到影响交付速度

### 也不建议：

- 所有能力堆在一个大模块里
- 订单、会员、促销互相直接穿透读写
- 页面需求直接驱动后端结构

## 8. Final Position

Restaurant POS 的正确架构策略不是：

- “现在就全面微服务”

也不是：

- “先做一个大单体以后再说”

而是：

**按微服务边界设计，按模块化单体交付，并为后续演进保留清晰边界。**
