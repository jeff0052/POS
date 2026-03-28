# External Team Start Package

## Purpose

本文档定义如果由外部团队从头开发 Restaurant POS，需要交付给他们的最小开工资料包。

目标不是“给一些参考文档”，而是确保外部团队可以：

- 理解产品目标
- 理解系统边界
- 理解核心业务模型
- 理解接口与数据契约
- 理解验收标准
- 直接开始架构、设计、开发和测试

---

## Core Principle

外部团队从头开发时，不能只给：

- 一个 PRD
- 或一套设计稿

而必须提供一整套：

- 产品包
- 架构包
- 契约包
- 设计包
- 验收包
- 交付包

---

## Package Structure

建议把开工资料分成 6 组：

1. Product Package
2. Architecture Package
3. Contract Package
4. Design Package
5. Acceptance and Test Package
6. Delivery Package

---

## 1. Product Package

### 1.1 Vision and Product Brief

必须说明：

- 产品愿景
- 项目目标
- 一期业务目标
- 不做什么

建议材料：

- `01-overview.md`
- `10-ai-restaurant-system-vision.md`

### 1.2 Market Requirements Document

必须说明：

- 目标客户是谁
- 市场痛点是什么
- 为什么现在做
- 竞争和差异化方向

建议材料：

- `02-market-requirements-document.md`

### 1.3 Scope Definition

必须说明：

- 一期范围
- 二期范围
- out of scope
- 功能优先级

建议材料：

- `05-roadmap.md`
- `26-v2-acceptance-document.md`

---

## 2. Architecture Package

### 2.1 System Landscape

必须说明四端边界：

- Platform Admin
- Merchant Admin
- Store POS
- Customer QR Ordering

每个端必须明确：

- 服务对象
- 主要职责
- 不负责什么

建议材料：

- `09-system-landscape.md`

### 2.2 Core Domain Model

必须说明核心对象：

- merchant
- store
- table
- table session
- draft order
- submitted order
- settlement
- member
- staff
- sku
- promotion

建议材料：

- `03-design-architecture.md`
- `32-table-session-and-multi-order-model.md`

### 2.3 Terminology and State Model

必须说明：

- 统一术语
- 统一状态
- 状态流转规则
- 非法流转规则

建议材料：

- `06-terminology-and-state-model.md`

### 2.4 System Architecture Proposal

必须说明：

- domain boundary
- source of truth
- table-session-centered transaction model
- facade/BFF strategy
- monitoring and consistency layer
- AI-ready cross-cutting design

建议材料：

- `15-service-oriented-architecture-principle.md`
- `16-backend-domain-breakdown.md`
- `19-v2-backend-module-package-structure.md`
- `35-system-architecture-improvements-proposal.md`

---

## 3. Contract Package

### 3.1 API Contract Baseline

必须给出：

- 核心 API 分组
- request / response
- error model
- auth assumption
- id rule

建议材料：

- `21-v2-api-contract-baseline.md`
- `24-v2-crm-api-contract.md`

### 3.2 Data Model / Database Baseline

必须给出：

- 核心表
- 核心字段
- 表关系
- 唯一约束
- 快照字段
- migration strategy

建议材料：

- `20-v2-database-migration-baseline.md`

### 3.3 Event and Audit Expectations

建议说明：

- 关键业务事件
- 操作审计要求
- AI / human action source tracking

建议材料：

- `22-ai-ready-product-and-api-design-principle.md`
- `35-system-architecture-improvements-proposal.md`

---

## 4. Design Package

### 4.1 Information Architecture

必须给出：

- 端级菜单结构
- 页面清单
- 一级/二级模块

建议材料：

- `12-platform-admin-information-architecture.md`
- Merchant Admin / POS 对应 IA 文档

### 4.2 UI Design Requirements

必须给出：

- 各端 UI 原则
- 关键页面信息结构
- 交互规则

建议材料：

- `04-ui-design-requirements.md`
- `13-platform-admin-ui-design-requirements.md`

### 4.3 Wireframe Requirements

建议给出：

- 页面结构块
- 重点组件
- 关键动作和反馈

建议材料：

- `14-platform-admin-wireframe-requirements.md`

---

## 5. Acceptance and Test Package

### 5.1 Acceptance Document

必须给出：

- 什么叫完成
- 什么叫通过
- 哪些能力属于当前阶段
- 哪些不作为 blocker

建议材料：

- `26-v2-acceptance-document.md`

### 5.2 Feature Completion Checklist

必须给出：

- 每个功能完成后怎么检查
- build / data / API / regression 怎么检查

建议材料：

- `27-feature-completion-checklist.md`

### 5.3 User Journeys and Test Runsheet

必须给出：

- 角色
- 入口
- 步骤
- 预期结果

建议材料：

- `33-user-journeys-and-test-runsheet.md`

---

## 6. Delivery Package

### 6.1 Technical Stack and Environment

必须说明：

- 技术栈
- 本地环境
- 运行方式
- build 方式
- 部署方式

建议材料：

- `08-technical-stack-and-selection-rationale.md`

### 6.2 Roadmap and Milestones

必须说明：

- 先做什么
- 后做什么
- 哪些依赖哪些
- 每阶段交付物是什么

建议材料：

- `05-roadmap.md`
- `32-current-progress-snapshot.md`

### 6.3 Engineering Rules

建议明确：

- 分支策略
- commit 规则
- review 规则
- build 规则
- smoke test 规则
- DoD

建议材料：

- `07-product-development-methodology.md`
- `27-feature-completion-checklist.md`

---

## Minimum Required Set

如果时间有限，外部团队开工前至少必须拿到这 10 份：

1. Overview
2. MRD
3. System Landscape
4. Core Domain / Architecture
5. Terminology and State Model
6. API Contract Baseline
7. Database Baseline
8. UI Design Requirements
9. Acceptance Document
10. User Journeys and Test Runsheet

没有这 10 份，项目大概率会在以下地方反复返工：

- 订单模型
- 状态语义
- 四端边界
- API 设计
- 验收标准

---

## Strongly Recommended Set

如果想让外部团队真正高效，建议再补上：

11. Technical Stack and Selection Rationale
12. Roadmap
13. Feature Completion Checklist
14. Current Progress Snapshot
15. System Architecture Improvements Proposal

---

## Not Enough by Themselves

以下材料单独存在都不够：

### Only PRD

问题：

- 无法指导状态机
- 无法指导 API
- 无法指导数据模型

### Only UI Design

问题：

- 页面能画出来
- 但交易和数据模型会错

### Only Backend Spec

问题：

- 团队不知道产品边界
- 也不知道四端怎么分

---

## Recommended Handoff Sequence

建议外部团队 onboarding 按以下顺序进行：

1. Overview
2. MRD
3. System Landscape
4. Core Domain + Terminology + State Model
5. User Journeys
6. Acceptance Document
7. API + Database Baseline
8. UI Requirements / Wireframes
9. Roadmap
10. Engineering Rules

---

## Final Recommendation

如果由外部团队从头开发：

**不要把“开工包”理解成几份参考文档。**

而应该把它理解成一套完整的：

- 产品定义
- 架构基线
- 技术契约
- 设计基线
- 测试基线
- 交付规则

只有这样，外部团队才能真正从头高效启动，而不是一边做一边重新定义系统。
