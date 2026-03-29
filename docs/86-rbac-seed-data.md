# FounderPOS V3 — RBAC 种子数据

**Version:** V20260328024
**Date:** 2026-03-28
**Status:** DRAFT
**Layer:** 1（实现基线）
**用途：** Session 1.1 RBAC 实现时的预置数据

---

## 1. 权限定义 (permissions)

### 1.1 桌台与订单

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| TABLE_VIEW | 查看桌台 | TABLE | LOW |
| TABLE_MANAGE | 桌台管理（开台/状态） | TABLE | LOW |
| TABLE_MERGE | 并台/拆台 | TABLE | MEDIUM |
| TABLE_CLEAN | 清台 | TABLE | LOW |
| ORDER_CREATE | 创建订单 | ORDER | LOW |
| ORDER_CANCEL | 取消订单 | ORDER | MEDIUM |
| ORDER_VIEW | 查看订单 | ORDER | LOW |

### 1.2 结算与退款

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| SETTLEMENT_COLLECT | 结账收款 | SETTLEMENT | LOW |
| SETTLEMENT_STACKING | 支付叠加 | SETTLEMENT | MEDIUM |
| REFUND_SMALL | 小额退款（≤ 阈值） | SETTLEMENT | MEDIUM |
| REFUND_LARGE | 大额退款（> 阈值，需审批） | SETTLEMENT | HIGH |
| PAYMENT_SWITCH | 换支付方式 | SETTLEMENT | LOW |

### 1.3 班次

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| SHIFT_OPEN_CLOSE | 开关班 | SHIFT | LOW |
| SHIFT_VIEW | 查看班次 | SHIFT | LOW |
| SHIFT_VIEW_ALL | 查看所有班次 | SHIFT | LOW |

### 1.4 菜单与商品

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| MENU_VIEW | 查看菜单 | CATALOG | LOW |
| MENU_MANAGE | 菜单管理（CRUD 商品/SKU/修饰符） | CATALOG | MEDIUM |
| PRICE_CHANGE | 修改价格 | CATALOG | HIGH |
| BUFFET_MANAGE | 自助餐档位管理 | CATALOG | MEDIUM |
| BUFFET_START | 开始自助餐 | CATALOG | LOW |

### 1.5 厨房

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| KDS_OPERATE | 厨房票操作 | KITCHEN | LOW |
| KDS_MANAGE | 工作站管理 | KITCHEN | MEDIUM |
| TICKET_CANCEL | 退单/取消票 | KITCHEN | MEDIUM |

### 1.6 库存

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| INVENTORY_VIEW | 查看库存 | INVENTORY | LOW |
| INVENTORY_MANAGE | 库存管理（入库/调拨/报损） | INVENTORY | MEDIUM |
| STOCKTAKE_CREATE | 创建盘点 | INVENTORY | MEDIUM |
| STOCKTAKE_APPROVE | 审批盘点 | INVENTORY | HIGH |
| SOP_MANAGE | SOP 配方管理 | INVENTORY | MEDIUM |
| PURCHASE_CREATE | 创建采购单 | INVENTORY | MEDIUM |
| PURCHASE_APPROVE | 审批采购单 | INVENTORY | HIGH |

### 1.7 会员与营销

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| MEMBER_VIEW | 查看会员 | MEMBER | LOW |
| MEMBER_MANAGE | 会员管理 | MEMBER | LOW |
| MEMBER_RECHARGE | 会员充值 | MEMBER | MEDIUM |
| COUPON_MANAGE | 优惠券管理 | MEMBER | MEDIUM |
| PROMOTION_MANAGE | 促销管理 | PROMOTION | MEDIUM |
| CAMPAIGN_MANAGE | 营销活动管理 | MARKETING | MEDIUM |
| CHANNEL_MANAGE | 渠道管理 | CHANNEL | MEDIUM |
| CHANNEL_SETTLEMENT | 渠道结算 | CHANNEL | HIGH |

### 1.8 预约与候位

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| RESERVATION_VIEW | 查看预约 | RESERVATION | LOW |
| RESERVATION_MANAGE | 预约管理 | RESERVATION | LOW |
| QUEUE_MANAGE | 候位管理（叫号/入座） | RESERVATION | LOW |

### 1.9 员工与运营

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| EMPLOYEE_VIEW | 查看员工 | EMPLOYEE | LOW |
| EMPLOYEE_MANAGE | 员工管理 | EMPLOYEE | MEDIUM |
| SCHEDULE_MANAGE | 排班管理 | EMPLOYEE | MEDIUM |
| PAYROLL_VIEW | 查看薪资 | EMPLOYEE | MEDIUM |
| PAYROLL_MANAGE | 薪资管理 | EMPLOYEE | HIGH |
| LEAVE_APPROVE | 请假审批 | EMPLOYEE | LOW |

### 1.10 报表与审计

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| REPORT_VIEW | 查看本店报表 | REPORT | LOW |
| REPORT_VIEW_ALL | 查看所有门店报表 | REPORT | LOW |
| AUDIT_VIEW | 查看审计日志 | AUDIT | LOW |
| AUDIT_APPROVE | 审批审计项 | AUDIT | HIGH |
| GTO_EXPORT | GTO 税务导出 | AUDIT | MEDIUM |

### 1.11 系统管理

| permission_code | permission_name | permission_group | risk_level |
|----------------|-----------------|-----------------|-----------|
| STORE_MANAGE | 门店管理 | SYSTEM | HIGH |
| ROLE_MANAGE | 角色管理 | SYSTEM | HIGH |
| USER_MANAGE | 用户管理 | SYSTEM | HIGH |
| INTEGRATION_VIEW | 查看外部对接日志 | SYSTEM | LOW |
| INTEGRATION_MANAGE | 外部对接配置 | SYSTEM | HIGH |
| AI_RECOMMENDATION_VIEW | 查看 AI 建议 | AI | LOW |
| AI_RECOMMENDATION_APPROVE | 审批 AI 建议 | AI | MEDIUM |

> **共 52 个权限**

---

## 2. 预置角色 (custom_roles)

### 2.1 系统角色（is_system=true, is_editable=false）

| role_code | role_name | role_level | max_refund_cents | 说明 |
|-----------|-----------|-----------|-----------------|------|
| SUPER_ADMIN | 超级管理员 | PLATFORM | NULL (无限) | 平台级，所有权限 |
| MERCHANT_OWNER | 商户老板 | MERCHANT | NULL (无限) | 商户级，所有权限 |

### 2.2 预置角色（is_system=true, is_editable=true）

| role_code | role_name | role_level | max_refund_cents | 说明 |
|-----------|-----------|-----------|-----------------|------|
| STORE_MANAGER | 店长 | STORE | 50000 ($500) | 门店全权 |
| CASHIER | 收银员 | STORE | 5000 ($50) | 点单+结账+小额退款 |
| KITCHEN_STAFF | 厨房员工 | STORE | NULL | 只操作 KDS |
| WAITER | 服务员 | STORE | NULL | 桌台+点单+上菜 |
| INVENTORY_CLERK | 库存员 | STORE | NULL | 库存操作 |
| FINANCE | 财务 | MERCHANT | NULL | 报表+结算+税务 |

---

## 3. 角色 → 权限映射 (custom_role_permissions)

### STORE_MANAGER 店长 (38 权限)
```
TABLE_VIEW, TABLE_MANAGE, TABLE_MERGE, TABLE_CLEAN,
ORDER_CREATE, ORDER_CANCEL, ORDER_VIEW,
SETTLEMENT_COLLECT, SETTLEMENT_STACKING, REFUND_SMALL, REFUND_LARGE, PAYMENT_SWITCH,
SHIFT_OPEN_CLOSE, SHIFT_VIEW, SHIFT_VIEW_ALL,
MENU_VIEW, MENU_MANAGE, PRICE_CHANGE, BUFFET_MANAGE, BUFFET_START,
KDS_OPERATE, KDS_MANAGE, TICKET_CANCEL,
INVENTORY_VIEW, INVENTORY_MANAGE, STOCKTAKE_CREATE, STOCKTAKE_APPROVE, SOP_MANAGE,
PURCHASE_CREATE, PURCHASE_APPROVE,
MEMBER_VIEW, MEMBER_MANAGE, MEMBER_RECHARGE, COUPON_MANAGE, PROMOTION_MANAGE,
RESERVATION_VIEW, RESERVATION_MANAGE, QUEUE_MANAGE,
EMPLOYEE_VIEW, SCHEDULE_MANAGE, LEAVE_APPROVE,
REPORT_VIEW, AUDIT_VIEW, AUDIT_APPROVE,
AI_RECOMMENDATION_VIEW, AI_RECOMMENDATION_APPROVE
```

### CASHIER 收银员 (17 权限)
```
TABLE_VIEW, TABLE_MANAGE, TABLE_CLEAN,
ORDER_CREATE, ORDER_VIEW,
SETTLEMENT_COLLECT, SETTLEMENT_STACKING, REFUND_SMALL, PAYMENT_SWITCH,
SHIFT_OPEN_CLOSE, SHIFT_VIEW,
MENU_VIEW, BUFFET_START,
MEMBER_VIEW, MEMBER_MANAGE, MEMBER_RECHARGE,
RESERVATION_VIEW, QUEUE_MANAGE
```

### KITCHEN_STAFF 厨房员工 (3 权限)
```
KDS_OPERATE, TICKET_CANCEL, MENU_VIEW
```

### WAITER 服务员 (12 权限)
```
TABLE_VIEW, TABLE_MANAGE, TABLE_CLEAN,
ORDER_CREATE, ORDER_VIEW,
MENU_VIEW, BUFFET_START,
KDS_OPERATE,
RESERVATION_VIEW, QUEUE_MANAGE,
MEMBER_VIEW
```

### INVENTORY_CLERK 库存员 (8 权限)
```
INVENTORY_VIEW, INVENTORY_MANAGE,
STOCKTAKE_CREATE, SOP_MANAGE,
PURCHASE_CREATE,
MENU_VIEW,
REPORT_VIEW
```

### FINANCE 财务 (10 权限)
```
REPORT_VIEW, REPORT_VIEW_ALL,
AUDIT_VIEW, AUDIT_APPROVE,
GTO_EXPORT,
SHIFT_VIEW_ALL,
CHANNEL_SETTLEMENT,
MEMBER_VIEW,
PAYROLL_VIEW, PAYROLL_MANAGE
```

---

## 4. Flyway 种子数据 SQL

```sql
-- V102__seed_permissions.sql
-- 在 Session 1.1 生成，此处为参考模板

INSERT INTO permissions (permission_code, permission_name, permission_group, risk_level) VALUES
-- 桌台与订单
('TABLE_VIEW', '查看桌台', 'TABLE', 'LOW'),
('TABLE_MANAGE', '桌台管理', 'TABLE', 'LOW'),
('TABLE_MERGE', '并台/拆台', 'TABLE', 'MEDIUM'),
('TABLE_CLEAN', '清台', 'TABLE', 'LOW'),
('ORDER_CREATE', '创建订单', 'ORDER', 'LOW'),
('ORDER_CANCEL', '取消订单', 'ORDER', 'MEDIUM'),
('ORDER_VIEW', '查看订单', 'ORDER', 'LOW'),
-- 结算
('SETTLEMENT_COLLECT', '结账收款', 'SETTLEMENT', 'LOW'),
('SETTLEMENT_STACKING', '支付叠加', 'SETTLEMENT', 'MEDIUM'),
('REFUND_SMALL', '小额退款', 'SETTLEMENT', 'MEDIUM'),
('REFUND_LARGE', '大额退款', 'SETTLEMENT', 'HIGH'),
('PAYMENT_SWITCH', '换支付方式', 'SETTLEMENT', 'LOW'),
-- ... 其余 40 个按同样格式
;

-- 预置角色
INSERT INTO custom_roles (merchant_id, role_code, role_name, is_system, is_editable, role_level, max_refund_cents) VALUES
(NULL, 'SUPER_ADMIN', '超级管理员', TRUE, FALSE, 'PLATFORM', NULL),
(NULL, 'MERCHANT_OWNER', '商户老板', TRUE, FALSE, 'MERCHANT', NULL),
(NULL, 'STORE_MANAGER', '店长', TRUE, TRUE, 'STORE', 50000),
(NULL, 'CASHIER', '收银员', TRUE, TRUE, 'STORE', 5000),
(NULL, 'KITCHEN_STAFF', '厨房员工', TRUE, TRUE, 'STORE', NULL),
(NULL, 'WAITER', '服务员', TRUE, TRUE, 'STORE', NULL),
(NULL, 'INVENTORY_CLERK', '库存员', TRUE, TRUE, 'STORE', NULL),
(NULL, 'FINANCE', '财务', TRUE, TRUE, 'MERCHANT', NULL);

-- 角色权限绑定（STORE_MANAGER 示例）
INSERT INTO custom_role_permissions (role_id, permission_code)
SELECT r.id, p.permission_code
FROM custom_roles r, permissions p
WHERE r.role_code = 'STORE_MANAGER'
  AND p.permission_code IN ('TABLE_VIEW', 'TABLE_MANAGE', 'TABLE_MERGE', ...);
```

---

## 5. 退款阈值规则

| 角色 | max_refund_cents | 行为 |
|------|-----------------|------|
| CASHIER | 5000 ($50) | ≤ $50 直接退，> $50 → audit_trail(requires_approval=true) |
| STORE_MANAGER | 50000 ($500) | ≤ $500 直接退，> $500 → 需 MERCHANT_OWNER 审批 |
| MERCHANT_OWNER | NULL (无限) | 直接退，但仍记 audit_trail |

---

*52 个权限 + 8 个预置角色。Session 1.1 实现时按此生成种子数据。*
