# UI Design Requirements

## 1. Document Purpose

本文件用于定义 Restaurant POS 一期的 UI 设计要求，确保：

- 产品视觉统一
- 桌台与订单逻辑表达清晰
- 顾客扫码页、POS、后台三端信息结构一致

## 2. Design Goal

本项目 UI 设计目标不是“做一个好看的消费类 App”，而是做一套：

- 可高频使用
- 高信息密度但清晰
- 餐饮业务导向
- 让 cashier、服务员、店长都能快速理解的界面系统

## 3. Visual Direction

### 3.1 Android POS

风格方向：

- 专业餐饮 POS
- 稳定、清晰、快速
- 桌台状态要一眼可扫
- 金额与关键状态层级清楚

### 3.2 QR Ordering Web

风格方向：

- 更接近中式扫码点餐 H5 / 小程序
- 操作直接
- 不做重营销首页
- 顾客扫码即进入点菜主流程

### 3.3 Merchant Admin

风格方向：

- 商业后台
- 强信息组织
- 强数据可读性

## 4. UX Principles

### 4.1 One Table, One Active Order

桌台页和订单页都必须强化这个认知：

- 一桌一单
- QR 与 POS 只是入口不同

### 4.2 Ordering and Settlement Are Different Tasks

UI 上必须明确区分：

- 点单
- 订单复核
- 结账

不能把这些任务混成一个页面动作。

### 4.3 Status Must Be Visible

必须清楚表达：

- 订单状态
- 桌台状态
- 会员身份
- 优惠命中
- 是否待结账

### 4.4 Do Not Hide Money Logic

所有结账页面必须明确展示：

- 原价
- 会员优惠
- 促销优惠
- 赠品
- 应付金额

## 5. Android POS UI Requirements

### 5.1 Table Management

必须包含：

- 桌台编号
- 桌台状态
- 当前金额
- QR / POS 来源标识
- 当前桌活动订单入口

不得出现：

- 桌台和订单信息脱节
- 桌台金额与订单内容不一致

### 5.2 Ordering

必须包含：

- 当前桌号
- 当前订单状态
- 类目
- 菜品区
- 当前购物车
- 会员信息
- 汇总区

### 5.3 Order Review

必须包含：

- 当前桌号
- 当前订单状态
- 当前菜品清单
- 订单金额拆解
- 明确操作按钮：
  - 继续点菜
  - 下单到厨房
  - 去结账

### 5.4 Payment / Cashier Settlement

必须包含：

- 桌号
- 订单来源
- 会员信息
- 优惠拆解
- 最终应收
- 收款方式
- 收款完成按钮

### 5.5 Payment Success

必须包含：

- 已结账确认
- 金额
- 后续动作
- 清桌/返回桌台

## 6. QR Ordering UI Requirements

扫码页必须：

- 直接进入当前桌菜单
- 弱化无关营销入口
- 强化分类与菜品操作
- 底部购物车固定
- 明确提示“提交给前台待结”

不要做成：

- 普通商城首页
- 多流程导流页

## 7. Merchant Admin UI Requirements

后台必须包含：

- 订单页
- CRM 页面
- 促销页面
- 报表页面
- GTO 同步页面

订单页必须明确显示：

- `POS / QR`
- 桌号
- 当前状态
- 会员
- 优惠
- 待结账标识

## 8. State Design Requirements

### 8.1 Table Status

建议视觉映射：

- `AVAILABLE`
- `ORDERING`
- `DINING`
- `PENDING_SETTLEMENT`
- `CLEANING`

### 8.2 Order Status

建议视觉映射：

- `DRAFT`
- `SUBMITTED`
- `PENDING_SETTLEMENT`
- `SETTLED`

### 8.3 Color Usage

颜色需要服务于状态，不为装饰而装饰。

建议：

- 可用：绿色
- 占用/进行中：深色或黑色
- 预订：紫色
- 待结：橙色
- 异常/催办：红色

## 9. Design Output Scope

一期设计至少应覆盖：

### Android POS

- Table Management
- Ordering
- Order Review
- Payment
- Payment Success

### QR Ordering Web

- 菜单列表
- 购物车
- 提交成功页

### Merchant Admin

- 订单列表
- CRM
- Promotions
- Reports
- GTO Sync

## 10. Design Handoff Requirements

设计稿输出时需明确：

- 页面用途
- 状态定义
- 关键按钮行为
- 组件复用关系
- 页面间跳转

并优先在 `android-preview-web` 中验证流程后，再同步到原生 Android。
