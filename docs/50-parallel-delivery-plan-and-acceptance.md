# Parallel Delivery Plan and Acceptance

## Purpose

本文档定义当前这一轮并行交付计划，覆盖以下 5 个工作流：

1. Reservations + Transfer Table backend formalization
2. CRM completion pass
3. Platform Admin real-data pass
4. Shift / handover foundation
5. Reports deepening pass

目标不是“大家同时忙”，而是：

- 每块都有明确边界
- 每块都有清楚的完成标准
- 每块都能独立 review 和合并

---

## Execution Rule

本轮统一执行规则：

- 每个工作流必须独立分支开发
- 每个工作流必须有明确写入范围
- 不允许只交分析，不交代码
- 每个工作流必须至少跑一轮 build / check
- 所有结果先 review，再决定 merge

---

## Workstream A: Reservations + Transfer Table

### Scope

- Reservation list
- Reservation create
- Reservation update
- Seat / check-in
- Table assignment
- Transfer table API

### Write Scope

- `pos-backend`
- `android-preview-web` 仅在接口接入阶段适配

### Acceptance

- 有 V2 reservation API：
  - list
  - create
  - update
  - seat/check-in
- Reservation 可把客人分配到桌台
- Table transfer 不再只是前端本地迁移
- Transfer 后 source / destination table 状态一致
- backend build 通过

### Out of Scope

- 高级 waitlist 策略
- 预订短信通知
- 多门店跨店预订

---

## Workstream B: CRM Completion Pass

### Scope

- Member detail
- Member edit
- Member active/inactive state handling
- Tier rules or one equivalent high-value CRM completion item

### Write Scope

- `pc-admin`
- `pos-backend` member / crm related code

### Acceptance

- CRM 不只支持 create/list，还至少补齐一个完整闭环：
  - detail + edit
  - 或 detail + status management
  - 或 tier rules 后端化
- `pc-admin` build 通过
- `pos-backend` build 通过
- 风险和剩余缺口写清楚

### Out of Scope

- AI CRM recommendations
- 完整会员分群系统

---

## Workstream C: Platform Admin Real Data

### Scope

- Platform admin 从 skeleton 接入真实数据
- 优先 merchant / store / device / config 中最值得先落的一块

### Write Scope

- `platform-admin`
- 必要时少量 `pos-backend` 平台读接口

### Acceptance

- 至少一个平台后台主页面开始读真实数据
- 不再只是静态 skeleton
- `platform-admin` build 通过
- 若有后端接口改动，backend build 通过

### Out of Scope

- 完整平台监管中心
- 完整支持工单体系

---

## Workstream D: Shift / Handover Foundation

### Scope

- Open shift
- Close shift
- Shift ownership baseline
- Shift summary baseline

### Write Scope

- `pos-backend`
- 需要时少量 `pc-admin` 或 preview 接入

### Acceptance

- 后端存在最小 shift 数据结构与 API
- 可以 open / close shift
- 可以读取一个最小 shift summary
- build 通过

### Out of Scope

- 完整审计与现金抽屉管理
- 高级班次异常处理

---

## Workstream E: Reports Deepening Pass

### Scope

- 在现有 reports foundation 上补一块更像经营报表的能力

优先方向：

- member consumption
- recharge summary
- table performance
- discount breakdown

### Write Scope

- `pos-backend`
- `pc-admin`

### Acceptance

- 至少一块经营报表从基础汇总提升为真实运营指标
- 商户后台可读到真实数据
- `pc-admin` build 通过
- backend build 通过

### Out of Scope

- 全量 BI
- 多维钻取分析平台

---

## Review Checklist

每个工作流交付时必须回答：

1. 分支名是什么
2. 改了哪些文件
3. 做成了什么
4. 跑了什么构建或检查
5. 最大剩余风险是什么

如果不能回答这 5 个问题，则不视为可 merge 结果。

---

## Merge Strategy

推荐 merge 顺序：

1. CRM
2. Platform Admin
3. Reports
4. Shift
5. Reservations + Transfer Table

原因：

- CRM / Platform Admin / Reports 相对更独立
- Shift 和 Reservations / Transfer Table 更容易碰交易主线
- 把耦合度高的合并放后面更稳

