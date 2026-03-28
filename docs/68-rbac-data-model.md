# RBAC 权限数据模型 — 商户可自定义角色与权限

**Version:** V20260328004
**Date:** 2026-03-28
**Status:** DRAFT

---

## 1. 设计原则

1. **角色是模板** — 系统预设一批，商户可以自建、改名、调权限
2. **权限是积木** — 系统定义细粒度权限，角色是权限的组合
3. **门店是范围** — 同一个人、同一个角色，在不同门店可能有不同权限
4. **合并 auth_users 和 staff** — 一个人一个账号，POS 用 PIN 快速切换，后台用密码登录

---

## 2. 改造：统一用户表

### 当前问题

| 表 | 登录方式 | 用途 | 问题 |
|---|---------|------|------|
| auth_users | 用户名+密码 | 后台登录 | 没关联 staff |
| staff | 工号+PIN | POS 终端 | 没关联 auth_users |

一个店长既要在 POS 上用 PIN 开单，又要在后台用密码看报表。现在要建两个账号。

### 方案：合并为 `users`

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    phone VARCHAR(64) NULL,
    email VARCHAR(255) NULL,

    -- 后台登录凭证
    username VARCHAR(64) NULL,
    password_hash VARCHAR(255) NULL,
    must_change_password BOOLEAN DEFAULT TRUE,

    -- POS 终端凭证
    pin_hash VARCHAR(255) NULL,

    -- 状态
    user_status VARCHAR(32) DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP NULL,
    failed_login_count INT DEFAULT 0,
    locked_until TIMESTAMP NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_users_code UNIQUE (merchant_id, user_code),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**关键设计：**
- `username` + `password_hash` = 后台登录（可以为 NULL，不需要后台的人不设）
- `pin_hash` = POS 登录（可以为 NULL，不用 POS 的人不设）
- 一个人可以两种方式都有
- `failed_login_count` + `locked_until` = 防暴力破解

---

## 3. 权限积木表（系统预设，不可删改）

```sql
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(64) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    permission_group VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    risk_level VARCHAR(16) DEFAULT 'LOW',
    CONSTRAINT uk_perm_code UNIQUE (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 预设权限清单

| Group | Code | Name | Risk |
|-------|------|------|------|
| **订单** | ORDER_CREATE | 点单 | LOW |
| | ORDER_VIEW | 查看订单 | LOW |
| | ORDER_CANCEL | 取消订单 | MEDIUM |
| | ORDER_TRANSFER | 转台 | LOW |
| **结账** | SETTLEMENT_COLLECT | 结账收款 | MEDIUM |
| | SETTLEMENT_REFUND | 退款（小额≤设定值） | MEDIUM |
| | SETTLEMENT_REFUND_LARGE | 大额退款 | HIGH |
| | SETTLEMENT_VOID | 撤销结账 | HIGH |
| **报表** | REPORT_VIEW_STORE | 查看本店报表 | LOW |
| | REPORT_VIEW_ALL | 查看全部门店报表 | MEDIUM |
| | REPORT_FINANCE | 查看财务报表（含成本/毛利） | HIGH |
| | REPORT_EXPORT | 导出报表 | MEDIUM |
| **菜单** | CATALOG_VIEW | 查看菜单 | LOW |
| | CATALOG_EDIT | 编辑商品/SKU | MEDIUM |
| | CATALOG_PRICING | 修改价格 | HIGH |
| | CATALOG_PUBLISH | 上下架 | MEDIUM |
| **促销** | PROMOTION_VIEW | 查看促销 | LOW |
| | PROMOTION_MANAGE | 创建/编辑促销 | MEDIUM |
| | PROMOTION_APPROVE | 审批促销上线 | HIGH |
| **会员** | MEMBER_VIEW | 查看会员 | LOW |
| | MEMBER_MANAGE | 管理会员（注册/编辑） | LOW |
| | MEMBER_RECHARGE | 会员充值 | HIGH |
| | MEMBER_POINTS_ADJUST | 手动调整积分 | HIGH |
| **库存** | INVENTORY_VIEW | 查看库存 | LOW |
| | INVENTORY_MANAGE | 管理库存（入库/调整） | MEDIUM |
| | INVENTORY_PURCHASE | 审批采购/订货 | HIGH |
| **员工** | STAFF_VIEW | 查看员工列表 | LOW |
| | STAFF_MANAGE | 增删改员工 | HIGH |
| | STAFF_ROLE_ASSIGN | 分配角色 | HIGH |
| **班次** | SHIFT_OPEN_CLOSE | 开关班次 | MEDIUM |
| | SHIFT_VIEW_ALL | 查看所有班次 | LOW |
| **厨房** | KDS_VIEW | 查看厨房显示 | LOW |
| | KDS_OPERATE | 操作厨房状态 | LOW |
| **门店** | STORE_CONFIG | 门店配置（桌台/打印机/二维码） | MEDIUM |
| | STORE_DEVICE_MANAGE | 管理终端设备 | MEDIUM |
| **平台** | PLATFORM_MERCHANT_MANAGE | 管理商户 | HIGH |
| | PLATFORM_SYSTEM_CONFIG | 系统配置 | HIGH |
| | PLATFORM_AUDIT_LOG | 查看审计日志 | MEDIUM |
| **AI** | AI_VIEW_RECOMMENDATIONS | 查看 AI 建议 | LOW |
| | AI_APPROVE_RECOMMENDATIONS | 审批 AI 建议 | MEDIUM |
| | AI_MCP_EXECUTE | 执行 MCP 工具 | HIGH |

---

## 4. 角色表（系统预设 + 商户可自建）

```sql
CREATE TABLE custom_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    role_description VARCHAR(255) NULL,
    is_system BOOLEAN DEFAULT FALSE,
    is_editable BOOLEAN DEFAULT TRUE,
    role_level VARCHAR(32) NOT NULL DEFAULT 'STORE',
    max_refund_cents BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_role UNIQUE (merchant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**字段说明：**
- `merchant_id = NULL + is_system = TRUE` → 系统预设角色（所有商户可见）
- `merchant_id = 1 + is_system = FALSE` → 商户自建角色（只该商户可见）
- `is_editable = FALSE` → 系统预设角色不可删除（但可以复制后修改）
- `role_level`：`PLATFORM` | `MERCHANT` | `STORE` — 这个角色是平台级/商户级/门店级
- `max_refund_cents`：该角色可退款的最大金额（null = 不限 / 0 = 不可退）

### 系统预设角色

```sql
INSERT INTO custom_roles (merchant_id, role_code, role_name, is_system, is_editable, role_level) VALUES
(NULL, 'PLATFORM_ADMIN', '平台管理员', TRUE, FALSE, 'PLATFORM'),
(NULL, 'OWNER', '老板', TRUE, FALSE, 'MERCHANT'),
(NULL, 'SHAREHOLDER', '股东', TRUE, FALSE, 'MERCHANT'),
(NULL, 'FINANCE', '财务', TRUE, TRUE, 'MERCHANT'),
(NULL, 'MARKETING', '营销', TRUE, TRUE, 'MERCHANT'),
(NULL, 'STORE_MANAGER', '店长', TRUE, TRUE, 'STORE'),
(NULL, 'SHIFT_SUPERVISOR', '值班经理', TRUE, TRUE, 'STORE'),
(NULL, 'CASHIER', '收银员', TRUE, TRUE, 'STORE'),
(NULL, 'WAITER', '服务员', TRUE, TRUE, 'STORE'),
(NULL, 'KITCHEN', '厨房', TRUE, TRUE, 'STORE');
```

**商户自建示例：**

```sql
INSERT INTO custom_roles (merchant_id, role_code, role_name, is_system, is_editable, role_level, max_refund_cents) VALUES
(1, 'AREA_MANAGER', '区域经理', FALSE, TRUE, 'MERCHANT', 50000),
(1, 'SENIOR_CASHIER', '资深收银', FALSE, TRUE, 'STORE', 10000),
(1, 'INTERN', '实习生', FALSE, TRUE, 'STORE', 0);
```

---

## 5. 角色-权限关联（商户可调整）

```sql
CREATE TABLE custom_role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    CONSTRAINT fk_crp_role FOREIGN KEY (role_id) REFERENCES custom_roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_crp UNIQUE (role_id, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**系统预设角色的默认权限：**

| 角色 | 权限 |
|------|------|
| OWNER | ALL（全部权限） |
| SHAREHOLDER | REPORT_VIEW_ALL, REPORT_FINANCE |
| FINANCE | REPORT_*, SETTLEMENT_REFUND, SETTLEMENT_REFUND_LARGE, SETTLEMENT_VOID, MEMBER_RECHARGE |
| MARKETING | PROMOTION_*, MEMBER_*, CATALOG_VIEW, REPORT_VIEW_ALL |
| STORE_MANAGER | ORDER_*, SETTLEMENT_*, REPORT_VIEW_STORE, CATALOG_*, INVENTORY_*, STAFF_*, SHIFT_*, KDS_*, STORE_CONFIG |
| SHIFT_SUPERVISOR | ORDER_*, SETTLEMENT_COLLECT, SETTLEMENT_REFUND, SHIFT_OPEN_CLOSE, KDS_*, REPORT_VIEW_STORE |
| CASHIER | ORDER_CREATE, ORDER_VIEW, SETTLEMENT_COLLECT, MEMBER_VIEW |
| WAITER | ORDER_CREATE, ORDER_VIEW, KDS_VIEW |
| KITCHEN | KDS_VIEW, KDS_OPERATE |

**商户可以：**
- 给预设角色加/减权限（is_editable=TRUE 的角色）
- 创建新角色，自由组合权限积木
- 给自建角色设置退款上限

---

## 6. 用户-角色关联

```sql
CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT NULL,
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES custom_roles(id),
    CONSTRAINT uk_ur UNIQUE (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

一个人可以有多个角色。权限取并集。

---

## 7. 用户-门店访问范围

```sql
CREATE TABLE user_store_access (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    access_level VARCHAR(32) DEFAULT 'FULL',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT NULL,
    CONSTRAINT fk_usa_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_usa_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_usa UNIQUE (user_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**access_level：** `FULL`（完整权限）| `READ_ONLY`（只读）| `REPORT_ONLY`（只看报表）

**规则：**
- 商户级角色（OWNER/SHAREHOLDER/FINANCE/MARKETING）→ 默认所有门店，不需要在此表插入
- 门店级角色（STORE_MANAGER/CASHIER 等）→ 必须在此表指定哪些门店
- 区域经理（自建角色）→ 指定 3 家门店

**示例：**

```sql
-- 区域经理张三管 3 家店
INSERT INTO user_store_access (user_id, store_id, access_level) VALUES
(10, 101, 'FULL'),   -- Jewel 店
(10, 102, 'FULL'),   -- ION 店
(10, 105, 'FULL');   -- Vivo 店

-- 股东李四看所有店报表（不需要插入，SHAREHOLDER 是商户级，默认全店）

-- 实习生小王只在 Jewel 店
INSERT INTO user_store_access (user_id, store_id, access_level) VALUES
(20, 101, 'FULL');
```

---

## 8. 权限检查逻辑

### 后端（每个 API 请求）

```
1. 从 JWT 解出 user_id
2. 查 user_roles → 拿到所有 role_id
3. 查 custom_role_permissions → 拿到所有 permission_code（取并集）
4. 查 user_store_access → 拿到可访问的 store_id 列表
5. 检查：
   a. 当前操作需要的 permission_code 在不在用户权限里
   b. 当前操作的 store_id 在不在用户可访问范围里
   c. 如果是退款操作 → 额外检查 max_refund_cents
```

### 前端（菜单/按钮显示）

```
登录后拿到：
{
  permissions: ["ORDER_CREATE", "ORDER_VIEW", "SETTLEMENT_COLLECT"],
  accessibleStores: [101, 102],
  maxRefundCents: 10000
}

根据 permissions 控制：
- 没有 SETTLEMENT_REFUND → 隐藏退款按钮
- 没有 CATALOG_EDIT → 隐藏编辑菜单入口
- 没有 REPORT_FINANCE → 隐藏财务报表 tab
```

---

## 9. 商户自定义界面（后台）

商户后台需要一个"角色管理"页面：

```
角色管理页
├── 角色列表（系统预设 + 自建）
├── 新建角色（输入角色名 → 勾选权限积木）
├── 编辑角色（拖拽权限开关）
└── 删除角色（仅自建角色，且无人使用时可删）

权限配置页（编辑某个角色时）
├── 按分组展示权限（订单、结账、报表、菜单...）
├── 每个权限一个开关
├── HIGH risk 权限标红提示
└── 退款上限输入框
```

---

## 10. 旧表迁移方案

| 旧表 | 处理 |
|------|------|
| auth_users | 数据迁移到 users（username/password_hash 对应） |
| staff | 数据迁移到 users（staff_code→user_code, pin_hash 对应） |
| roles (旧) | 数据迁移到 custom_roles |
| role_permissions (旧) | 数据迁移到 custom_role_permissions |

迁移后旧表改名为 `_legacy_auth_users`、`_legacy_staff` 保留 30 天再删。

---

## 11. 新增表清单（本文档）

| 表 | 用途 |
|----|------|
| users | 统一用户表（合并 auth_users + staff） |
| permissions | 权限积木定义（系统预设） |
| custom_roles | 角色定义（系统预设 + 商户自建） |
| custom_role_permissions | 角色-权限关联 |
| user_roles | 用户-角色关联 |
| user_store_access | 用户-门店访问范围 |

**新增 6 张表，替代旧的 4 张表（auth_users, staff, roles, role_permissions）。**

**累计：74 (doc/66+67) + 6 - 4 (旧表废弃) = 76 张表**
