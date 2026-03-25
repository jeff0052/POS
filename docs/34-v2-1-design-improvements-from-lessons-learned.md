# V2.1 Design Improvements from Lessons Learned

## Purpose

本文档记录当前 Restaurant POS 开发过程中暴露出的关键设计问题，并给出如果重新设计或进入下一轮架构升级时应优先采用的改进原则。

目标不是复盘抱怨，而是把已经付出的试错成本沉淀成下一轮更高效的设计基线。

---

## Core Conclusion

如果重做一轮，我们不应该再从“页面”和“功能列表”开始，而应该先锁定：

1. 核心对象
2. 状态机
3. 后端单一事实来源
4. 用户旅程
5. 验收与监测

---

## Lesson 1: Table Session Should Be a First-Class Model

### What happened

一开始用 `active order` 承载了过多职责：

- 草稿
- 已送厨
- QR 合单
- Payment 汇总

后面才逐步发现真实餐饮需要：

- 一张桌的营业会话
- 当前草稿
- 多个已送厨订单
- 最后统一结账

### Better design

从第一天就明确：

- `table_session`
- `draft_order`
- `submitted_order`
- `settlement`

### Why it matters

这样会天然支持：

- 多次送厨
- 已送厨不可直接编辑
- QR 直接形成 submitted order
- Payment 汇总多笔 submitted order

---

## Lesson 2: State Machine Must Be Designed Before UI Flow

### What happened

很多开发问题本质不是 UI 问题，而是状态没有一次设计清楚：

- 什么时候能 payment
- sent to kitchen 后还能不能加菜
- QR 提交后是 draft 还是 submitted
- 为什么刷新后状态会回弹

### Better design

在页面设计前先冻结这些状态机：

#### Table Session State

- `OPEN`
- `PAYMENT_PENDING`
- `SETTLED`
- `CLOSED`

#### Draft Order State

- `DRAFT`
- `DISCARDED`

#### Submitted Order State

- `SUBMITTED`
- `PREPARING`
- `READY`
- `SERVED`
- `SETTLED`
- `VOIDED`

### Why it matters

按钮、页面、接口、数据库都应该从状态机推导，而不是反过来。

---

## Lesson 3: Frontend Should Not Own Business Truth

### What happened

前端曾同时维护：

- 本地 draft
- 本地 submitted rounds
- 本地 stage
- 后端 current order

结果出现：

- 刷新回弹
- 状态不一致
- 页面看起来像成功，但数据库没同步

### Better design

从第一天规定：

- 前端只允许持有 UI 临时态
- 业务状态以后端为准
- 刷新必须完全从后端重建页面

### Rule

本地允许缓存：

- 搜索关键词
- 当前选中 tab
- 展开收起状态

本地不应长期持有：

- 桌台状态
- 订单状态
- 已送厨轮次
- payment 状态

---

## Lesson 4: Build the Smallest Real Backend Loop Before Expanding UI

### What happened

前期页面做得很快，但很多问题后来才发现是后端模型不完整。

### Better design

先完成一条最小真实后端链：

1. Open table session
2. Create draft
3. Send to kitchen
4. Move to payment
5. Collect payment
6. Release table

只有这条链真实跑通后，才继续扩页面和细节。

---

## Lesson 5: User Journeys Should Exist Before Feature Expansion

### What happened

没有固定 journey 时，测试会变成：

- 临时点击
- 临时发现问题
- 临时解释预期

### Better design

每个阶段都必须先有：

- user journeys
- smoke pack
- acceptance paths

### Rule

开发一个 Ordering / QR / Payment / CRM / Promotion 相关功能前，先明确它属于哪条 journey。

---

## Lesson 6: Terminology Must Freeze Earlier

### What happened

像这些词后期才慢慢对齐：

- `Pending settlement`
- `Payment Pending`
- `Open payment`
- `Collect payment`
- `active order`
- `table session`
- `submitted order`

### Better design

更早冻结：

- 业务术语
- 页面文案
- API 命名
- 状态字段名

### Why it matters

术语不稳定会造成：

- 页面改词
- API 改名
- 文档重写
- 团队理解混乱

---

## Lesson 7: Four-End Boundary Should Be Defined Before Feature Allocation

### What happened

有些能力先落在哪个端，后面才慢慢收清楚：

- 总后台
- 商户后台
- POS
- QR

### Better design

先明确四端边界：

#### Platform Admin

- merchant
- store
- device
- config

#### Merchant Admin

- orders
- members
- promotions
- reports

#### Store POS

- table
- ordering
- payment
- cashier operations

#### QR Ordering

- menu
- cart
- submit

### Rule

任何能力上线前，必须先明确属于哪个端，以及哪个端不应该出现它。

---

## Lesson 8: Monitoring and Consistency Checks Should Start Earlier

### What happened

很多问题都是通过人眼发现：

- table refresh 回弹
- order 状态不一致
- payment 后桌台没释放

### Better design

更早设计：

- 状态一致性检查
- 最小审计日志
- 关键动作 trace
- 异常检测清单

### Example checks

- `AVAILABLE` table should not have unpaid submitted orders
- `SETTLED` session should not still have current draft
- `PAYMENT_PENDING` should have at least one submitted order
- QR submit should not duplicate existing submitted items unintentionally

---

## Lesson 9: Production-Like Seed Data Helps Reveal Real Problems

### What happened

早期页面用大量展示型 seed/mock 数据，导致：

- 看起来很丰富
- 实际和真实状态混在一起
- 很难判断是 demo 还是 backend truth

### Better design

种子数据应该：

- 尽量最小
- 尽量可解释
- 尽量与真实数据结构一致

并且：

- demo seed 和 runtime truth 必须清楚分离

---

## Lesson 10: “Can Build” and “Can Operate” Must Be Treated Differently

### What happened

有些模块代码接上了，但还没做到：

- 真实编译验证
- 真实运行验证
- 真实 smoke test

### Better design

以后完成度要分层表达：

1. `Code integrated`
2. `Build verified`
3. `Runtime verified`
4. `Journey verified`

尤其是原生 Android，不能只因为代码写完就算完成。

---

## Recommended V2.1 Design Rules

下一轮设计建议直接遵守以下规则：

1. 所有交易能力围绕 `table_session + draft_order + submitted_order + settlement`
2. 所有页面先服从状态机，而不是先服从视觉流程
3. 所有业务状态以后端为准
4. 所有新功能必须先对应到 user journey
5. 所有模块完成后必须经历：
   - build check
   - smoke test
   - journey check
6. 四端边界先定，再分配页面和接口
7. 文案、字段、状态名尽量一次冻结
8. 监测和一致性检查不能放到最后

---

## Final Recommendation

如果后面继续进入 `V2.1` 或新一轮重构，最值得先做的不是多写几个页面，而是先补这 5 份基线：

1. Table Session and Submitted Order Final Model
2. State Machine Diagram
3. User Journeys and Test Runsheet
4. Order State Consistency Checklist
5. Four-End Capability Allocation Matrix

这 5 个东西先稳定，后面开发效率会明显高很多。
