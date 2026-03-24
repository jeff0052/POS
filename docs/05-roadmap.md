# Roadmap

## 1. Roadmap Goal

本路线图用于说明 Restaurant POS 从当前原型阶段到一期可交付版本，再到平台化扩展阶段的推进路径。

当前项目已经不再是单一 POS，而是一个四端系统：

- 总后台
- 商户后台
- 门店 POS
- 顾客 QR 扫码点餐

因此 roadmap 需要同时反映：

- 交易闭环如何落地
- 四端如何分阶段成型
- 核心业务域如何逐步补全

## 2. Current Status

当前已经完成的基础：

- Android POS 原型与预览基础
- Merchant Admin 骨架
- Backend 骨架
- QR Ordering Web
- POS 与 QR 统一桌台活动订单主线
- 基础 Docker 环境
- 核心文档体系
- 术语和状态模型统一

当前已明确的一期目标：

- 下单系统
- CRM 会员系统
- 促销与结账拆解
- 销售报表与 GTO
- 四端角色边界逐步成型

## 3. Product Evolution Stages

### Stage 0: Prototype and Validation

目标：

- 验证餐饮 POS 核心交易模型
- 打通 POS 与 QR 共用桌台活动订单
- 确认产品术语、状态流和页面主结构

当前完成重点：

- POS 与 QR 一桌一单主线
- QR 下单与 cashier 结账闭环基础
- Android Preview / Merchant Admin / Backend 基础可联调

### Stage 1: Store Transaction MVP

目标：

- 交付门店可用的一期交易闭环

覆盖端：

- 门店 POS
- 顾客 QR 扫码点餐
- 商户后台（基础经营与管理）

一期必须完成：

- 桌台管理
- POS 点单
- QR 点餐
- 送厨 / 待结账 / 已结账状态流
- cashier 换班 / 交班
- 订单、退款、打印基础
- CRM 基础
- 促销基础
- 销售报表基础
- GTO 日结批量同步基础

### Stage 2: Merchant Operations Expansion

目标：

- 完整支撑商户经营管理

覆盖端：

- 商户后台

重点建设：

- 会员管理
- 充值与积分流水
- 等级与权益配置
- 促销规则中心
- SKU / Catalog 管理
- 财务报表与对账
- GTO 管理与重试
- 员工与角色权限

### Stage 3: Platform Control Center

目标：

- 形成平台级总后台

覆盖端：

- 总后台

重点建设：

- 商户管理
- 门店开通与停用
- 平台账号与权限
- 设备与终端管理
- 配置模板下发
- 平台级订单监管
- 平台运营与技术支持视图

### Stage 4: Production Hardening

目标：

- 支撑更多门店、更稳定的试点和持续上线

重点建设：

- 稳定性与监控
- 部署体系升级
- 数据备份与恢复
- 实时同步能力
- 更完整的多租户支持

## 4. Phase Plan by Business Domain

### Phase 1: Unified Table Order Foundation

目标：

- 打通一桌一单主线
- 让 POS 与 QR 共用活动桌单

已完成重点：

- QR 下单基础
- POS 点菜基础
- 统一桌单主线
- POS 与 QR 不再互相覆盖

待继续补齐：

- 原生 Android 状态流继续收敛
- 厨房单视角
- 商户后台状态自动刷新

### Phase 2: Cashier Shift and Store Operations

目标：

- 完成门店 POS 的 cashier 班次逻辑

内容：

- cashier 登录
- 开班
- 当班订单归属
- 交班 / 关班
- 班次汇总
- 交班审计

### Phase 3: CRM Foundation

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

### Phase 4: SKU / Catalog Foundation

目标：

- 让商品和可售卖单元稳定化

内容：

- 分类
- 商品
- SKU
- 规格选项
- 门店可售范围
- 价格快照基础

### Phase 5: Promotion Rule Center

目标：

- 完成促销配置和结账命中

内容：

- 满减
- 满赠
- 会员价
- 等级折扣
- 充值赠送

### Phase 6: Settlement and Reporting

目标：

- 完成 cashier 结账与报表沉淀

内容：

- 结账流水
- 优惠拆解
- 销售报表
- 会员消费报表
- 充值报表
- 单桌销售和翻台表现

### Phase 7: GTO Batch Sync

目标：

- 支撑商场日结数据同步

内容：

- GTO 批次生成
- 导出
- 重试
- 失败记录

### Phase 8: Platform Admin

目标：

- 构建总后台第一版

内容：

- 商户管理
- 门店管理
- 平台员工权限
- POS 配置管理
- 门店状态巡检

## 5. Milestones

### Milestone A

统一桌台订单模型稳定运行

成功标准：

- POS 与 QR 不互相覆盖
- 一桌一单
- 桌台状态清晰
- cashier 可接手顾客扫码订单

### Milestone B

门店交易 MVP 可演示

成功标准：

- 门店 POS 和顾客 H5 主流程可跑通
- cashier 状态流清晰
- 订单可从点单推进到结账完成

### Milestone C

商户经营管理基础可用

成功标准：

- CRM 基础可用
- SKU / Catalog 基础可用
- 报表基础可用
- 商户后台可支撑门店日常管理

### Milestone D

促销与 GTO 基础可用

成功标准：

- 满减 / 满赠 / 会员价可配置
- 结账页清晰展示优惠结果
- GTO 批次可生成和重试

### Milestone E

平台总后台第一版成型

成功标准：

- 可管理商户
- 可管理门店
- 可看平台级 POS 状态
- 可下发基础配置

## 6. Recommended Execution Order

建议优先顺序：

1. 统一桌单与状态机
2. cashier 班次与门店 POS 流程
3. CRM 会员基础
4. SKU / Catalog 基础
5. 促销规则中心
6. 结账与报表
7. GTO 批量同步
8. 总后台

## 7. Current Priority

当前最优先事项：

- 统一桌台订单状态在三端表现
- 原生 Android 状态流继续收敛
- cashier 班次逻辑建模
- CRM 核心实体与后台入口
- SKU 作为商品中枢纳入后端与后台设计

## 8. Delivery Guideline

每次版本迭代建议都围绕一个明确闭环：

- 闭环 1：一桌一单
- 闭环 2：cashier 班次与结账
- 闭环 3：会员识别与优惠
- 闭环 4：SKU / 价格口径稳定
- 闭环 5：结账与报表
- 闭环 6：GTO 导出
- 闭环 7：平台总后台

避免多个业务域同时半完成。
