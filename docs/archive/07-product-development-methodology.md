# Product Development Methodology for Restaurant POS

## 1. Goal

本方法论用于指导 Restaurant POS 后续产品开发、设计决策和工程推进，确保团队在新增需求和复杂交易场景下仍然能够：

- 从真实客户场景出发
- 保持核心模型稳定
- 让需求可追踪、可验证、可交付

它不是一套空泛流程，而是针对当前项目实际复杂度形成的工作方法。

## 2. Core Principle

开发 Restaurant POS 时，不应采用“想到一个功能就加一个页面”的方式，而应采用：

**客户场景 -> 核心对象 -> 状态模型 -> 交互流程 -> 页面/API/数据 -> 测试闭环**

所有新增需求都应该回到这条主线判断。

## 3. Eight Working Principles

### 3.1 Define Customer Before Feature

先定义客户，再定义功能。

本项目当前主要客户与使用者包括：

- 平台管理员
- 商户老板
- 财务
- 店长
- IT / 运维
- cashier
- 顾客

如果一个需求无法明确属于哪个角色，通常说明需求定义还不够清晰。

### 3.2 Define Scenario Before Screen

先定义场景，再定义页面。

例如：

- 顾客扫码点餐
- cashier 代客点餐
- 一桌一单合并
- 下单到厨房
- 待结账
- cashier 收款
- 日结与 GTO 同步

页面只是承载场景的表现层，不是需求本身。

### 3.3 Define Core Objects Before Modules

先定义核心对象，再拆模块。

当前系统围绕 6 类核心对象：

- Merchant
- Store
- Order
- Member
- Staff
- SKU

其中：

- Order 是交易中枢
- SKU 是商品中枢

模块设计必须围绕这些对象展开，而不是围绕零散页面展开。

### 3.4 Define State Machine Before Action Buttons

先定义状态流，再定义按钮和页面动作。

例如当前主订单状态：

- `DRAFT`
- `SUBMITTED`
- `PENDING_SETTLEMENT`
- `SETTLED`

只有状态机清晰，UI 上的按钮才有稳定语义。

### 3.5 Every Requirement Must Map to Deliverables

每条需求都要能落到具体交付物：

- 文档
- 页面
- 接口
- 数据表
- 测试用例

如果一条需求无法映射到交付物，它通常还不够成熟。

### 3.6 Stabilize Specifications Early

早期必须尽量稳定以下关键规格：

- 订单模型
- 桌台模型
- SKU 模型
- 会员结算规则
- 促销执行顺序
- GTO 输出口径

这些一旦反复漂移，后面产品、设计、开发、测试都会持续返工。

### 3.7 Product, Design, Engineering Move Together

市场、产品、设计、工程要并行协同，而不是串行交接。

我们在本项目里需要同时关注：

- 市场是否真实需要
- 产品是否闭环
- 设计是否顺手
- 工程是否可落地
- 数据是否可对账

### 3.8 Every Phase Needs Verifiable Outcomes

每个阶段都必须有可验证结果，而不是只有描述。

例如：

- 顾客扫码能否提交到当前桌活动订单
- POS 能否看到并改单
- 改单刷新后是否保持
- 结账后是否清桌
- 后台是否能同步看到订单

## 4. How This Applies to Restaurant POS

### 4.1 We Build Around Unified Table Orders

不是把 QR 点单和 POS 点单当成两套订单体系，而是：

- 一张桌只有一张活动订单
- QR 和 POS 都是在编辑这张活动订单
- cashier 负责最终结账

### 4.2 We Separate Ordering From Settlement

顾客扫码点餐不等于付款。

- 点餐是订单编辑
- cashier 收款是结账
- 报表与 GTO 基于结账结果沉淀

### 4.3 We Treat CRM and Promotions as First-Class Domains

会员和促销不能作为“订单上的附加字段”草率处理，而必须：

- 有独立模型
- 有独立规则
- 有快照
- 有报表口径

### 4.4 We Build with Auditability in Mind

所有关键交易动作最终都要支持：

- 查询
- 对账
- 报表
- GTO 输出
- 运营追溯

## 5. Required Artifacts for Each Major Capability

每个大功能上线前，至少应该具备这些内容：

### 5.1 Market / Product

- 是否进入当前版本
- 目标客户是谁
- 解决什么问题
- 关键使用场景是什么

### 5.2 Design

- 页面目标
- 信息结构
- 状态表现
- 核心操作按钮
- 异常反馈

### 5.3 Engineering

- 核心对象
- 状态流
- 接口
- 数据存储
- 日志与追踪

### 5.4 QA / Validation

- 主流程用例
- 状态切换用例
- 刷新 / 重试 / 并发场景
- 报表与同步口径验证

## 6. Decision Standard

后续每次遇到新需求，都建议按这 5 个问题判断：

1. 这是哪个客户角色的需求？
2. 它属于哪个核心场景？
3. 它影响哪个核心对象？
4. 它会改变哪个状态流？
5. 它是否能落到页面、接口、数据和测试？

如果这 5 个问题答不清，就不应该直接进入开发。

## 7. Working Formula

Restaurant POS 的推荐开发公式是：

**市场需求 -> 产品边界 -> 核心对象 -> 状态模型 -> 页面交互 -> 技术实现 -> 测试闭环**

这是当前项目后续继续扩展时必须遵循的统一方法基线。
