# Market Requirements Document V2 — AI-Driven Restaurant POS

## 1. Document Purpose

本 MRD 定义 FounderPOS 的市场定位、核心价值主张和产品方向。FounderPOS 不是传统 POS，而是一个 AI 驱动的餐厅操作系统，让一个老板用极少的人力驱动一家餐厅。

## 2. Market Background

### 2.1 行业痛点

中小餐饮的核心矛盾：**经营复杂度持续上升，但人力成本和管理能力跟不上。**

| 痛点 | 现状 |
|------|------|
| 运营依赖人 | 菜单调整、促销策划、会员管理全靠老板或运营经理，一旦人走就断 |
| 数据看不懂 | 有报表但不知道该做什么，数据到决策的链路断裂 |
| 多平台碎片化 | POS、外卖、会员、报表各一套系统，数据不通 |
| 人力成本高 | 需要收银员、运营经理、财务、店长，小店养不起 |

### 2.2 市场机会

AI 技术成熟到可以替代"运营经理"角色：分析数据、生成方案、半自动执行。但目前没有一个 POS 系统真正做到 AI 原生。

## 3. Target Customer

### 3.1 Primary — 一人管店的餐饮老板

- 自己开店、自己管、没有运营团队
- 会用手机但不会看报表
- 希望有人帮他做决策，他只管审批
- 典型画像：购物中心里的中小餐厅、茶饮店、咖啡馆

### 3.2 Secondary — 小型连锁（3-10 家店）

- 一个老板管多家店
- 需要统一看各店数据
- 需要标准化运营但没有总部运营团队

## 4. Core Value Proposition

> **FounderPOS = 给每家餐厅配一个不休息的 AI 运营总监**

| 传统 POS | FounderPOS |
|---------|------------|
| 老板用软件管餐厅 | 老板有一个 AI 合伙人帮他管餐厅 |
| 软件是工具 | AI 是主动的运营者 |
| 老板看报表自己想 | AI 分析数据、生成方案、老板审批 |
| 每家店独立 | 餐厅有自己的 Agent，能对外交互 |

## 5. Product Capability Map

### Layer 1 — Transaction Foundation（传统 POS 能力）
- 堂食点餐（POS + QR 扫码）
- 商品/SKU/分类管理
- 桌台管理（一桌一单）
- 多支付方式（现金/刷卡/QR 支付）
- 会员（充值/积分/等级/自动升级）
- 促销引擎（满减/百分比折扣/赠品）
- 结账 + 退款
- 员工/角色/权限 + Cashier 班次
- GTO 税务导出
- 报表（日报/销售/会员）

### Layer 2 — MCP Tool Server（AI 操作接口）
- 每个域暴露为 MCP Tools
- AI Agent 通过标准接口操作所有模块
- ActionContext 审计链路（谁操作/为什么/来源是 AI 还是人）

### Layer 3 — AI Operator（AI 运营大脑）
- 5 个顾问角色：菜单/营销/会员/经营/出品
- 模式：Sense → Think → Propose → Approve → Act
- 触发：定时巡检 + 事件驱动 + 老板提问

### Layer 4 — Agent + Wallet（餐厅 AI 身份）
- 每家餐厅 = 1 Agent + 1 Wallet
- 对外：接受预定、包场询价、供应商报价
- 对内：收款、分账、预算控制

### Layer 5 — Restaurant Network（网络效应）
- 匿名经营数据共享（同区域/同菜系基准线）
- Agent 间协作（联合采购、互相导流）

## 6. Competitive Differentiation

| 维度 | 传统 POS (客如云/美味不用等) | SaaS (二维火/收钱吧) | FounderPOS |
|------|--------------------------|-------------------|-----------|
| 核心能力 | 收银 + 点餐 | 收银 + 简单会员 | AI 驱动全链路运营 |
| AI 能力 | 无 | 无或简单报表 | 原生 AI Operator |
| 对外交互 | 无 | 无 | Agent Protocol |
| 一人管多店 | 需要运营团队 | 需要运营团队 | AI 替代运营团队 |
| 数据智能 | 看报表 | 看报表 | AI 分析 + 生成方案 + 执行 |

## 7. Business Model

| 层级 | 收费 |
|------|------|
| Layer 1 传统 POS | 免费 / 低价（获客） |
| Layer 2-3 AI Operator | 月费订阅（核心收入） |
| Layer 4-5 Agent + Network | 交易抽成 / 增值服务 |

## 8. Phased Delivery

| 阶段 | 目标 | 时间线 |
|------|------|--------|
| V2.0 | 传统 POS 功能完整可用 | 当前 |
| V2.1 | MCP Tool Server + AI Operator MVP | 下一步 |
| V3.0 | Agent + Wallet + 对外交互 | 后续 |
| V4.0 | Restaurant Network + 匿名数据 | 远期 |

## 9. Success Metrics

- **V2.0**: 一家真实餐厅跑通全链路（点餐→结账→报表→会员）
- **V2.1**: 老板每天收到 AI 经营摘要，至少采纳 1 条建议
- **V3.0**: 餐厅 Agent 自动处理 50%+ 的预定/询价请求
- **V4.0**: 10+ 家餐厅组成网络，共享匿名经营洞察
