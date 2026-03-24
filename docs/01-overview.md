# Restaurant POS Project Overview

## 1. Project Name

Restaurant POS

## 2. Project Positioning

Restaurant POS 是一套面向餐饮门店的一体化数字经营系统，聚焦中小型餐饮商户的核心经营链路：

- 桌台管理
- 堂食点单
- 桌码扫码点餐
- 前台收银结账
- 会员运营
- 促销结算
- 销售报表
- 商场 GTO 数据同步

它不是一个纯收银工具，也不是一个纯扫码商城，而是一套围绕“门店交易闭环”和“会员运营能力”展开的餐饮业务系统。

## 3. Project Goal

项目目标是交付一套可在真实餐饮门店运行的一期系统，帮助门店完成：

- 顾客扫码或收银员代客点单
- 一桌一单的统一桌台订单管理
- 会员识别、积分、充值、等级权益
- 满减、满赠、会员价等促销结算
- cashier 统一收款结账
- 销售与优惠报表沉淀
- 日结批量同步至商场 GTO 系统

## 4. Core Product Principle

### 4.1 One Active Order Per Table

每一张桌台在同一时刻只允许存在一张活动订单。

该订单可以由：

- 顾客通过桌码扫码点餐
- 收银员或服务员通过 POS 点餐

共同编辑和推进，但不会拆成两张独立订单。

### 4.2 Ordering and Settlement Are Separate

顾客扫码点餐不代表已经付款。

下单与结账分离：

- 顾客扫码负责提交菜品
- cashier 负责最终确认优惠与收款

### 4.3 CRM Is a First-Class Domain

会员系统不是订单上的一个附属字段，而是独立业务域，覆盖：

- 会员注册与绑定
- 积分
- 充值
- 余额
- 等级
- 权益
- 升级规则

### 4.4 Promotion Must Be Auditable

促销不能只在页面上做展示，必须进入订单快照和报表口径，保证：

- 账目清晰
- 优惠可追溯
- 商场对账可落地

## 5. Product Components

本项目由 5 个产品组件组成：

### 5.1 Android POS

用于门店前台和服务员操作：

- 桌台管理
- POS 点单
- 收银结账
- 订单复核
- 日结

### 5.2 QR Ordering Web

用于顾客扫桌码后进行自助点餐：

- 进入指定桌台
- 浏览菜单
- 下单提交至该桌活动订单

### 5.3 Merchant Admin

用于商户后台管理：

- 订单管理
- CRM 管理
- 促销规则管理
- 报表查看
- GTO 同步管理

### 5.4 Backend Transaction Services

用于承载统一订单、会员、促销、报表、同步等业务逻辑。

### 5.5 Boss / Operator Monitoring

后续可用于老板端或运营侧查看：

- 销售表现
- 门店状态
- 会员与活动效果

## 6. Target Market

适用市场主要为：

- 购物中心餐饮门店
- 中小型连锁餐饮
- 快餐、小吃、轻食、奶茶、咖啡等高频点单场景
- 需要商场销售数据同步的商户

## 7. Applicable Scope

### 7.1 Included in Current Scope

- 堂食桌台管理
- 桌码扫码点餐
- POS 点餐
- cashier 收款
- CRM 会员体系
- 积分、充值、等级
- 会员专属价与权益
- 满减、满赠、会员价
- 销售报表
- GTO 日结批量同步

### 7.2 Not Included in Current Scope

- 厨房打印联动深度流程
- 后厨出品工作站
- 多门店总部运营中台
- 外卖平台深度集成
- 复杂营销编排引擎
- 高级多组织财务系统

## 8. Business Value

### For Merchants

- 降低前台点单与结账混乱
- 提升堂食点单效率
- 带动会员沉淀与复购
- 让促销有规则、有报表、有留痕
- 满足商场数据对接需求

### For Store Staff

- 桌台与订单状态更清晰
- 点单与结账职责更明确
- 顾客扫码和 POS 点餐不再相互冲突

### For Mall / Operators

- 销售数据同步更标准
- 对账和活动让利口径更清楚

## 9. Current Stage

当前项目处于一期核心链路搭建阶段，已经完成：

- Android POS 原型与预览基础
- QR 桌码点餐基础
- 商户后台基础
- 后端订单接口骨架
- POS 与 QR 统一桌台活动订单主线

下一阶段将重点完善：

- CRM 闭环
- 促销规则中心
- 报表与 GTO 批量同步
- UI 统一收敛
