# Roadmap

## 1. Roadmap Goal

本路线图用于说明 Restaurant POS 从当前版本到一期可交付版本的推进路径。

## 2. Current Status

当前已经完成的核心基础：

- Android POS 原型与预览基础
- Merchant Admin 骨架
- Backend 骨架
- QR Ordering Web
- POS 与 QR 统一桌台活动订单主线
- 基础 Docker 环境

当前已明确的一期目标：

- 下单系统
- CRM 会员系统
- 结账报表与 GTO

## 3. Phase Plan

### Phase 1: Unified Table Order Foundation

目标：

- 打通一桌一单主线
- 让 POS 与 QR 共用活动桌单

已完成重点：

- QR 下单基础
- POS 点菜基础
- 统一桌单主线

待继续补齐：

- 订单状态完整表达
- 下单到厨房流转
- 后台状态同步

### Phase 2: CRM Foundation

目标：

- 完成会员闭环基础

内容：

- 会员列表
- 会员详情
- 会员识别
- 积分
- 充值
- 等级
- 权益

### Phase 3: Promotion Rule Center

目标：

- 完成促销配置和结账命中

内容：

- 满减
- 满赠
- 会员价
- 等级折扣
- 充值赠送

### Phase 4: Settlement and Reporting

目标：

- 完成 cashier 结账与报表沉淀

内容：

- 结账流水
- 优惠拆解
- 销售报表
- 会员消费报表
- 充值报表

### Phase 5: GTO Batch Sync

目标：

- 支撑商场日结数据同步

内容：

- GTO 批次生成
- 导出
- 重试
- 失败记录

## 4. Milestones

### Milestone A

统一桌台订单模型稳定运行

成功标准：

- POS 与 QR 不互相覆盖
- 一桌一单
- 桌台状态清晰

### Milestone B

CRM 基础能力可用

成功标准：

- 可识别会员
- 可积分
- 可充值
- 可设置等级和权益

### Milestone C

促销规则与结账拆解可用

成功标准：

- 满减 / 满赠 / 会员价可配置
- 结账页清晰展示优惠结果

### Milestone D

报表与 GTO 批次可用

成功标准：

- 销售报表可查看
- GTO 批次可生成和重试

## 5. Recommended Execution Order

建议优先顺序：

1. 统一桌单与状态机
2. CRM 会员基础
3. 促销规则中心
4. 结账与报表
5. GTO 批量同步

## 6. Current Priority

当前最优先事项：

- 统一桌台订单状态在三端表现
- cashier 与 QR 同单模型继续收敛
- 商户后台订单状态与桌号一致性
- CRM 核心实体与后台管理入口

## 7. Delivery Guideline

每次版本迭代建议都围绕一个明确闭环：

- 闭环 1：一桌一单
- 闭环 2：会员识别与优惠
- 闭环 3：结账与报表
- 闭环 4：GTO 导出

避免多个业务域同时半完成。
