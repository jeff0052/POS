# User Journeys and Test Runsheet

## Purpose

本文档定义 Restaurant POS 当前阶段的核心 `user journeys`，并将它们直接转成可执行测试跑表。

目标是：

- 不再靠临时点页面找问题
- 每次测试都按固定路径执行
- 每条路径都能映射到明确角色、明确入口、明确状态、明确预期结果
- 后续开发完成后，可以直接拿这份文档做回归测试

本文件当前主要覆盖：

- Store POS
- QR Ordering
- Merchant Admin
- Core payment flow

本文件当前暂不覆盖：

- Kitchen/KDS 详细履约
- Shift / handover
- Refund
- Delivery integration
- Platform Admin deep operations

---

## Scope Definition

当前测试范围的核心模型是：

- 一张桌对应一个 `Table Session`
- 一张桌可以存在：
  - 一个当前 `Draft`
  - 多个 `Submitted Orders`
- POS 可以维护 `Draft`
- QR 提交默认直接形成新的 `Submitted Order`
- `Payment` 汇总当前桌所有未结的 `Submitted Orders`
- `Draft` 不进入 `Payment`

---

## Test Environment Baseline

当前本地测试基线：

- POS preview:
  - `http://localhost:5185/`
- QR ordering:
  - `http://localhost:4179/`
- V2 backend:
  - `http://localhost:8090`
- Store code:
  - `1001`

建议优先使用的干净测试桌：

- `T10`
- `T15`
- `T17`

如遇历史脏数据，先不要用：

- `T2`
- `T3`

---

## User Roles

本轮测试涉及的角色：

1. `Cashier`
- 使用门店 POS
- 负责点菜、送厨、付款

2. `Customer`
- 使用 QR ordering
- 负责扫码点菜并提交

3. `Store Manager`
- 使用 Merchant Admin
- 负责查看订单、会员、促销和报表

4. `Merchant Operator`
- 使用 Merchant Admin
- 负责配置 promotion、CRM 基础信息

---

## Journey Groups

本轮 user journey 分成 7 组：

1. POS Basic Ordering
2. POS Multi-Round Ordering
3. QR Basic Ordering
4. POS + QR Mixed Ordering
5. Payment and Table Release
6. Member and Pricing
7. Merchant Admin Verification

---

## Group 1: POS Basic Ordering

### J1. Open an empty table and create a draft

**Actor**
- Cashier

**Entry**
- POS preview
- Table Management

**Steps**
1. Open an `AVAILABLE` table
2. Enter `Ordering`
3. Add one SKU

**Expected**
- The table becomes `ORDERING`
- A draft exists
- Refresh should keep the draft
- No submitted order is created yet

### J2. Send one POS draft to kitchen

**Actor**
- Cashier

**Precondition**
- Table has a draft with at least one item

**Steps**
1. In `Ordering`, tap `Send to kitchen`

**Expected**
- Current draft is cleared
- A new `Submitted Order` is created
- `Sent to kitchen` section shows one submitted round
- Table state becomes `DINING`
- Refresh should still show the submitted round

### J3. Draft should not enter payment before submission

**Actor**
- Cashier

**Precondition**
- Table has only draft items and no submitted order

**Steps**
1. Add draft items
2. Do not send to kitchen
3. Try to enter payment

**Expected**
- Payment should not be available
- Draft remains editable

---

## Group 2: POS Multi-Round Ordering

### J4. Send multiple POS rounds

**Actor**
- Cashier

**Precondition**
- Empty table

**Steps**
1. Add 2 items
2. Tap `Send to kitchen`
3. Add 1 more item
4. Tap `Send to kitchen` again

**Expected**
- Two separate submitted rounds exist
- First submitted round is not overwritten
- New add-on is stored as a second submitted round
- Table remains `DINING`

### J5. Submitted rounds are locked, new draft remains editable

**Actor**
- Cashier

**Precondition**
- Table already has at least one submitted round

**Steps**
1. Add a new draft item
2. Change draft quantity
3. Observe submitted rounds

**Expected**
- Draft items can still change
- Previously submitted rounds stay unchanged
- Submitted content does not move back into draft

### J6. Refresh after multiple rounds

**Actor**
- Cashier

**Precondition**
- Table has two submitted rounds and no draft

**Steps**
1. Refresh the page

**Expected**
- Both submitted rounds remain visible
- Table status remains `DINING`
- Total remains correct

---

## Group 3: QR Basic Ordering

### J7. Enter QR menu from table code

**Actor**
- Customer

**Entry**
- `http://localhost:4179/?storeName=Riverside%20Branch&storeCode=1001&table=T10`

**Steps**
1. Open QR page for a clean table

**Expected**
- QR context loads successfully
- Menu loads successfully
- Current table is shown correctly

### J8. Submit QR order directly to kitchen

**Actor**
- Customer

**Precondition**
- Empty table

**Steps**
1. Add 1 or more items
2. Submit order

**Expected**
- No local-only draft remains
- A new submitted order is created directly
- Table state becomes `DINING`
- POS preview can see the submitted round

### J9. Refresh QR page after submit

**Actor**
- Customer

**Precondition**
- QR order already submitted

**Steps**
1. Refresh QR page

**Expected**
- Submitted content remains visible through context
- No duplicate submission appears automatically

---

## Group 4: POS + QR Mixed Ordering

### J10. POS first, QR second

**Actor**
- Cashier + Customer

**Precondition**
- Clean table

**Steps**
1. POS adds items and sends to kitchen
2. Customer scans same table
3. Customer adds new items and submits

**Expected**
- QR submission creates another submitted order
- POS submitted round remains
- Both rounds are visible on the same table
- No overwrite happens

### J11. QR first, POS second

**Actor**
- Customer + Cashier

**Precondition**
- Clean table

**Steps**
1. Customer submits QR order
2. Cashier opens same table on POS
3. Cashier adds draft items
4. Cashier sends new round to kitchen

**Expected**
- QR submitted round remains
- POS creates another submitted round
- Both rounds are payable together

### J12. Refresh after mixed ordering

**Actor**
- Cashier

**Precondition**
- Same table contains both QR and POS rounds

**Steps**
1. Refresh POS preview

**Expected**
- Mixed submitted rounds still exist
- Table should not fall back to demo/default state
- Payment should still be available

---

## Group 5: Payment and Table Release

### J13. Move table to payment

**Actor**
- Cashier

**Precondition**
- Table has submitted rounds
- Table has no remaining draft

**Steps**
1. Enter `Payment`

**Expected**
- Table becomes `Payment Pending`
- Payment preview is loaded
- Collect button becomes available

### J14. Payment only includes submitted rounds

**Actor**
- Cashier

**Precondition**
- Table has submitted rounds
- Table also has a fresh draft

**Steps**
1. Open payment

**Expected**
- Draft items do not enter payment
- Only submitted rounds are counted

### J15. Collect payment and release table

**Actor**
- Cashier

**Precondition**
- Table is `Payment Pending`

**Steps**
1. Tap `Collect payment`
2. Return to `Table Management`
3. Refresh page

**Expected**
- Table returns to `AVAILABLE`
- `currentActiveOrder = null`
- `submittedOrders = []`
- Table does not bounce back after refresh

### J16. Payment done on QR-origin table

**Actor**
- Cashier

**Precondition**
- Table contains QR submitted order

**Steps**
1. Open payment
2. Collect payment

**Expected**
- QR-origin table can complete payment correctly
- Table returns to `AVAILABLE`
- QR context no longer shows active submitted orders

---

## Group 6: Member and Pricing

### J17. Create member and find by phone

**Actor**
- Cashier or Merchant Operator

**Entry**
- Merchant Admin or API-driven flow

**Steps**
1. Create a member
2. Search by phone

**Expected**
- Same member is returned
- Initial account exists

### J18. Bind member to active order

**Actor**
- Cashier

**Precondition**
- Table has draft or active order
- Member exists

**Steps**
1. Bind member to the order

**Expected**
- `memberId` is saved
- Base member discount applies
- Payment preview reflects member discount

### J19. Unbind member and rollback pricing

**Actor**
- Cashier

**Precondition**
- Order already has a bound member

**Steps**
1. Unbind member

**Expected**
- `memberId = null`
- Member discount returns to `0`
- Payable amount rolls back correctly

### J20. Promotion after member discount

**Actor**
- System evaluation

**Precondition**
- Order qualifies for member discount and promotion

**Steps**
1. Build order total over threshold
2. Bind member
3. Apply best promotion
4. Open payment preview

**Expected**
- Pricing order is:
  - original
  - member discount
  - promotion discount
  - payable
- Payment preview matches backend pricing

### J21. Gift promotion appears in preview

**Actor**
- Cashier

**Precondition**
- Order qualifies for `GIFT_SKU`

**Steps**
1. Build threshold order
2. Apply promotion
3. Open payment preview

**Expected**
- Gift item appears in preview
- Gift item does not distort payable amount

---

## Group 7: Merchant Admin Verification

### J22. Merchant admin sees POS/QR orders

**Actor**
- Store Manager

**Entry**
- Merchant Admin

**Steps**
1. Open order list
2. Filter or inspect recent orders

**Expected**
- POS and QR orders are both visible
- Table code, source, amount, and status are visible

### J23. Merchant admin sees payment breakdown

**Actor**
- Store Manager

**Steps**
1. Open order detail

**Expected**
- Original amount
- Member discount
- Promotion discount
- Payable
- Gift item when applicable

### J24. Merchant admin can manage promotion rules

**Actor**
- Merchant Operator

**Steps**
1. Create a promotion rule
2. Edit the rule
3. Re-read the rule

**Expected**
- Created rule persists
- Updated values persist
- Rule can later affect order pricing

### J25. Merchant admin can manage member value

**Actor**
- Merchant Operator

**Steps**
1. Recharge member
2. Adjust member points
3. Re-read member account

**Expected**
- Balance updates correctly
- Points update correctly
- Ledger records remain traceable

---

## Recommended Manual Test Sequence

如果要做一轮最高价值、最省时间的人工回归，建议按这个顺序跑：

1. `J1`
2. `J2`
3. `J4`
4. `J6`
5. `J8`
6. `J10`
7. `J13`
8. `J15`
9. `J18`
10. `J20`
11. `J22`
12. `J23`

这 12 条跑完，基本能覆盖当前最核心的经营主线。

---

## Fast Smoke Pack

如果只想做最小冒烟测试，跑这 6 条：

1. `J2`
2. `J4`
3. `J8`
4. `J10`
5. `J15`
6. `J23`

---

## Pass / Fail Recording Format

每次测试建议按下面格式记录：

- `Journey ID`
- `Tester`
- `Date`
- `Environment`
- `Result`
  - `PASS`
  - `FAIL`
  - `BLOCKED`
- `Observed behavior`
- `Expected behavior`
- `Notes`

示例：

- `Journey ID`: `J15`
- `Tester`: `Jeff`
- `Date`: `2026-03-25`
- `Environment`: `POS preview 5185 + V2 backend 8090`
- `Result`: `FAIL`
- `Observed behavior`: `T2 became available but returned to occupied after refresh`
- `Expected behavior`: `Table remains available after successful collect payment`
- `Notes`: `Possible frontend stale state issue`

---

## Current Priority Test Targets

当前最值得优先回归的路径：

1. `J4` POS multi-round ordering
2. `J10` POS first, QR second
3. `J11` QR first, POS second
4. `J15` payment and table release
5. `J16` payment on QR-origin table

因为这几条最容易暴露：

- 状态回弹
- 多轮覆盖
- POS/QR 冲突
- payment 汇总错误
- table release 失败

---

## Usage Rule

从现在开始：

- 开发一个 Ordering 相关功能
- 必须至少对照本文件中的相关 `journeys` 做一次检查
- 不再依赖“临时想到哪点到哪”

本文件将作为 Ordering / Payment / QR / Member / Promotion 当前阶段的默认回归基线。
