-- V102: RBAC seed data
-- Canonical seed set:
--   * 59 permissions
--   * 8 preset system roles
--   * role-permission mappings for preset roles
--
-- Re-run safety:
--   * permissions: update existing rows, insert missing rows
--   * custom_roles: update existing merchant_id IS NULL system roles by role_code,
--                   then insert only missing rows
--   * custom_role_permissions: insert missing mappings only

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_permissions;
CREATE TEMPORARY TABLE tmp_rbac_permissions (
    permission_code VARCHAR(64) NOT NULL PRIMARY KEY,
    permission_name VARCHAR(128) NOT NULL,
    permission_group VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    risk_level VARCHAR(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_rbac_permissions (
    permission_code,
    permission_name,
    permission_group,
    description,
    risk_level
) VALUES
    -- 1.1 桌台与订单 (7)
    ('TABLE_VIEW', '查看桌台', 'TABLE', '查看桌台列表和状态', 'LOW'),
    ('TABLE_MANAGE', '桌台管理', 'TABLE', '开台、关台、修改桌台状态', 'LOW'),
    ('TABLE_MERGE', '并台/拆台', 'TABLE', '合并或拆分桌台', 'MEDIUM'),
    ('TABLE_CLEAN', '清台', 'TABLE', '清理桌台并恢复可用状态', 'LOW'),
    ('ORDER_CREATE', '创建订单', 'ORDER', '创建和修改订单', 'LOW'),
    ('ORDER_CANCEL', '取消订单', 'ORDER', '取消整单', 'MEDIUM'),
    ('ORDER_VIEW', '查看订单', 'ORDER', '查看订单详情和历史', 'LOW'),

    -- 1.2 结算与退款 (5)
    ('SETTLEMENT_COLLECT', '结账收款', 'SETTLEMENT', '发起结账并收款', 'LOW'),
    ('SETTLEMENT_STACKING', '支付叠加', 'SETTLEMENT', '使用多种支付方式叠加结账', 'MEDIUM'),
    ('REFUND_SMALL', '小额退款', 'SETTLEMENT', '退款金额不超过角色阈值', 'MEDIUM'),
    ('REFUND_LARGE', '大额退款', 'SETTLEMENT', '大额退款且需要审批', 'HIGH'),
    ('PAYMENT_SWITCH', '换支付方式', 'SETTLEMENT', '结算中途切换支付方式', 'LOW'),

    -- 1.3 班次 (3)
    ('SHIFT_OPEN_CLOSE', '开关班', 'SHIFT', '开班和关班操作', 'LOW'),
    ('SHIFT_VIEW', '查看班次', 'SHIFT', '查看自己的班次', 'LOW'),
    ('SHIFT_VIEW_ALL', '查看所有班次', 'SHIFT', '查看所有员工班次', 'LOW'),

    -- 1.4 菜单与商品 (5)
    ('MENU_VIEW', '查看菜单', 'CATALOG', '查看商品和菜单', 'LOW'),
    ('MENU_MANAGE', '菜单管理', 'CATALOG', 'CRUD 商品、SKU、修饰符', 'MEDIUM'),
    ('PRICE_CHANGE', '修改价格', 'CATALOG', '修改 SKU 售价', 'HIGH'),
    ('BUFFET_MANAGE', '自助餐档位管理', 'CATALOG', '管理自助餐套餐和档位', 'MEDIUM'),
    ('BUFFET_START', '开始自助餐', 'CATALOG', '为桌台开始自助餐计时', 'LOW'),

    -- 1.5 厨房 (3)
    ('KDS_OPERATE', '厨房票操作', 'KITCHEN', '接单、完成、催单', 'LOW'),
    ('KDS_MANAGE', '工作站管理', 'KITCHEN', '管理厨房工作站配置', 'MEDIUM'),
    ('TICKET_CANCEL', '退单/取消票', 'KITCHEN', '取消厨房票', 'MEDIUM'),

    -- 1.6 库存 (7)
    ('INVENTORY_VIEW', '查看库存', 'INVENTORY', '查看库存列表和余量', 'LOW'),
    ('INVENTORY_MANAGE', '库存管理', 'INVENTORY', '入库、调拨、报损', 'MEDIUM'),
    ('STOCKTAKE_CREATE', '创建盘点', 'INVENTORY', '发起盘点任务', 'MEDIUM'),
    ('STOCKTAKE_APPROVE', '审批盘点', 'INVENTORY', '审批盘点差异', 'HIGH'),
    ('SOP_MANAGE', 'SOP 配方管理', 'INVENTORY', '管理配方和 BOM', 'MEDIUM'),
    ('PURCHASE_CREATE', '创建采购单', 'INVENTORY', '创建采购订单', 'MEDIUM'),
    ('PURCHASE_APPROVE', '审批采购单', 'INVENTORY', '审批采购订单', 'HIGH'),

    -- 1.7 会员与营销 (8)
    ('MEMBER_VIEW', '查看会员', 'MEMBER', '查看会员列表和详情', 'LOW'),
    ('MEMBER_MANAGE', '会员管理', 'MEMBER', '编辑会员信息和标签', 'LOW'),
    ('MEMBER_RECHARGE', '会员充值', 'MEMBER', '为会员充值储值账户', 'MEDIUM'),
    ('COUPON_MANAGE', '优惠券管理', 'MEMBER', '创建和管理优惠券模板', 'MEDIUM'),
    ('PROMOTION_MANAGE', '促销管理', 'PROMOTION', '管理促销规则', 'MEDIUM'),
    ('CAMPAIGN_MANAGE', '营销活动管理', 'MARKETING', '管理营销活动和触达', 'MEDIUM'),
    ('CHANNEL_MANAGE', '渠道管理', 'CHANNEL', '管理分销渠道', 'MEDIUM'),
    ('CHANNEL_SETTLEMENT', '渠道结算', 'CHANNEL', '执行渠道佣金结算', 'HIGH'),

    -- 1.8 预约与候位 (3)
    ('RESERVATION_VIEW', '查看预约', 'RESERVATION', '查看预约列表', 'LOW'),
    ('RESERVATION_MANAGE', '预约管理', 'RESERVATION', '创建、修改、取消预约', 'LOW'),
    ('QUEUE_MANAGE', '候位管理', 'RESERVATION', '叫号和安排入座', 'LOW'),

    -- 1.9 员工与运营 (6)
    ('EMPLOYEE_VIEW', '查看员工', 'EMPLOYEE', '查看员工列表', 'LOW'),
    ('EMPLOYEE_MANAGE', '员工管理', 'EMPLOYEE', '添加、编辑、停用员工', 'MEDIUM'),
    ('SCHEDULE_MANAGE', '排班管理', 'EMPLOYEE', '管理班次模板和排班', 'MEDIUM'),
    ('PAYROLL_VIEW', '查看薪资', 'EMPLOYEE', '查看薪资报表', 'MEDIUM'),
    ('PAYROLL_MANAGE', '薪资管理', 'EMPLOYEE', '管理薪资计算和发放', 'HIGH'),
    ('LEAVE_APPROVE', '请假审批', 'EMPLOYEE', '审批员工请假', 'LOW'),

    -- 1.10 报表与审计 (5)
    ('REPORT_VIEW', '查看本店报表', 'REPORT', '查看当前门店报表', 'LOW'),
    ('REPORT_VIEW_ALL', '查看所有门店报表', 'REPORT', '查看所有门店报表', 'LOW'),
    ('AUDIT_VIEW', '查看审计日志', 'AUDIT', '查看审计日志', 'LOW'),
    ('AUDIT_APPROVE', '审批审计项', 'AUDIT', '审批审计项', 'HIGH'),
    ('GTO_EXPORT', 'GTO 税务导出', 'AUDIT', '导出 GTO 税务数据', 'MEDIUM'),

    -- 1.11 系统管理 (7)
    ('STORE_MANAGE', '门店管理', 'SYSTEM', '创建和管理门店', 'HIGH'),
    ('ROLE_MANAGE', '角色管理', 'SYSTEM', '创建和管理角色权限', 'HIGH'),
    ('USER_MANAGE', '用户管理', 'SYSTEM', '创建和管理用户账号', 'HIGH'),
    ('INTEGRATION_VIEW', '查看外部对接日志', 'SYSTEM', '查看外部系统对接日志', 'LOW'),
    ('INTEGRATION_MANAGE', '外部对接配置', 'SYSTEM', '管理外部系统对接配置', 'HIGH'),
    ('AI_RECOMMENDATION_VIEW', '查看 AI 建议', 'AI', '查看 AI 推荐和建议', 'LOW'),
    ('AI_RECOMMENDATION_APPROVE', '审批 AI 建议', 'AI', '审批并执行 AI 建议', 'MEDIUM');

UPDATE permissions p
JOIN tmp_rbac_permissions s
  ON s.permission_code = p.permission_code
SET
    p.permission_name = s.permission_name,
    p.permission_group = s.permission_group,
    p.description = s.description,
    p.risk_level = s.risk_level;

INSERT INTO permissions (
    permission_code,
    permission_name,
    permission_group,
    description,
    risk_level
)
SELECT
    s.permission_code,
    s.permission_name,
    s.permission_group,
    s.description,
    s.risk_level
FROM tmp_rbac_permissions s
LEFT JOIN permissions p
  ON p.permission_code = s.permission_code
WHERE p.id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_roles;
CREATE TEMPORARY TABLE tmp_rbac_roles (
    role_code VARCHAR(64) NOT NULL PRIMARY KEY,
    role_name VARCHAR(128) NOT NULL,
    role_description VARCHAR(255) NULL,
    is_system BOOLEAN NOT NULL,
    is_editable BOOLEAN NOT NULL,
    role_level VARCHAR(32) NOT NULL,
    max_refund_cents BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_rbac_roles (
    role_code,
    role_name,
    role_description,
    is_system,
    is_editable,
    role_level,
    max_refund_cents
) VALUES
    ('SUPER_ADMIN', '超级管理员', '平台级管理员，拥有所有权限', TRUE, FALSE, 'PLATFORM', NULL),
    ('MERCHANT_OWNER', '商户老板', '商户级管理员，拥有商户内所有权限', TRUE, FALSE, 'MERCHANT', NULL),
    ('STORE_MANAGER', '店长', '门店全权管理', TRUE, TRUE, 'STORE', 50000),
    ('CASHIER', '收银员', '点单、结账、小额退款', TRUE, TRUE, 'STORE', 5000),
    ('KITCHEN_STAFF', '厨房员工', '厨房票操作', TRUE, TRUE, 'STORE', NULL),
    ('WAITER', '服务员', '桌台服务、点单、上菜', TRUE, TRUE, 'STORE', NULL),
    ('INVENTORY_CLERK', '库存员', '库存管理和盘点', TRUE, TRUE, 'STORE', NULL),
    ('FINANCE', '财务', '报表、结算、税务', TRUE, TRUE, 'MERCHANT', NULL);

UPDATE custom_roles r
JOIN tmp_rbac_roles s
  ON r.merchant_id IS NULL
 AND r.role_code = s.role_code
SET
    r.role_name = s.role_name,
    r.role_description = s.role_description,
    r.is_system = s.is_system,
    r.is_editable = s.is_editable,
    r.role_level = s.role_level,
    r.max_refund_cents = s.max_refund_cents;

INSERT INTO custom_roles (
    merchant_id,
    role_code,
    role_name,
    role_description,
    is_system,
    is_editable,
    role_level,
    max_refund_cents
)
SELECT
    NULL,
    s.role_code,
    s.role_name,
    s.role_description,
    s.is_system,
    s.is_editable,
    s.role_level,
    s.max_refund_cents
FROM tmp_rbac_roles s
LEFT JOIN custom_roles r
  ON r.merchant_id IS NULL
 AND r.role_code = s.role_code
WHERE r.id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_role_permissions;
CREATE TEMPORARY TABLE tmp_rbac_role_permissions (
    role_code VARCHAR(64) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_code, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SUPER_ADMIN + MERCHANT_OWNER: all 59 permissions
INSERT INTO tmp_rbac_role_permissions (role_code, permission_code)
SELECT 'SUPER_ADMIN', permission_code
FROM tmp_rbac_permissions;

INSERT INTO tmp_rbac_role_permissions (role_code, permission_code)
SELECT 'MERCHANT_OWNER', permission_code
FROM tmp_rbac_permissions;

-- STORE_MANAGER: 46 permissions
INSERT INTO tmp_rbac_role_permissions (role_code, permission_code) VALUES
    ('STORE_MANAGER', 'TABLE_VIEW'),
    ('STORE_MANAGER', 'TABLE_MANAGE'),
    ('STORE_MANAGER', 'TABLE_MERGE'),
    ('STORE_MANAGER', 'TABLE_CLEAN'),
    ('STORE_MANAGER', 'ORDER_CREATE'),
    ('STORE_MANAGER', 'ORDER_CANCEL'),
    ('STORE_MANAGER', 'ORDER_VIEW'),
    ('STORE_MANAGER', 'SETTLEMENT_COLLECT'),
    ('STORE_MANAGER', 'SETTLEMENT_STACKING'),
    ('STORE_MANAGER', 'REFUND_SMALL'),
    ('STORE_MANAGER', 'REFUND_LARGE'),
    ('STORE_MANAGER', 'PAYMENT_SWITCH'),
    ('STORE_MANAGER', 'SHIFT_OPEN_CLOSE'),
    ('STORE_MANAGER', 'SHIFT_VIEW'),
    ('STORE_MANAGER', 'SHIFT_VIEW_ALL'),
    ('STORE_MANAGER', 'MENU_VIEW'),
    ('STORE_MANAGER', 'MENU_MANAGE'),
    ('STORE_MANAGER', 'PRICE_CHANGE'),
    ('STORE_MANAGER', 'BUFFET_MANAGE'),
    ('STORE_MANAGER', 'BUFFET_START'),
    ('STORE_MANAGER', 'KDS_OPERATE'),
    ('STORE_MANAGER', 'KDS_MANAGE'),
    ('STORE_MANAGER', 'TICKET_CANCEL'),
    ('STORE_MANAGER', 'INVENTORY_VIEW'),
    ('STORE_MANAGER', 'INVENTORY_MANAGE'),
    ('STORE_MANAGER', 'STOCKTAKE_CREATE'),
    ('STORE_MANAGER', 'STOCKTAKE_APPROVE'),
    ('STORE_MANAGER', 'SOP_MANAGE'),
    ('STORE_MANAGER', 'PURCHASE_CREATE'),
    ('STORE_MANAGER', 'PURCHASE_APPROVE'),
    ('STORE_MANAGER', 'MEMBER_VIEW'),
    ('STORE_MANAGER', 'MEMBER_MANAGE'),
    ('STORE_MANAGER', 'MEMBER_RECHARGE'),
    ('STORE_MANAGER', 'COUPON_MANAGE'),
    ('STORE_MANAGER', 'PROMOTION_MANAGE'),
    ('STORE_MANAGER', 'RESERVATION_VIEW'),
    ('STORE_MANAGER', 'RESERVATION_MANAGE'),
    ('STORE_MANAGER', 'QUEUE_MANAGE'),
    ('STORE_MANAGER', 'EMPLOYEE_VIEW'),
    ('STORE_MANAGER', 'SCHEDULE_MANAGE'),
    ('STORE_MANAGER', 'LEAVE_APPROVE'),
    ('STORE_MANAGER', 'REPORT_VIEW'),
    ('STORE_MANAGER', 'AUDIT_VIEW'),
    ('STORE_MANAGER', 'AUDIT_APPROVE'),
    ('STORE_MANAGER', 'AI_RECOMMENDATION_VIEW'),
    ('STORE_MANAGER', 'AI_RECOMMENDATION_APPROVE');

-- CASHIER: 18 permissions
INSERT INTO tmp_rbac_role_permissions (role_code, permission_code) VALUES
    ('CASHIER', 'TABLE_VIEW'),
    ('CASHIER', 'TABLE_MANAGE'),
    ('CASHIER', 'TABLE_CLEAN'),
    ('CASHIER', 'ORDER_CREATE'),
    ('CASHIER', 'ORDER_VIEW'),
    ('CASHIER', 'SETTLEMENT_COLLECT'),
    ('CASHIER', 'SETTLEMENT_STACKING'),
    ('CASHIER', 'REFUND_SMALL'),
    ('CASHIER', 'PAYMENT_SWITCH'),
    ('CASHIER', 'SHIFT_OPEN_CLOSE'),
    ('CASHIER', 'SHIFT_VIEW'),
    ('CASHIER', 'MENU_VIEW'),
    ('CASHIER', 'BUFFET_START'),
    ('CASHIER', 'MEMBER_VIEW'),
    ('CASHIER', 'MEMBER_MANAGE'),
    ('CASHIER', 'MEMBER_RECHARGE'),
    ('CASHIER', 'RESERVATION_VIEW'),
    ('CASHIER', 'QUEUE_MANAGE');

-- KITCHEN_STAFF: 3 permissions
INSERT INTO tmp_rbac_role_permissions (role_code, permission_code) VALUES
    ('KITCHEN_STAFF', 'KDS_OPERATE'),
    ('KITCHEN_STAFF', 'TICKET_CANCEL'),
    ('KITCHEN_STAFF', 'MENU_VIEW');

-- WAITER: 11 permissions
INSERT INTO tmp_rbac_role_permissions (role_code, permission_code) VALUES
    ('WAITER', 'TABLE_VIEW'),
    ('WAITER', 'TABLE_MANAGE'),
    ('WAITER', 'TABLE_CLEAN'),
    ('WAITER', 'ORDER_CREATE'),
    ('WAITER', 'ORDER_VIEW'),
    ('WAITER', 'MENU_VIEW'),
    ('WAITER', 'BUFFET_START'),
    ('WAITER', 'KDS_OPERATE'),
    ('WAITER', 'RESERVATION_VIEW'),
    ('WAITER', 'QUEUE_MANAGE'),
    ('WAITER', 'MEMBER_VIEW');

-- INVENTORY_CLERK: 7 permissions
INSERT INTO tmp_rbac_role_permissions (role_code, permission_code) VALUES
    ('INVENTORY_CLERK', 'INVENTORY_VIEW'),
    ('INVENTORY_CLERK', 'INVENTORY_MANAGE'),
    ('INVENTORY_CLERK', 'STOCKTAKE_CREATE'),
    ('INVENTORY_CLERK', 'SOP_MANAGE'),
    ('INVENTORY_CLERK', 'PURCHASE_CREATE'),
    ('INVENTORY_CLERK', 'MENU_VIEW'),
    ('INVENTORY_CLERK', 'REPORT_VIEW');

-- FINANCE: 10 permissions
INSERT INTO tmp_rbac_role_permissions (role_code, permission_code) VALUES
    ('FINANCE', 'REPORT_VIEW'),
    ('FINANCE', 'REPORT_VIEW_ALL'),
    ('FINANCE', 'AUDIT_VIEW'),
    ('FINANCE', 'AUDIT_APPROVE'),
    ('FINANCE', 'GTO_EXPORT'),
    ('FINANCE', 'SHIFT_VIEW_ALL'),
    ('FINANCE', 'CHANNEL_SETTLEMENT'),
    ('FINANCE', 'MEMBER_VIEW'),
    ('FINANCE', 'PAYROLL_VIEW'),
    ('FINANCE', 'PAYROLL_MANAGE');

INSERT IGNORE INTO custom_role_permissions (role_id, permission_code)
SELECT
    r.id,
    rp.permission_code
FROM tmp_rbac_role_permissions rp
JOIN custom_roles r
  ON r.merchant_id IS NULL
 AND r.role_code = rp.role_code
JOIN permissions p
  ON p.permission_code = rp.permission_code;

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_role_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_rbac_roles;
DROP TEMPORARY TABLE IF EXISTS tmp_rbac_permissions;
