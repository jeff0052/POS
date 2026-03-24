# Platform Admin Requirements

## 1. Goal

本文档用于定义 Restaurant POS 总后台的一期需求边界，明确：

- 总后台服务谁
- 总后台为什么存在
- 总后台与商户后台的区别
- 总后台需要管理哪些核心对象
- 一期优先建设哪些能力

## 2. Product Positioning

总后台是平台方使用的管理控制台，不面向门店 cashier，也不面向顾客。

它的作用不是替代商户后台，而是站在平台视角管理：

- 商户
- 门店
- 平台员工
- 终端与设备
- 平台级配置
- 平台级风险与运营问题

## 3. Target Users

总后台的目标用户包括：

- 平台管理员
- 平台运营
- 平台财务
- 平台实施
- 平台技术支持 / IT

## 4. Why Platform Admin Is Needed

如果没有总后台，平台方将无法有效管理：

- 商户开通和停用
- 门店部署与配置
- POS 终端状态
- 支付 / 打印 / GTO 配置下发
- 平台级异常排查
- 平台级经营视图

总后台的存在，是为了让系统从“单商户工具”升级成“平台型餐饮系统”。

## 5. Difference from Merchant Admin

### 5.1 Platform Admin

站在平台视角，管理：

- 所有商户
- 所有门店
- 平台配置
- 终端和接入状态
- 平台级风险和支持

### 5.2 Merchant Admin

站在单个商户视角，管理：

- 自己的门店
- 自己的订单
- 自己的会员
- 自己的员工
- 自己的促销和报表

一句话区分：

- 总后台管理“平台和商户体系”
- 商户后台管理“单个商户经营”

## 6. Core Managed Objects

总后台一期重点管理这些对象：

- Merchant
- Store
- Platform Staff
- Device / Terminal
- Platform Configuration
- Merchant Activation Status
- Store Integration Status

## 7. Functional Scope

### 7.1 Merchant Management

功能：

- 商户列表
- 商户详情
- 商户开通
- 商户停用
- 商户状态管理
- 商户基础信息维护

### 7.2 Store Management

功能：

- 门店列表
- 门店详情
- 门店开通
- 门店停用
- 门店归属商户
- 门店状态查看

### 7.3 Platform Account and Role Management

功能：

- 平台账号管理
- 平台角色管理
- 平台权限分配

### 7.4 Device and Terminal Management

功能：

- POS 终端列表
- 终端绑定门店
- 终端在线状态
- 支付 / 打印配置状态
- 基础设备诊断信息

### 7.5 Platform Configuration

功能：

- 配置模板
- 支付配置模板
- 打印配置模板
- GTO 配置模板
- 门店配置下发

### 7.6 Merchant / Store Support View

功能：

- 门店异常状态查看
- 订单异常定位入口
- 设备异常定位入口
- GTO 同步失败查看

### 7.7 Platform-Level Reporting

一期建议先做轻量：

- 商户数量
- 门店数量
- 已开通门店
- 异常门店数
- GTO 失败门店数

## 8. What Is Not in Phase 1

一期总后台暂不做：

- 复杂计费 / 订阅系统
- 平台级高级财务结算
- 工单系统
- 复杂自动化运维编排
- 高级多组织审批流
- 大规模数据运营看板

## 9. Recommended Phase 1 Pages

建议总后台一期至少包含：

1. 登录页
2. 平台首页 Dashboard
3. 商户管理页
4. 商户详情页
5. 门店管理页
6. 门店详情页
7. 终端设备页
8. 配置模板页
9. 平台账号与角色页
10. 异常巡检页

## 10. Data and Integration Requirements

总后台一期需要能够查看或管理：

- 商户状态
- 门店状态
- 终端在线状态
- 支付 / 打印 / GTO 配置状态
- 门店接入完整度

但不直接处理：

- 顾客下单
- cashier 点单
- 门店现场结账

## 11. Phase 1 Success Criteria

总后台一期成功标准：

- 可管理商户
- 可管理门店
- 可查看 POS 终端与配置状态
- 可查看门店接入异常
- 可支持平台实施和技术支持进行问题定位

## 12. Final Position

总后台是 Restaurant POS 从“单商户产品”走向“平台型系统”的关键一端。

它负责平台管理与商户体系控制，不替代商户后台和门店 POS，而是为它们提供治理、配置和支撑能力。
