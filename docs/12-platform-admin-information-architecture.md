# Platform Admin Information Architecture

## 1. Goal

本文档用于定义 Restaurant POS 总后台的一期信息架构，明确：

- 总后台有哪些一级、二级模块
- 每个模块对应哪些页面
- 每个页面的目标是什么
- 哪些页面属于一期必须建设，哪些属于后续扩展

它的作用是让总后台从“需求存在”推进到“可以开始画 UI、拆任务、做实现”。

## 2. IA Design Principles

总后台信息架构遵循以下原则：

- 以平台管理视角组织，而不是门店操作视角
- 先围绕核心管理对象组织页面
- 平台治理优先于经营展示
- 一期只放真正需要的平台能力
- 避免与商户后台的经营页面重复

## 3. Phase 1 Top-Level Navigation

总后台一期建议采用以下一级导航：

1. Dashboard
2. Merchants
3. Stores
4. Devices
5. Configurations
6. Platform Users
7. Support & Monitoring

## 4. Module Structure

### 4.1 Dashboard

目标：

- 给平台管理员一个总览视图

一期建议内容：

- 商户总数
- 门店总数
- 已开通门店数
- 在线终端数
- 异常门店数
- GTO 异常门店数
- 最近平台告警

一期页面：

- Platform Dashboard

### 4.2 Merchants

目标：

- 管理商户生命周期

一期页面：

1. Merchant List
2. Merchant Detail
3. Merchant Create / Edit

Merchant Detail 建议包含：

- 基础信息
- 状态
- 名下门店
- 配置模板绑定
- 接入状态摘要

### 4.3 Stores

目标：

- 管理门店生命周期与接入状态

一期页面：

1. Store List
2. Store Detail
3. Store Create / Edit

Store Detail 建议包含：

- 所属商户
- 基础信息
- 接入状态
- 终端设备
- 配置状态
- GTO 状态

### 4.4 Devices

目标：

- 管理门店 POS 终端和设备状态

一期页面：

1. Device List
2. Device Detail

Device Detail 建议包含：

- 所属门店
- 设备类型
- 在线状态
- 支付配置状态
- 打印配置状态
- 最后心跳时间

### 4.5 Configurations

目标：

- 管理平台模板和门店配置下发

一期页面：

1. Configuration Template List
2. Template Detail / Edit
3. Store Configuration Assignment

一期建议先覆盖：

- Payment config template
- Printer config template
- GTO config template

### 4.6 Platform Users

目标：

- 管理平台内部账号和角色

一期页面：

1. Platform User List
2. User Detail
3. Role / Permission Matrix

### 4.7 Support & Monitoring

目标：

- 支撑平台实施、运维、客服定位问题

一期页面：

1. Store Health Monitor
2. Exception Center
3. GTO Sync Monitor

一期建议重点异常：

- 门店未完成接入
- 终端离线
- 支付 / 打印配置异常
- GTO 同步失败

## 5. Recommended Navigation Tree

```text
Platform Admin
  Dashboard
  Merchants
    Merchant List
    Merchant Detail
    Merchant Create/Edit
  Stores
    Store List
    Store Detail
    Store Create/Edit
  Devices
    Device List
    Device Detail
  Configurations
    Template List
    Template Detail/Edit
    Store Configuration Assignment
  Platform Users
    User List
    User Detail
    Role/Permission Matrix
  Support & Monitoring
    Store Health Monitor
    Exception Center
    GTO Sync Monitor
```

## 6. Phase 1 Must-Have Pages

一期必须有：

- Platform Dashboard
- Merchant List
- Merchant Detail
- Store List
- Store Detail
- Device List
- Configuration Template List
- Platform User List
- Store Health Monitor

## 7. Phase 1 Optional but Valuable

一期可选增强：

- Merchant Create / Edit
- Store Create / Edit
- Device Detail
- Template Detail / Edit
- GTO Sync Monitor
- Exception Center

## 8. Deferred Pages

后续再做：

- 平台计费页
- 平台高级财务结算
- 平台工单系统
- 平台自动化运维编排
- 高级运营分析页

## 9. Relationship to Merchant Admin

总后台页面的重点是：

- 看平台视角的管理和治理

商户后台页面的重点是：

- 看门店经营和交易

所以总后台不应出现大量以下页面：

- 详细经营报表中心
- cashier 收银页面
- 顾客点餐页面

这些应继续留在商户后台、POS、QR 端。

## 10. Final Position

总后台一期的信息架构应先围绕：

- 商户
- 门店
- 设备
- 配置
- 平台人员
- 监控支持

六类平台管理对象展开，而不是一开始做成过重的超级运营中台。
