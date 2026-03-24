# POS V2 Rebuild Plan

## Goal
在不丢失当前业务认知和验证结果的前提下，启动一套新的正式基础工程，作为餐饮 POS 的 `v2 foundation`。

V2 的目标不是“重写同样的 demo”，而是：
- 把已经验证过的业务模型正式化
- 按服务边界重建工程结构
- 为四端和 AI 演进打稳定底座

---

## Why V2

当前代码已经帮助我们完成了这些关键验证：
- 四端产品边界成立
- 一桌一张活动订单成立
- POS 点单和 QR 点单合单成立
- cashier 负责最终结账成立
- 订单状态模型已经稳定
- 术语、对象和文档体系已经清楚

但当前代码也有明显问题：
- 原型代码和正式业务代码混在一起
- 前端页面状态和业务状态耦合较多
- 后端虽然有模块，但核心订单逻辑仍偏集中
- 一些目录结构是按“先跑通”长出来的，不适合继续放大
- 再继续在旧骨架上修补，迭代成本会越来越高

因此，V2 是一次 **结构化重建**，不是情绪化推倒。

---

## V2 Principles

### 1. Freeze Prototype, Rebuild Foundation
- 当前代码视为 `prototype`
- 继续保留用于业务验证、交互参考和回归对照
- 新的正式开发从 `v2 foundation` 开始

### 2. Keep the Proven Model
V2 不重新发明业务模型，直接继承已经验证的核心定义：
- 四端结构
- 六类核心对象
- 一桌一单
- POS + QR 共编辑
- cashier 结账
- 状态模型与术语文档

### 3. Service Boundary First
- 按未来微服务边界设计
- 当前先做模块化单体
- 代码结构优先服务于未来可拆分性

### 4. Rebuild the Core Before the Surface
先重建核心交易底座，再迁移页面和管理功能：
- Store + Table
- Catalog / SKU
- Active Table Order
- QR Ordering
- Cashier Settlement
- Staff Shift

### 5. Migrate by Capability, Not by File
V2 迁移时按能力迁移，不按旧项目文件原样拷贝。

---

## What We Keep from V1 / Prototype

以下内容视为稳定资产，应继续保留：

### Product Assets
- Overview
- MRD
- Design Architecture
- UI Design Requirements
- Roadmap
- Terminology and State Model
- Product Development Methodology
- System Landscape
- AI Vision
- Platform Admin documents

### Business Decisions
- Merchant / Store / Order / Member / Staff / SKU 六类核心对象
- 四端定义
- 一桌一张活动订单
- 订单状态：
  - `DRAFT`
  - `SUBMITTED`
  - `PENDING_SETTLEMENT`
  - `SETTLED`
- 桌台状态：
  - `AVAILABLE`
  - `ORDERING`
  - `DINING`
  - `PENDING_SETTLEMENT`
  - `CLEANING`

### Interaction Learnings
- QR 点单与 POS 点单必须合并到同一活动单
- cashier 修改订单必须持久化，刷新不能回弹
- 结账成功后必须清桌
- 收银流程与点单流程需要明确分离

---

## What We Do Not Carry Forward Directly

以下内容可以继续参考，但不建议作为 V2 的正式基础直接沿用：

### Frontend
- `android-preview-web` 中大量页面内状态逻辑
- 一些为了演示而写死的数据和页面跳转
- 与正式后端模型强耦合不稳的临时 patch

### Backend
- 旧 `OrderService` 风格的集中式逻辑
- 临时 mock 数据映射
- 未按 domain clean boundary 切开的 controller / service 组合

### Native Android
- 部分演示型支付流和 mock 状态
- 与最终真实 API / SDK 脱节的页面状态实现

---

## V2 Target System Structure

### Endpoints / Products
1. Platform Admin
2. Merchant Admin
3. Store POS
4. QR Ordering

### Core Domains
1. Merchant
2. Store
3. Catalog / SKU
4. Order
5. Member / CRM
6. Staff
7. Promotion
8. Settlement
9. Report
10. GTO
11. Platform Admin

---

## V2 Rebuild Phases

## Phase 0: Freeze and Prepare
目标：
- 冻结旧原型
- 明确 V2 范围和目录
- 确定重建起点

产出：
- V2 rebuild plan
- Prototype freeze note
- V2 project structure proposal

---

## Phase 1: Core Transaction Foundation
目标：
- 重建最小交易内核

范围：
- Store
- Table
- Catalog / SKU
- Active Table Order
- POS Ordering
- QR Ordering
- Cashier Settlement

产出：
- 新后端域骨架
- 新数据库迁移结构
- 统一活动单 API
- 新 POS / QR 最小闭环

---

## Phase 2: Store Operation Foundation
目标：
- 重建门店运营能力

范围：
- Staff / Cashier shift
- Refund
- Print integration boundary
- Kitchen submission state
- Merchant admin base pages

产出：
- 班次体系
- 门店后台核心页
- 订单与桌台运营能力

---

## Phase 3: CRM and Promotion
目标：
- 把交易系统升级为经营系统

范围：
- Member
- Points
- Balance / Recharge
- Tier
- Benefits
- Promotion rules
- Order pricing breakdown

产出：
- 会员域
- 促销域
- 统一结算定价快照

---

## Phase 4: Reports and GTO
目标：
- 可对账、可输出、可看经营

范围：
- Sales reports
- Member reports
- Promotion reports
- Table turnover
- GTO export batches

产出：
- 报表中心
- GTO 批量导出
- 商户后台分析能力

---

## Phase 5: Platform Admin
目标：
- 让系统具备平台级可运营能力

范围：
- Merchant onboarding
- Store provisioning
- Device management
- Global config
- Platform support

产出：
- 总后台 MVP

---

## Recommended Build Order

### Backend
1. Order
2. Store
3. Catalog / SKU
4. Staff
5. Settlement
6. Member / CRM
7. Promotion
8. Report
9. GTO
10. Platform Admin

### Frontend
1. QR Ordering
2. Store POS Preview / POS Native
3. Merchant Admin
4. Platform Admin

---

## Engineering Strategy

### Backend Strategy
- Start a new domain-clean backend structure
- Prefer explicit module packages from day one
- Use migrations instead of ad-hoc init growth
- Keep controller contracts minimal and domain-aligned

### Frontend Strategy
- Rebuild by surface, not by copying prototype state
- Shared terminology and state model across all ends
- Keep preview as design validation, not production logic host

### Native Strategy
- Native POS should only keep flows that are product-confirmed
- UI and state should follow the same order model as backend
- Payment / printer SDK boundaries stay abstracted

---

## Definition of Done for V2 Foundation

V2 foundation is considered ready when:
- one active table order per table is enforced
- POS and QR edit the same active table order
- cashier settlement is the only final payment entry
- order state flow is implemented consistently across backend and UI
- SKU and order item snapshots are stable
- staff shift boundary exists
- merchant admin can see and manage real store order flow

---

## What Not to Do

- do not rewrite all four ends at the same time without phase control
- do not start with full microservices deployment
- do not migrate by copying prototype files blindly
- do not rebuild visual polish before transaction core is stable
- do not lose the validated business model while refactoring code

---

## Immediate Next Steps

1. Freeze current codebase as prototype baseline
2. Define V2 backend module/package structure
3. Define V2 database migration baseline
4. Rebuild active table order core first
5. Rebuild QR ordering and POS ordering against the new core
6. Reconnect cashier settlement

---

## Decision

V2 is the correct path.

The team should stop treating the current codebase as the long-term foundation and instead use it as:
- product validation baseline
- interaction reference
- migration reference

The long-term product should move forward on a rebuilt, domain-structured V2 foundation.
