# FounderPOS V3 — 完整 Sprint 规划

**Version:** V20260328012
**Date:** 2026-03-28
**Status:** DRAFT — 待审核
**前置文档:** `2026-03-28-17-gaps-phase1-buffet-design.md`（Gap 级设计）
**本文档定位:** Sprint 级详细实现规划，含完整 DDL、API schema、边界条件、前端组件、测试场景

---

## 0. 总体架构

### 0.1 部署架构（验证阶段）

```
单体应用 pos-backend (一个 JAR)
  + pos-payment-service (独立微服务，已有)
  + MySQL 8.0
  + Nginx 反向代理
  + 4 个前端 SPA
  ──────────────────
  全部跑在一台 AWS EC2 (Docker Compose)
```

### 0.2 Maven 多模块结构

```
pos-backend/
├── pom.xml (parent)
├── pos-common/          — 通用：异常、DTO、审计切面、工具类
├── pos-core/            — 桌台、session、订单（核心交易）
├── pos-catalog/         — 商品、SKU、修饰符、价格、菜单
├── pos-buffet/          — 自助餐（新模块）
├── pos-kitchen/         — KDS、工作站、出票
├── pos-inventory/       — 库存、SOP、采购、OCR
├── pos-member/          — 会员、积分、储值、券、反馈
├── pos-settlement/      — 结算、支付叠加、重试
├── pos-promotion/       — 促销、库存驱动促销
├── pos-report/          — 报表、快照、AI 摘要
├── pos-auth/            — 用户、RBAC、权限
├── pos-ops/             — 巡店、CCTV、审计日志
├── pos-integration/     — 外部对接、日志
├── pos-ai/              — MCP、AI Operator、Agent
└── pos-app/             — Spring Boot 启动器
```

**模块间依赖规则：**
- `pos-common` ← 所有模块依赖
- `pos-core` ← `pos-buffet`, `pos-settlement`, `pos-kitchen`
- `pos-catalog` ← `pos-buffet`, `pos-inventory`, `pos-promotion`
- `pos-member` ← `pos-settlement`（积分/储值抵扣）
- 模块间通过 **interface** 依赖，不直接依赖实现类
- 禁止循环依赖

### 0.3 Sprint 总览

| Sprint | 名称 | Flyway | 新表 | 改造表 | 估时 |
|--------|------|--------|------|--------|------|
| S1 | 桌台增强 + 审计 | V070-V076 | 2 | 3 | 1周 |
| S2 | 自助餐 | V077-V082 | 4 | 3 | 1.5周 |
| S3 | 支付闭环 | V083-V086 | 1 | 2 | 1周 |
| S4 | 库存链路 | V087-V092 | 2 | 2 | 1.5周 |
| S5 | 运营 + CRM | V093-V098 | 5 | 0 | 1周 |
| S6 | 报表 + KDS 增强 | V099-V102 | 1 | 1 | 1周 |

---

# Sprint 1: 桌台增强 + 审计日志

**目标：** 桌台状态机完善（并台、清台、动态 QR）+ 统一审计切面
**涉及 Gap：** G01 并台, G03 清台, G10 动态二维码, G17 审计日志
**Maven 模块：** `pos-core`, `pos-common`（审计切面）

---

## S1.1 Flyway Migrations

### V070__alter_store_tables.sql

```sql
-- 桌台表增强：区域、容量、QR、状态扩展
ALTER TABLE store_tables
  ADD COLUMN zone VARCHAR(64) NULL COMMENT '区域(大厅/包间/露台)' AFTER table_name,
  ADD COLUMN min_guests INT NOT NULL DEFAULT 1 COMMENT '最少容纳人数' AFTER zone,
  ADD COLUMN max_guests INT NOT NULL DEFAULT 4 COMMENT '最多容纳人数' AFTER min_guests,
  ADD COLUMN qr_token VARCHAR(128) NULL COMMENT '动态二维码token' AFTER max_guests,
  ADD COLUMN qr_generated_at TIMESTAMP NULL COMMENT 'token生成时间' AFTER qr_token,
  ADD COLUMN qr_expires_at TIMESTAMP NULL COMMENT 'token过期时间' AFTER qr_generated_at,
  MODIFY COLUMN table_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE'
    COMMENT 'AVAILABLE|OCCUPIED|RESERVED|PENDING_CLEAN|MERGED|DISABLED';

-- 区域索引（按区域查桌台）
ALTER TABLE store_tables ADD INDEX idx_st_zone (store_id, zone);
```

### V071__alter_table_sessions.sql

```sql
-- session 增加并台支持 + 人数
ALTER TABLE table_sessions
  ADD COLUMN merged_into_session_id BIGINT NULL
    COMMENT '被并入哪个session，NULL=未被并' AFTER session_status,
  ADD COLUMN total_guest_count INT NOT NULL DEFAULT 1
    COMMENT '含并桌后总人数' AFTER merged_into_session_id,
  ADD INDEX idx_ts_merged (merged_into_session_id);
```

### V072__create_table_merge_records.sql

```sql
CREATE TABLE table_merge_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    master_session_id BIGINT NOT NULL COMMENT '主桌session',
    merged_session_id BIGINT NOT NULL COMMENT '被并入的session',
    master_table_id BIGINT NOT NULL COMMENT '主桌',
    merged_table_id BIGINT NOT NULL COMMENT '被并入的桌',
    guest_count_snapshot INT NOT NULL COMMENT '并入时被并桌的人数',
    merged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unmerged_at TIMESTAMP NULL COMMENT 'NULL=仍在合并中',
    operated_by BIGINT NOT NULL COMMENT '操作人user_id',
    reason VARCHAR(255) NULL COMMENT '并台原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_tmr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_tmr_master_session FOREIGN KEY (master_session_id) REFERENCES table_sessions(id),
    CONSTRAINT fk_tmr_merged_session FOREIGN KEY (merged_session_id) REFERENCES table_sessions(id),
    CONSTRAINT fk_tmr_master_table FOREIGN KEY (master_table_id) REFERENCES store_tables(id),
    CONSTRAINT fk_tmr_merged_table FOREIGN KEY (merged_table_id) REFERENCES store_tables(id),
    INDEX idx_tmr_store_time (store_id, merged_at),
    INDEX idx_tmr_master (master_session_id),
    INDEX idx_tmr_merged (merged_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V073__alter_action_log_audit.sql

```sql
-- 统一审计日志增强
ALTER TABLE action_log
  ADD COLUMN actor_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM'
    COMMENT 'SYSTEM|USER|AI|WEBHOOK' AFTER id,
  ADD COLUMN actor_user_id BIGINT NULL COMMENT '操作人' AFTER actor_type,
  ADD COLUMN actor_ip VARCHAR(64) NULL AFTER actor_user_id,
  ADD COLUMN actor_device VARCHAR(128) NULL COMMENT '设备标识(POS终端号/浏览器)' AFTER actor_ip,
  ADD COLUMN risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW'
    COMMENT 'LOW|MEDIUM|HIGH|CRITICAL' AFTER actor_device,
  ADD COLUMN target_type VARCHAR(64) NULL COMMENT '操作对象类型(TABLE|ORDER|SKU|MEMBER...)' AFTER risk_level,
  ADD COLUMN target_id VARCHAR(128) NULL COMMENT '操作对象ID' AFTER target_type,
  ADD COLUMN before_snapshot JSON NULL COMMENT '变更前数据' AFTER target_id,
  ADD COLUMN after_snapshot JSON NULL COMMENT '变更后数据' AFTER before_snapshot,
  ADD COLUMN requires_approval BOOLEAN NOT NULL DEFAULT FALSE AFTER after_snapshot,
  ADD COLUMN approval_status VARCHAR(32) NULL COMMENT 'PENDING|APPROVED|REJECTED' AFTER requires_approval,
  ADD COLUMN approved_by BIGINT NULL AFTER approval_status,
  ADD COLUMN approved_at TIMESTAMP NULL AFTER approved_by,
  ADD INDEX idx_al_actor (actor_type, actor_user_id),
  ADD INDEX idx_al_risk (risk_level, created_at),
  ADD INDEX idx_al_target (target_type, target_id),
  ADD INDEX idx_al_approval (requires_approval, approval_status);
```

---

## S1.2 后端设计

### S1.2.1 审计切面（pos-common）

**注解定义：**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();                    // "TABLE_MERGE", "PRICE_CHANGE" 等
    RiskLevel riskLevel() default RiskLevel.LOW;
    boolean requiresApproval() default false;
    String targetType() default "";     // "TABLE", "ORDER", "SKU"
    String targetIdExpression() default ""; // SpEL: "#tableId", "#request.orderId"
}
```

**切面核心逻辑：**
```
@Around("@annotation(audited)")
AuditAspect.around(audited):
  1. 从 SecurityContext 获取当前用户 → actorType, actorUserId
  2. 从 HttpServletRequest 获取 → actorIp, actorDevice
  3. 解析 targetIdExpression (SpEL) → targetId
  4. 如果 requiresApproval:
     a. 检查用户是否有对应权限（按 risk_level 查 RBAC）
     b. 权限不足 → 抛 ApprovalRequiredException
     c. 权限够 → 继续执行
  5. 查 DB 当前值 → beforeSnapshot (JSON)
  6. 执行方法
  7. 查 DB 新值 → afterSnapshot (JSON)
  8. INSERT action_log
  9. 如果方法抛异常 → 仍然记录日志，action 后缀加 "_FAILED"
```

**beforeSnapshot 获取策略：**
- 单实体操作（改价、改库存）：直接查该实体当前值
- 批量操作：记录操作参数而非逐条查（避免性能问题）
- 创建操作：beforeSnapshot = null
- 删除操作：beforeSnapshot = 完整实体，afterSnapshot = null

### S1.2.2 并台服务（pos-core）

**TableMergeApplicationService.java:**

```
merge(storeId, masterTableId, mergedTableId, operatorId):
  校验:
    1. masterTable = tableRepo.findByIdAndStoreId(masterTableId, storeId)
       → 不存在: throw TableNotFoundException
    2. mergedTable = tableRepo.findByIdAndStoreId(mergedTableId, storeId)
       → 不存在: throw TableNotFoundException
    3. masterTable.status != OCCUPIED → throw InvalidTableStatusException("主桌必须在使用中")
    4. mergedTable.status != OCCUPIED → throw InvalidTableStatusException("被并桌必须在使用中")
    5. masterTableId == mergedTableId → throw SameTableException("不能自己并自己")
    6. masterSession = sessionRepo.findActiveByTableId(masterTableId)
       → 不存在: throw NoActiveSessionException
    7. mergedSession = sessionRepo.findActiveByTableId(mergedTableId)
       → 不存在: throw NoActiveSessionException
    8. mergedSession.mergedIntoSessionId != null
       → throw AlreadyMergedException("该桌已被并入其他桌")

  执行 (事务):
    9.  mergedSession.mergedIntoSessionId = masterSession.id
    10. masterSession.totalGuestCount += mergedSession.guestCount
    11. mergedTable.tableStatus = 'MERGED'
    12. INSERT table_merge_records {
          storeId, masterSessionId, mergedSessionId,
          masterTableId, mergedTableId,
          guestCountSnapshot: mergedSession.guestCount,
          operatedBy: operatorId
        }

  返回:
    MergeResultDTO { mergeRecordId, masterSession, mergedSession, mergedTable }

  事件:
    publish(TableMergedEvent { storeId, masterTableId, mergedTableId, mergeRecordId })
```

```
unmerge(storeId, mergeRecordId, operatorId):
  校验:
    1. record = mergeRecordRepo.findById(mergeRecordId)
       → 不存在: throw MergeRecordNotFoundException
    2. record.storeId != storeId → throw StoreAccessDeniedException
    3. record.unmergedAt != null → throw AlreadyUnmergedException("已经拆台了")
    4. masterSession = sessionRepo.findById(record.masterSessionId)
    5. masterSession.sessionStatus == 'SETTLED'
       → throw CannotUnmergeSettledException("已结账，不能拆台")

  执行 (事务):
    6. mergedSession = sessionRepo.findById(record.mergedSessionId)
    7. mergedSession.mergedIntoSessionId = null
    8. masterSession.totalGuestCount -= record.guestCountSnapshot
    9. mergedTable = tableRepo.findById(record.mergedTableId)
    10. mergedTable.tableStatus = 'OCCUPIED'
    11. record.unmergedAt = NOW()

  返回:
    UnmergeResultDTO { masterSession, restoredSession }
```

**结账时并台金额聚合：**
```
getSettlementItems(masterSessionId):
  1. items = orderItemRepo.findBySessionId(masterSessionId)
  2. mergedSessionIds = sessionRepo.findMergedIntoIds(masterSessionId)
  3. for each mergedId:
       items.addAll(orderItemRepo.findBySessionId(mergedId))
  4. return items
  // 价格计算在 SettlementService 里统一做，这里只聚合 items
```

### S1.2.3 清台服务（pos-core）

```
markClean(storeId, tableId, operatorId):
  校验:
    1. table = tableRepo.findByIdAndStoreId(tableId, storeId)
    2. table.status != PENDING_CLEAN
       → throw InvalidTableStatusException("只有待清台状态才能标记清台")

  执行:
    3. table.tableStatus = 'AVAILABLE'
    4. 刷新 QR token（调 QrService.refresh）
    5. 发事件 TableCleanedEvent

  自动清台 (可选):
    定时任务每分钟检查:
    SELECT * FROM store_tables
    WHERE table_status = 'PENDING_CLEAN'
      AND updated_at < NOW() - INTERVAL {config.autoCleanTimeoutMinutes} MINUTE
    → 自动标记 AVAILABLE
    （config 从 merchant_configs 读，key = 'auto_clean_timeout_minutes'，默认 0 = 关闭）
```

### S1.2.4 动态二维码服务（pos-core）

```
refreshQr(storeId, tableId):
  table = tableRepo.findByIdAndStoreId(tableId, storeId)
  store = storeRepo.findById(storeId)

  timestamp = System.currentTimeMillis()
  random = SecureRandom.nextBytes(16)
  payload = storeId + ":" + tableId + ":" + timestamp + ":" + hex(random)
  token = HmacSHA256(store.secretKey, payload)   // store 需要有 secret_key 字段

  table.qrToken = token
  table.qrGeneratedAt = NOW()
  table.qrExpiresAt = NOW() + config.qrExpiryHours (默认 24h)

  qrUrl = "https://{domain}/qr/{storeId}/{tableId}?t={token}&ts={timestamp}"
  return QrDTO { qrUrl, expiresAt }

validateQr(storeId, tableId, token, timestamp):
  table = tableRepo.findByIdAndStoreId(tableId, storeId)
  1. table.qrExpiresAt < NOW() → throw QrExpiredException
  2. table.qrToken != token → throw QrInvalidException
  3. 通过 → 返回桌台信息 + 当前 session（如果有）

批量刷新（cron 每日凌晨 3:00）:
  SELECT id, store_id FROM store_tables WHERE table_status != 'DISABLED'
  → 逐个调 refreshQr
```

**stores 表需要加 secret_key：**
```sql
-- 放在 V070 或单独 migration
ALTER TABLE stores
  ADD COLUMN secret_key VARCHAR(128) NULL COMMENT 'HMAC签名密钥，首次自动生成';
```

---

## S1.3 API 详细定义

### POST /api/v2/stores/{storeId}/tables/merge

**Request:**
```json
{
  "masterTableId": 101,
  "mergedTableId": 102
}
```

**Response 200:**
```json
{
  "mergeRecordId": 1,
  "masterSession": {
    "id": 501,
    "tableId": 101,
    "tableCode": "A01",
    "totalGuestCount": 6,
    "sessionStatus": "ACTIVE"
  },
  "mergedSession": {
    "id": 502,
    "tableId": 102,
    "tableCode": "A02",
    "mergedIntoSessionId": 501,
    "sessionStatus": "ACTIVE"
  },
  "mergedTable": {
    "id": 102,
    "tableCode": "A02",
    "tableStatus": "MERGED"
  }
}
```

**Error Responses:**
```
400: { "code": "SAME_TABLE", "message": "不能将桌台并入自身" }
400: { "code": "INVALID_TABLE_STATUS", "message": "主桌必须在使用中，当前状态: AVAILABLE" }
400: { "code": "ALREADY_MERGED", "message": "桌台 A02 已被并入其他桌" }
404: { "code": "TABLE_NOT_FOUND", "message": "桌台不存在" }
404: { "code": "NO_ACTIVE_SESSION", "message": "桌台没有活跃的 session" }
```

### POST /api/v2/stores/{storeId}/tables/unmerge

**Request:**
```json
{
  "mergeRecordId": 1
}
```

**Response 200:**
```json
{
  "masterSession": {
    "id": 501,
    "totalGuestCount": 4
  },
  "restoredSession": {
    "id": 502,
    "tableId": 102,
    "mergedIntoSessionId": null,
    "sessionStatus": "ACTIVE"
  },
  "restoredTable": {
    "id": 102,
    "tableCode": "A02",
    "tableStatus": "OCCUPIED"
  }
}
```

**Error Responses:**
```
400: { "code": "ALREADY_UNMERGED", "message": "已经拆台了" }
400: { "code": "CANNOT_UNMERGE_SETTLED", "message": "已结账的 session 不能拆台" }
404: { "code": "MERGE_RECORD_NOT_FOUND" }
```

### GET /api/v2/stores/{storeId}/tables/{tableId}/merge-info

**Response 200:**
```json
{
  "tableId": 101,
  "isMasterTable": true,
  "mergedTables": [
    {
      "mergeRecordId": 1,
      "tableId": 102,
      "tableCode": "A02",
      "guestCount": 2,
      "mergedAt": "2026-03-28T19:30:00"
    }
  ]
}
```

### POST /api/v2/stores/{storeId}/tables/{tableId}/mark-clean

**Response 200:**
```json
{
  "tableId": 101,
  "tableCode": "A01",
  "previousStatus": "PENDING_CLEAN",
  "newStatus": "AVAILABLE",
  "qrRefreshed": true
}
```

### POST /api/v2/stores/{storeId}/tables/{tableId}/qr/refresh

**Response 200:**
```json
{
  "tableId": 101,
  "qrUrl": "https://pos.example.com/qr/1/101?t=abc123&ts=1711612800",
  "generatedAt": "2026-03-28T20:00:00",
  "expiresAt": "2026-03-29T20:00:00"
}
```

### GET /qr/{storeId}/{tableId} (公开接口，扫码入口)

**Query Params:** `t` (token), `ts` (timestamp)

**Response 302 (成功):** 重定向到 `/ordering/{storeId}?table={tableId}&session={sessionId}`

**Response 200 (失败，返回错误页):**
```json
{ "code": "QR_EXPIRED", "message": "二维码已过期，请联系服务员" }
{ "code": "QR_INVALID", "message": "无效的二维码" }
```

### GET /api/v2/stores/{storeId}/audit-logs

**Query Params:**
- `riskLevel`: LOW|MEDIUM|HIGH|CRITICAL
- `actorType`: USER|SYSTEM|AI
- `actorUserId`: Long
- `targetType`: TABLE|ORDER|SKU|MEMBER|...
- `action`: TABLE_MERGE|PRICE_CHANGE|...
- `dateFrom`, `dateTo`: ISO datetime
- `page`, `size`: 分页

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "actorType": "USER",
      "actorUserId": 10,
      "actorDisplayName": "张经理",
      "actorIp": "192.168.1.100",
      "action": "TABLE_MERGE",
      "riskLevel": "LOW",
      "targetType": "TABLE",
      "targetId": "101",
      "beforeSnapshot": { "tableStatus": "OCCUPIED" },
      "afterSnapshot": { "tableStatus": "MERGED" },
      "requiresApproval": false,
      "createdAt": "2026-03-28T19:30:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 5,
  "page": 0,
  "size": 10
}
```

---

## S1.4 前端改动

### POS 端 (android-preview-web)

**桌台地图页改动：**
```
现有: 桌台卡片显示 tableCode + status 颜色
新增:
  - PENDING_CLEAN 状态 → 黄色卡片 + 扫帚图标
  - MERGED 状态 → 灰色卡片 + 链接图标 + 显示"已并入 {masterTableCode}"
  - RESERVED 状态 → 蓝色卡片
  - 每张卡片显示 zone 标签（如"大厅"、"包间"）

长按桌台弹出操作菜单:
  OCCUPIED 状态:
    - 并台 → 弹出桌台选择器（只显示同 store 的 OCCUPIED 桌台）
    - 转台 (已有)
    - 查看订单 (已有)
  MERGED 状态:
    - 拆台 → 确认对话框
    - 查看主桌订单
  PENDING_CLEAN 状态:
    - 标记清台 → 一键操作
```

**并台流程 UI:**
```
1. 长按桌台 A01 → 选"并台"
2. 弹出: "选择要并入 A01 的桌台"
   → 显示所有 OCCUPIED 桌台卡片（排除自己和已 MERGED 的）
3. 点击 A02
4. 确认对话框: "将 A02 (2人) 并入 A01 (4人)？并台后 A02 的订单将归入 A01 统一结账。"
5. 确认 → 调 merge API → 刷新桌台地图
6. A01 显示 "6人" + 并台标识
7. A02 变灰 + "已并入 A01"
```

### 后台 (pc-admin)

**新增页面: 审计日志**
```
路径: /admin/audit-logs
功能:
  - 筛选: 风险等级、操作类型、操作人、日期范围
  - 列表: 时间 | 操作人 | 操作 | 风险等级 | 目标 | 状态
  - 展开行: 显示 before/after snapshot diff（JSON diff 高亮）
  - 待审批 tab: 显示 requires_approval=true && approval_status=PENDING
  - 审批操作: 通过/拒绝 + 备注
```

---

## S1.5 测试场景

| # | 场景 | 预期结果 |
|---|------|---------|
| T01 | A01(4人) 并 A02(2人) | A01 变 6 人，A02 变 MERGED |
| T02 | 并台后查看 A01 订单 | 包含 A01+A02 的所有 items |
| T03 | 并台后 A01 结账 | 金额含 A01+A02 全部消费 |
| T04 | 并台后拆台 A02 | A02 恢复 OCCUPIED，A01 人数减回 |
| T05 | 已结账后尝试拆台 | 报错 CANNOT_UNMERGE_SETTLED |
| T06 | A02 已被并入 A01，再并 A02 到 A03 | 报错 ALREADY_MERGED |
| T07 | 结账后桌台状态 | PENDING_CLEAN（不是直接 AVAILABLE） |
| T08 | 标记清台 | 变 AVAILABLE + QR 自动刷新 |
| T09 | 扫过期 QR 码 | 提示"二维码已过期" |
| T10 | 扫有效 QR 码 | 进入点单页，自动识别桌号 |
| T11 | 改价操作 | action_log 记录 before/after snapshot |
| T12 | 无权限用户尝试大额退款 | 生成待审批记录 |

---

# Sprint 2: 自助餐

**目标：** 自助餐完整流程（选档位 → 点单 → 计时 → 结账 → 打印单据）
**涉及：** Phase 1 自助餐全部 task
**Maven 模块：** `pos-buffet`（新建）, `pos-catalog`（改造）, `pos-core`（改造）
**依赖 S1：** table_sessions 的 merged_into_session_id, store_tables 的状态扩展

---

## S2.1 Flyway Migrations

### V077__create_buffet_packages.sql

```sql
CREATE TABLE buffet_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    package_code VARCHAR(64) NOT NULL COMMENT '档位编码',
    package_name VARCHAR(255) NOT NULL COMMENT '档位名(如"标准自助餐""豪华海鲜档")',
    description TEXT NULL COMMENT '档位描述',
    price_cents BIGINT NOT NULL COMMENT '每人价格(分)',
    child_price_cents BIGINT NULL COMMENT '儿童价(分)，NULL=无儿童价',
    child_age_max INT NULL COMMENT '儿童年龄上限',
    duration_minutes INT NOT NULL DEFAULT 90 COMMENT '用餐时长(分钟)',
    warning_before_minutes INT NOT NULL DEFAULT 10 COMMENT '到期前N分钟提醒',
    overtime_fee_per_minute_cents BIGINT NOT NULL DEFAULT 0 COMMENT '超时每分钟费用(分)',
    overtime_grace_minutes INT NOT NULL DEFAULT 5 COMMENT '超时宽限期(分钟)',
    max_overtime_minutes INT NOT NULL DEFAULT 60 COMMENT '最长超时(超过强制结账)',
    package_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|INACTIVE',
    applicable_time_slots JSON NULL COMMENT '适用时段["LUNCH","DINNER"]，NULL=全时段',
    applicable_days JSON NULL COMMENT '适用日期["MON"-"SUN"]，NULL=全天',
    sort_order INT NOT NULL DEFAULT 0,
    image_id BIGINT NULL COMMENT '档位图片',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_bp_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_bp_code UNIQUE (store_id, package_code),
    INDEX idx_bp_status (store_id, package_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V078__create_buffet_package_items.sql

```sql
CREATE TABLE buffet_package_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    inclusion_type VARCHAR(32) NOT NULL DEFAULT 'INCLUDED'
      COMMENT 'INCLUDED(套餐内免费)|SURCHARGE(有差价)|EXCLUDED(套餐外原价)',
    surcharge_cents BIGINT NOT NULL DEFAULT 0 COMMENT '差价(分)，仅SURCHARGE时有效',
    max_qty_per_person INT NULL COMMENT '每人限点数量，NULL=不限',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bpi_package FOREIGN KEY (package_id) REFERENCES buffet_packages(id) ON DELETE CASCADE,
    CONSTRAINT fk_bpi_sku FOREIGN KEY (sku_id) REFERENCES skus(id),
    CONSTRAINT uk_bpi UNIQUE (package_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V079__alter_table_sessions_buffet.sql

```sql
ALTER TABLE table_sessions
  ADD COLUMN dining_mode VARCHAR(32) NOT NULL DEFAULT 'A_LA_CARTE'
    COMMENT 'A_LA_CARTE|BUFFET|DELIVERY' AFTER session_status,
  ADD COLUMN guest_count INT NOT NULL DEFAULT 1 AFTER dining_mode,
  ADD COLUMN child_count INT NOT NULL DEFAULT 0 AFTER guest_count,
  ADD COLUMN buffet_package_id BIGINT NULL AFTER child_count,
  ADD COLUMN buffet_started_at TIMESTAMP NULL AFTER buffet_package_id,
  ADD COLUMN buffet_ends_at TIMESTAMP NULL AFTER buffet_started_at,
  ADD COLUMN buffet_status VARCHAR(32) NULL
    COMMENT 'NULL(非自助)|ACTIVE|WARNING|OVERTIME|ENDED' AFTER buffet_ends_at,
  ADD COLUMN buffet_overtime_minutes INT NOT NULL DEFAULT 0 AFTER buffet_status,
  ADD CONSTRAINT fk_ts_buffet_pkg FOREIGN KEY (buffet_package_id) REFERENCES buffet_packages(id),
  ADD INDEX idx_ts_dining (store_id, dining_mode),
  ADD INDEX idx_ts_buffet_status (store_id, buffet_status);
```

### V080__alter_products_menu_modes.sql

```sql
ALTER TABLE products
  ADD COLUMN menu_modes JSON NULL
    COMMENT '可见模式["A_LA_CARTE","BUFFET","DELIVERY"]，NULL=全模式可见'
    AFTER image_id;
```

### V081__alter_order_items_buffet.sql

```sql
ALTER TABLE active_table_order_items
  ADD COLUMN is_buffet_included BOOLEAN NOT NULL DEFAULT FALSE
    COMMENT 'true=套餐内免费项' AFTER item_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '套餐内差价(分)' AFTER is_buffet_included;

ALTER TABLE submitted_order_items
  ADD COLUMN is_buffet_included BOOLEAN NOT NULL DEFAULT FALSE AFTER line_total_cents,
  ADD COLUMN buffet_surcharge_cents BIGINT NOT NULL DEFAULT 0 AFTER is_buffet_included;
```

### V082__create_menu_time_slots.sql

```sql
CREATE TABLE menu_time_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    slot_code VARCHAR(64) NOT NULL,
    slot_name VARCHAR(128) NOT NULL COMMENT '如"早茶""午市""晚市"',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    applicable_days JSON NOT NULL DEFAULT '["MON","TUE","WED","THU","FRI","SAT","SUN"]',
    dining_modes JSON NOT NULL DEFAULT '["A_LA_CARTE"]'
      COMMENT '该时段适用的用餐模式',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0 COMMENT '重叠时段取priority最高的',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mts_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_mts UNIQUE (store_id, slot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE menu_time_slot_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_slot_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE
      COMMENT 'true=该时段显示, false=隐藏',
    CONSTRAINT fk_mtsp_slot FOREIGN KEY (time_slot_id) REFERENCES menu_time_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_mtsp_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_mtsp UNIQUE (time_slot_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## S2.2 后端设计

### S2.2.1 BuffetPackageApplicationService (CRUD)

```
createPackage(storeId, request):
  校验:
    - package_code 唯一
    - price_cents > 0
    - duration_minutes > 0, <= 300
    - child_price_cents <= price_cents (如果有)
    - overtime_fee <= price_cents / duration (合理性校验，超时费不应超过原价)
  INSERT buffet_packages
  return PackageDTO

updatePackage(storeId, packageId, request):
  校验: 不能修改已有活跃 session 引用的 package (或只允许改名/改图不改价)
  实际业务: 改价应该创建新 package，旧 package INACTIVE

bindItems(packageId, items[]):
  校验:
    - 每个 sku_id 存在且属于同一 merchant
    - surcharge_cents >= 0
    - inclusion_type 合法
  批量 UPSERT buffet_package_items
  return 绑定结果

getPackagesForStore(storeId, ?timeSlot):
  查询 ACTIVE packages
  如果传了 timeSlot → 过滤 applicable_time_slots 包含该时段的
  返回包含 item 数量统计的列表
```

### S2.2.2 BuffetSessionApplicationService

```
startBuffet(sessionId, packageId, guestCount, childCount):
  校验:
    1. session 存在且 status = ACTIVE
    2. session.diningMode 当前为 A_LA_CARTE (未设置过)
    3. package 存在且 ACTIVE
    4. package 适用当前时段 (applicable_time_slots check)
    5. guestCount >= 1, childCount >= 0

  执行:
    session.diningMode = 'BUFFET'
    session.guestCount = guestCount
    session.childCount = childCount
    session.buffetPackageId = packageId
    session.buffetStartedAt = NOW()
    session.buffetEndsAt = NOW() + package.durationMinutes
    session.buffetStatus = 'ACTIVE'

  返回:
    BuffetSessionDTO {
      sessionId, packageName, guestCount, childCount,
      startedAt, endsAt, remainingMinutes, status
    }

getBuffetStatus(sessionId):
  session = load session
  如果 diningMode != BUFFET → throw NotBuffetSessionException

  now = NOW()
  remainingMs = session.buffetEndsAt - now
  package = load package

  如果 remainingMs > package.warningBeforeMinutes * 60 * 1000:
    status = ACTIVE
  否则如果 remainingMs > 0:
    status = WARNING
    updateIfChanged(session.buffetStatus, WARNING)
  否则:
    overtimeMinutes = abs(remainingMs) / 60000
    如果 overtimeMinutes <= package.overtimeGraceMinutes:
      status = WARNING (宽限期)
    否则:
      status = OVERTIME
      actualOvertimeMinutes = overtimeMinutes - package.overtimeGraceMinutes
      如果 actualOvertimeMinutes > package.maxOvertimeMinutes:
        actualOvertimeMinutes = package.maxOvertimeMinutes
        // TODO: 通知强制结账
      session.buffetOvertimeMinutes = actualOvertimeMinutes
      updateIfChanged(session.buffetStatus, OVERTIME)

  返回:
    BuffetStatusDTO {
      status, remainingMinutes (可为负数),
      overtimeMinutes, overtimeFeeCents,
      warningMessage (如果有)
    }

calculateBuffetTotal(sessionId):
  session = load session + package
  items = load active_table_order_items for session

  // 套餐基础价
  adultTotal = package.priceCents * session.guestCount
  childTotal = (package.childPriceCents ?: package.priceCents) * session.childCount
  packageTotal = adultTotal + childTotal

  // 差价项
  surchargeTotal = items.stream()
    .filter(i -> i.buffetSurchargeCents > 0)
    .mapToLong(i -> i.buffetSurchargeCents * i.quantity)
    .sum()

  // 套餐外项（完整价格）
  extraTotal = items.stream()
    .filter(i -> !i.isBuffetIncluded && i.buffetSurchargeCents == 0)
    .mapToLong(i -> i.itemTotalCents)
    .sum()

  // 超时费
  overtimeMinutes = session.buffetOvertimeMinutes
  overtimeFee = overtimeMinutes * package.overtimeFeePerMinuteCents

  grandTotal = packageTotal + surchargeTotal + extraTotal + overtimeFee

  返回:
    BuffetBillDTO {
      packageName, guestCount, childCount,
      packageTotal, surchargeTotal, extraTotal, overtimeFee,
      grandTotal,
      surchargeItems[],  // { skuName, qty, surcharge }
      extraItems[]       // { skuName, qty, price }
    }
```

### S2.2.3 菜单过滤（pos-catalog 改造）

```
getMenuForDiningMode(storeId, diningMode, ?packageId, ?timeSlotCode):
  1. 获取所有 ACTIVE products for store
  2. 过滤 menu_modes:
     - product.menuModes == null → 包含（向后兼容）
     - product.menuModes 包含 diningMode → 包含
     - 否则 → 排除
  3. 如果 timeSlotCode != null:
     timeSlot = findByCode(storeId, timeSlotCode)
     过滤 menu_time_slot_products:
       - 有绑定且 is_visible=true → 包含
       - 有绑定且 is_visible=false → 排除
       - 无绑定 → 包含（默认全时段可见）
  4. 如果 diningMode == BUFFET && packageId != null:
     对每个 SKU 标注:
       buffetItem = buffetPackageItemRepo.findByPackageAndSku(packageId, skuId)
       如果 buffetItem != null:
         sku.inclusionType = buffetItem.inclusionType
         sku.surchargeCents = buffetItem.surchargeCents
         sku.maxQtyPerPerson = buffetItem.maxQtyPerPerson
       否则:
         sku.inclusionType = 'EXCLUDED' (套餐外，按原价)
  5. 按 category 分组返回
```

### S2.2.4 自助餐点单校验（pos-core 改造）

```
addItemToOrder(sessionId, skuId, quantity, modifiers[]):
  session = load session

  如果 session.diningMode == 'BUFFET':
    package = load buffet_package
    packageItem = findByPackageAndSku(package.id, skuId)

    // 限量校验
    如果 packageItem != null && packageItem.maxQtyPerPerson != null:
      existingQty = getOrderedQtyForSku(sessionId, skuId)
      maxTotal = packageItem.maxQtyPerPerson * session.guestCount
      如果 existingQty + quantity > maxTotal:
        throw BuffetItemLimitExceededException(
          "每人限点 {max} 份，{guestCount} 人共可点 {maxTotal} 份，已点 {existingQty} 份"
        )

    // 设置价格
    如果 packageItem == null:
      // 套餐外商品，按原价
      isBuffetIncluded = false
      buffetSurchargeCents = 0
      // itemTotalCents = sku.basePriceCents * quantity (正常定价)
    否则如果 packageItem.inclusionType == 'INCLUDED':
      isBuffetIncluded = true
      buffetSurchargeCents = 0
      // itemTotalCents = 0 (免费)
    否则如果 packageItem.inclusionType == 'SURCHARGE':
      isBuffetIncluded = true
      buffetSurchargeCents = packageItem.surchargeCents
      // itemTotalCents = surchargeCents * quantity

  // 继续走原有的 addItem 逻辑...
```

---

## S2.3 API 详细定义

### POST /api/v2/stores/{storeId}/buffet-packages

**Request:**
```json
{
  "packageCode": "DELUXE_SEAFOOD",
  "packageName": "豪华海鲜自助",
  "description": "包含三文鱼、龙虾、帝王蟹等高端海鲜",
  "priceCents": 16800,
  "childPriceCents": 9800,
  "childAgeMax": 12,
  "durationMinutes": 120,
  "warningBeforeMinutes": 15,
  "overtimeFeePerMinuteCents": 200,
  "overtimeGraceMinutes": 5,
  "maxOvertimeMinutes": 30,
  "applicableTimeSlots": ["LUNCH", "DINNER"],
  "applicableDays": ["FRI", "SAT", "SUN"],
  "imageId": 42
}
```

**Response 201:**
```json
{
  "id": 1,
  "storeId": 1,
  "packageCode": "DELUXE_SEAFOOD",
  "packageName": "豪华海鲜自助",
  "priceCents": 16800,
  "childPriceCents": 9800,
  "durationMinutes": 120,
  "packageStatus": "ACTIVE",
  "itemCount": 0
}
```

### POST /api/v2/buffet-packages/{pkgId}/items/batch

**Request:**
```json
{
  "items": [
    { "skuId": 101, "inclusionType": "INCLUDED", "maxQtyPerPerson": null },
    { "skuId": 102, "inclusionType": "INCLUDED", "maxQtyPerPerson": 2 },
    { "skuId": 201, "inclusionType": "SURCHARGE", "surchargeCents": 2800 },
    { "skuId": 301, "inclusionType": "EXCLUDED" }
  ]
}
```

**Response 200:**
```json
{
  "packageId": 1,
  "totalItems": 4,
  "included": 2,
  "surcharge": 1,
  "excluded": 1
}
```

### POST /api/v2/sessions/{sessionId}/buffet/start

**Request:**
```json
{
  "packageId": 1,
  "guestCount": 4,
  "childCount": 1
}
```

**Response 200:**
```json
{
  "sessionId": 501,
  "packageName": "豪华海鲜自助",
  "guestCount": 4,
  "childCount": 1,
  "startedAt": "2026-03-28T19:00:00",
  "endsAt": "2026-03-28T21:00:00",
  "durationMinutes": 120,
  "remainingMinutes": 120,
  "buffetStatus": "ACTIVE"
}
```

### GET /api/v2/sessions/{sessionId}/buffet/status

**Response 200 (正常):**
```json
{
  "buffetStatus": "ACTIVE",
  "remainingMinutes": 45,
  "overtimeMinutes": 0,
  "overtimeFeeCents": 0,
  "warningMessage": null
}
```

**Response 200 (超时):**
```json
{
  "buffetStatus": "OVERTIME",
  "remainingMinutes": -20,
  "overtimeMinutes": 15,
  "overtimeFeeCents": 3000,
  "warningMessage": "已超时 20 分钟（宽限 5 分钟后计费 15 分钟），超时费 $30.00"
}
```

### GET /api/v2/stores/{storeId}/menu?diningMode=BUFFET&packageId=1

**Response 200:**
```json
{
  "diningMode": "BUFFET",
  "packageId": 1,
  "packageName": "豪华海鲜自助",
  "categories": [
    {
      "categoryId": 10,
      "categoryName": "海鲜",
      "products": [
        {
          "productId": 100,
          "productName": "三文鱼刺身",
          "skus": [
            {
              "skuId": 101,
              "skuName": "三文鱼刺身 (标准)",
              "basePriceCents": 4800,
              "inclusionType": "INCLUDED",
              "surchargeCents": 0,
              "maxQtyPerPerson": 2,
              "displayPrice": "套餐内 (每人限2份)"
            },
            {
              "skuId": 201,
              "skuName": "澳洲龙虾",
              "basePriceCents": 12800,
              "inclusionType": "SURCHARGE",
              "surchargeCents": 2800,
              "maxQtyPerPerson": null,
              "displayPrice": "+$28.00"
            }
          ]
        }
      ]
    }
  ]
}
```

### POST /api/v2/sessions/{sessionId}/buffet/calculate

**Response 200:**
```json
{
  "packageName": "豪华海鲜自助",
  "guestCount": 4,
  "childCount": 1,
  "breakdown": {
    "adultPackageTotal": 67200,
    "childPackageTotal": 9800,
    "packageSubtotal": 77000,
    "surchargeItems": [
      { "skuName": "澳洲龙虾", "quantity": 2, "unitSurcharge": 2800, "subtotal": 5600 }
    ],
    "surchargeTotal": 5600,
    "extraItems": [
      { "skuName": "青岛啤酒", "quantity": 3, "unitPrice": 1500, "subtotal": 4500 }
    ],
    "extraTotal": 4500,
    "overtimeMinutes": 15,
    "overtimeFeeCents": 3000,
    "grandTotal": 90100
  }
}
```

---

## S2.4 前端改动

### QR 点单端 (qr-ordering-web)

**新增：模式选择页**
```
扫码后 → 检查 session.diningMode:
  如果已有 session 且 diningMode = BUFFET → 直接进自助餐菜单
  如果无 session 或 A_LA_CARTE:
    显示模式选择:
    ┌─────────────┐  ┌─────────────┐
    │   🍽️ 单点    │  │   🍱 自助餐  │
    │  按菜品点单   │  │  选档位入座   │
    └─────────────┘  └─────────────┘
    (外卖模式通过外卖平台进入，QR不显示)
```

**新增：档位选择页**
```
选"自助餐" → 显示档位列表:
┌─────────────────────────────────┐
│ 🦐 豪华海鲜自助                  │
│ $168/人 (儿童$98)  时长: 2小时    │
│ 三文鱼、龙虾、帝王蟹...          │
│ 适用: 午市/晚市                  │
│                    [选择]        │
├─────────────────────────────────┤
│ 🥩 标准自助                      │
│ $88/人  时长: 1.5小时             │
│ 烤肉、寿司、沙拉...              │
│ 适用: 全天                       │
│                    [选择]        │
└─────────────────────────────────┘

选择档位 → 输入人数 (成人/儿童) → 确认开台
→ 调 startBuffet API → 进入自助餐菜单
```

**自助餐菜单页改动：**
```
顶部固定: 倒计时组件
  ┌──────────────────────────────┐
  │ 🕐 剩余 45:30  豪华海鲜 4人  │  ← 绿色
  │ 🕐 剩余 08:20  即将到期      │  ← 黄色 + 脉冲动画
  │ 🕐 超时 15:00  超时费 $30    │  ← 红色 + 闪烁
  └──────────────────────────────┘

每个 SKU 卡片:
  套餐内免费: 显示 "✓ 套餐内" 绿色标签
  有差价:    显示 "+$28" 橙色标签
  套餐外:    显示原价 "$15" 无特殊标签
  限量:      显示 "每人限2份" + 已点数量

底部购物车:
  显示已点项目 + 差价/额外费用小计
  不显示套餐内免费项的价格
```

**结账页改动：**
```
分区显示:
  ┌─────────────────────────────┐
  │ 自助餐 - 豪华海鲜           │
  │ 成人 4 × $168.00 = $672.00  │
  │ 儿童 1 × $98.00  = $98.00   │
  │                    $770.00   │
  ├─────────────────────────────┤
  │ 加点项                      │
  │ 澳洲龙虾(+$28) × 2  $56.00 │
  ├─────────────────────────────┤
  │ 套餐外                      │
  │ 青岛啤酒 × 3       $45.00   │
  ├─────────────────────────────┤
  │ 超时费 15min × $2   $30.00  │
  ├─────────────────────────────┤
  │ 合计              $901.00    │
  └─────────────────────────────┘
```

### POS 端 (android-preview-web)

**开台流程改动：**
```
点击空桌 → 开台:
  1. 选择用餐模式: 单点 | 自助餐
  2. 如果自助餐:
     a. 选档位 (下拉/卡片)
     b. 输入成人数 + 儿童数
     c. 确认开台
  3. 桌台卡片显示:
     - 自助餐图标 + 档位名缩写
     - 倒计时 (绿→黄→红)
     - 人数
```

**桌台列表增强：**
```
自助餐桌台卡片:
  ┌──────────┐
  │ A01  🍱  │
  │ 海鲜档    │
  │ 4+1人     │
  │ ⏱ 45:30  │ ← 绿色
  └──────────┘

  ┌──────────┐
  │ A03  🍱  │
  │ 标准档    │
  │ 2人       │
  │ ⏱ 超时!  │ ← 红色闪烁
  └──────────┘
```

### 后台 (pc-admin)

**新增页面：自助餐档位管理**
```
路径: /admin/buffet-packages
功能:
  - 列表: 档位名 | 价格 | 时长 | 超时费 | 包含商品数 | 状态
  - 新建/编辑: 表单含所有字段
  - 商品绑定: 穿梭框或表格，左边全部SKU，右边已绑定
    每个已绑定项可设: 类型(免费/差价/排除) + 差价金额 + 限量
  - 预览: 模拟顾客视角看到的菜单
```

**新增页面：时段菜单配置**
```
路径: /admin/menu-time-slots
功能:
  - 时段列表: 时段名 | 时间 | 适用日 | 适用模式 | 状态
  - 编辑时段: 时间范围 + 适用天 + 适用模式
  - 商品可见性: 勾选框矩阵 (行=商品, 列=时段)
```

---

## S2.5 测试场景

| # | 场景 | 预期 |
|---|------|------|
| T20 | 开台选自助餐，选档位，输人数 | session 创建成功，buffetStatus=ACTIVE |
| T21 | 自助餐菜单只显示 BUFFET 模式商品 | 非 BUFFET 商品不出现 |
| T22 | 点套餐内免费商品 | item 价格=0，isBuffetIncluded=true |
| T23 | 点差价商品 | item 价格=差价，buffetSurchargeCents 正确 |
| T24 | 点套餐外商品 | item 价格=原价 |
| T25 | 点限量商品超过上限 | 报错提示限量 |
| T26 | 倒计时到 warning 阈值 | status 变 WARNING，前端变黄 |
| T27 | 超时（含宽限期） | 宽限期内不计费，超过后按分钟计费 |
| T28 | 超时达到最大值 | 停止计费，通知强制结账 |
| T29 | 结账金额 = 套餐×人 + 差价 + 套餐外 + 超时费 | 金额正确 |
| T30 | 自助餐 + 并台 | 并台后两桌人数合并，共享同一个档位 |
| T31 | 儿童价计算 | 成人和儿童分别按对应价格计算 |
| T32 | 时段菜单过滤 | 午市时段只显示午市商品 |

---

# Sprint 3: 支付闭环

**目标：** 积分/储值/券/外部支付的组合支付 + 失败重试/换方式
**涉及 Gap：** G02 支付叠加, G04 重试换方式
**Maven 模块：** `pos-settlement`（改造）, `pos-member`（积分/储值抵扣接口）
**依赖 S2：** 自助餐结账需要走支付叠加

---

## S3.1 Flyway Migrations

### V083__create_payment_stacking_rules.sql

```sql
CREATE TABLE payment_stacking_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NULL COMMENT 'NULL=品牌级规则',
    rule_name VARCHAR(128) NOT NULL,

    -- 叠加开关
    allow_points_deduct BOOLEAN NOT NULL DEFAULT TRUE,
    allow_cash_balance BOOLEAN NOT NULL DEFAULT TRUE,
    allow_coupon BOOLEAN NOT NULL DEFAULT TRUE,
    allow_mixed_payment BOOLEAN NOT NULL DEFAULT TRUE,

    -- 扣减优先级（数字越小越先扣）
    points_priority INT NOT NULL DEFAULT 1,
    coupon_priority INT NOT NULL DEFAULT 2,
    cash_balance_priority INT NOT NULL DEFAULT 3,
    external_payment_priority INT NOT NULL DEFAULT 4,

    -- 积分限制
    max_points_deduct_percent INT NOT NULL DEFAULT 50
      COMMENT '积分最多抵扣订单金额的%',
    points_to_cents_rate INT NOT NULL DEFAULT 100
      COMMENT '多少积分=1分钱',
    min_points_deduct BIGINT NOT NULL DEFAULT 0
      COMMENT '最少使用多少积分',

    -- 储值限制
    max_cash_balance_percent INT NOT NULL DEFAULT 100,

    -- 优惠券限制
    max_coupons_per_order INT NOT NULL DEFAULT 1,
    coupon_stackable_with_promotion BOOLEAN NOT NULL DEFAULT FALSE,

    -- 适用范围
    applicable_dining_modes JSON NOT NULL DEFAULT '["A_LA_CARTE","BUFFET","DELIVERY"]',
    applicable_order_min_cents BIGINT NOT NULL DEFAULT 0,

    -- 状态
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0 COMMENT '多条规则时取priority最高的active规则',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,

    CONSTRAINT fk_psr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_psr_lookup (merchant_id, store_id, is_active, priority DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V084__alter_settlement_records_stacking.sql

```sql
ALTER TABLE settlement_records
  ADD COLUMN points_deduct_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '积分抵扣金额(分)' AFTER total_amount_cents,
  ADD COLUMN points_deducted BIGINT NOT NULL DEFAULT 0
    COMMENT '消耗的积分数' AFTER points_deduct_cents,
  ADD COLUMN cash_balance_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '储值扣款(分)' AFTER points_deducted,
  ADD COLUMN coupon_discount_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '优惠券减免(分)' AFTER cash_balance_cents,
  ADD COLUMN promotion_discount_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '促销减免(分)' AFTER coupon_discount_cents,
  ADD COLUMN external_payment_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '外部支付金额(分)' AFTER promotion_discount_cents,
  ADD COLUMN coupon_id BIGINT NULL AFTER external_payment_cents,
  ADD COLUMN stacking_rule_id BIGINT NULL AFTER coupon_id;
```

### V085__alter_payment_attempts_retry.sql

```sql
ALTER TABLE payment_attempts
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER attempt_status,
  ADD COLUMN max_retries INT NOT NULL DEFAULT 3 AFTER retry_count,
  ADD COLUMN failure_reason VARCHAR(512) NULL AFTER max_retries,
  ADD COLUMN failure_code VARCHAR(64) NULL COMMENT '支付方返回的错误码' AFTER failure_reason,
  ADD COLUMN replaced_by_attempt_id BIGINT NULL
    COMMENT '换方式后新attempt的id' AFTER failure_code,
  ADD COLUMN parent_attempt_id BIGINT NULL
    COMMENT '原attempt的id(重试链)' AFTER replaced_by_attempt_id,
  MODIFY COLUMN attempt_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING|PROCESSING|SUCCESS|FAILED|CANCELLED|REPLACED|TIMEOUT',
  ADD INDEX idx_pa_parent (parent_attempt_id),
  ADD INDEX idx_pa_replaced (replaced_by_attempt_id);
```

### V086__create_settlement_stacking_log.sql

```sql
-- 结算叠加扣减流水（每一步的冻结/确认/回滚都记录）
CREATE TABLE settlement_stacking_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    step_order INT NOT NULL COMMENT '扣减顺序1,2,3,4',
    deduct_type VARCHAR(32) NOT NULL
      COMMENT 'POINTS|COUPON|CASH_BALANCE|EXTERNAL',
    amount_cents BIGINT NOT NULL COMMENT '本步扣减金额(分)',
    points_amount BIGINT NULL COMMENT '积分数(仅POINTS类型)',
    coupon_id BIGINT NULL COMMENT '券ID(仅COUPON类型)',
    step_status VARCHAR(32) NOT NULL DEFAULT 'FROZEN'
      COMMENT 'FROZEN|CONFIRMED|ROLLED_BACK',
    frozen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    rolled_back_at TIMESTAMP NULL,
    rollback_reason VARCHAR(255) NULL,

    CONSTRAINT fk_ssl_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_records(id),
    INDEX idx_ssl_settlement (settlement_id, step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## S3.2 后端设计

### S3.2.1 PaymentStackingService

```
calculateStacking(orderId, memberId, request):
  request = { usePoints, useCashBalance, couponId }

  1. order = loadOrder(orderId)
     orderTotal = order.totalCents

  2. promotionDiscount = promotionService.getAppliedDiscount(orderId)
     payableAmount = orderTotal - promotionDiscount

  3. rule = loadStackingRule(order.storeId, order.merchantId)
     如果 rule == null → 使用默认规则（全允许，默认顺序）

  4. 校验 applicable_order_min_cents
     如果 payableAmount < rule.applicableOrderMinCents → 不允许叠加

  5. 构建扣减步骤列表（按 priority 排序）:
     steps = []
     sortedMethods = sort by priority:
       { type: POINTS, priority: rule.pointsPriority, enabled: rule.allowPointsDeduct && request.usePoints },
       { type: COUPON, priority: rule.couponPriority, enabled: rule.allowCoupon && request.couponId != null },
       { type: CASH_BALANCE, priority: rule.cashBalancePriority, enabled: rule.allowCashBalance && request.useCashBalance },
       { type: EXTERNAL, priority: rule.externalPaymentPriority, enabled: true }

  6. remaining = payableAmount
     对每个 enabled method（按 priority 排序）:

     POINTS:
       如果 !rule.couponStackableWithPromotion && promotionDiscount > 0 → 跳过
       maxDeduct = remaining * rule.maxPointsDeductPercent / 100
       memberPoints = memberService.getAvailablePoints(memberId)
       pointsValue = memberPoints * 100 / rule.pointsToCentsRate  // 积分换算为分
       actualDeduct = min(maxDeduct, pointsValue, remaining)
       如果 actualDeduct > 0 && memberPoints >= rule.minPointsDeduct:
         pointsNeeded = actualDeduct * rule.pointsToCentsRate / 100
         steps.add({ type: POINTS, cents: actualDeduct, points: pointsNeeded })
         remaining -= actualDeduct

     COUPON:
       coupon = couponService.validate(request.couponId, memberId, orderId)
       如果 !rule.couponStackableWithPromotion && promotionDiscount > 0:
         → throw CouponNotStackableException
       couponValue = coupon.calculateDiscount(orderTotal)
       actualDiscount = min(couponValue, remaining)
       steps.add({ type: COUPON, cents: actualDiscount, couponId })
       remaining -= actualDiscount

     CASH_BALANCE:
       maxDeduct = remaining * rule.maxCashBalancePercent / 100
       availableCash = memberService.getAvailableCash(memberId)
       actualDeduct = min(maxDeduct, availableCash, remaining)
       如果 actualDeduct > 0:
         steps.add({ type: CASH_BALANCE, cents: actualDeduct })
         remaining -= actualDeduct

     EXTERNAL:
       如果 remaining > 0:
         steps.add({ type: EXTERNAL, cents: remaining })
         remaining = 0

  7. 返回 StackingCalculation {
       orderId, payableAmount, promotionDiscount,
       steps[], // 按执行顺序
       externalPaymentCents, // 需要外部支付的金额
       calculationId // 缓存此计算结果，confirm 时用
     }
```

```
confirmStacking(calculationId, externalPaymentMethod):
  calc = loadCachedCalculation(calculationId) // Redis 或内存，5分钟过期

  事务:
    对每个 step (按 step_order):
      POINTS → memberService.freezePoints(memberId, points)
      COUPON → couponService.markUsed(couponId)
      CASH_BALANCE → memberService.freezeCash(memberId, cents)
      EXTERNAL → 创建 payment_attempt, 调支付适配器

    每步写 settlement_stacking_log (status = FROZEN)
    写 settlement_record (含各项明细)

  外部支付回调成功后:
    对每个 FROZEN step → 改为 CONFIRMED
    POINTS → memberService.confirmPointsDeduct
    CASH_BALANCE → memberService.confirmCashDeduct
    settlement_record.status = SETTLED

  外部支付失败后:
    → 见 G04 重试/换方式
    最终放弃时:
      对每个 FROZEN step → 改为 ROLLED_BACK
      POINTS → memberService.unfreezePoints
      CASH_BALANCE → memberService.unfreezeCash
      COUPON → couponService.markAvailable
```

### S3.2.2 PaymentRetryService

```
retryPayment(attemptId):
  attempt = loadAttempt(attemptId)
  校验:
    - attempt.status == FAILED
    - attempt.retryCount < attempt.maxRetries

  执行:
    attempt.retryCount++
    attempt.status = PROCESSING
    调用同一支付适配器重试
    → 成功: attempt.status = SUCCESS, 触发 stacking confirm
    → 失败: attempt.status = FAILED, 记录 failureReason

switchPaymentMethod(attemptId, newMethod):
  oldAttempt = loadAttempt(attemptId)
  校验:
    - oldAttempt.status IN (FAILED, CANCELLED)

  执行:
    oldAttempt.status = REPLACED
    newAttempt = createAttempt(
      orderId, newMethod, oldAttempt.amountCents,
      parentAttemptId = oldAttempt.id
    )
    oldAttempt.replacedByAttemptId = newAttempt.id
    调用新支付适配器
    return newAttempt

cancelPayment(attemptId):
  attempt = loadAttempt(attemptId)
  attempt.status = CANCELLED
  回滚所有 stacking steps (unfreeze points/cash, restore coupon)
```

---

## S3.3 测试场景

| # | 场景 | 预期 |
|---|------|------|
| T40 | 纯现金结账（无叠加） | external_payment = 全额 |
| T41 | 积分抵扣50% + 现金 | 积分扣减正确，现金=剩余 |
| T42 | 积分 + 储值 + 现金组合 | 按 priority 顺序扣减 |
| T43 | 券 + 促销不可叠加 | 报错 CouponNotStackable |
| T44 | 积分不足最低使用量 | 积分抵扣跳过 |
| T45 | 储值余额不足 | 扣完储值，剩余走现金 |
| T46 | 外部支付失败 → 重试成功 | retryCount+1, 最终 SUCCESS |
| T47 | 外部支付失败 → 换方式 → 成功 | 旧 REPLACED, 新 SUCCESS |
| T48 | 外部支付失败 → 放弃 | 积分/储值/券全部回滚 |
| T49 | 重试超过 maxRetries | 提示换方式或放弃 |
| T50 | 自助餐结账走支付叠加 | 金额=套餐+差价+超时, 叠加逻辑正确 |

---

# Sprint 4: 库存链路

**目标：** 库存驱动促销 + OCR + SOP 批量导入 + 规格消耗差异
**涉及 Gap：** G05, G06, G07, G14
**Maven 模块：** `pos-inventory`, `pos-promotion`
**与 S1-S3 无硬依赖，可并行开发**

---

## S4.1 Flyway Migrations

### V087__alter_recipes_modifier.sql

```sql
ALTER TABLE recipes
  ADD COLUMN modifier_option_id BIGINT NULL
    COMMENT '关联修饰符选项，NULL=基础消耗' AFTER inventory_item_id,
  ADD COLUMN consumption_multiplier DECIMAL(5,2) NOT NULL DEFAULT 1.00
    COMMENT '消耗倍率(大份=1.5)' AFTER consumption_qty,
  DROP INDEX uk_recipe,
  ADD UNIQUE INDEX uk_recipe_v2 (sku_id, inventory_item_id, modifier_option_id);
```

### V088__alter_purchase_invoices_ocr.sql

```sql
ALTER TABLE purchase_invoices
  ADD COLUMN ocr_raw_result JSON NULL COMMENT 'OCR原始识别结果' AFTER ocr_status,
  ADD COLUMN ocr_confidence DECIMAL(3,2) NULL COMMENT '置信度0-1' AFTER ocr_raw_result,
  ADD COLUMN ocr_reviewed BOOLEAN NOT NULL DEFAULT FALSE AFTER ocr_confidence,
  ADD COLUMN ocr_reviewed_by BIGINT NULL AFTER ocr_reviewed,
  ADD COLUMN ocr_reviewed_at TIMESTAMP NULL AFTER ocr_reviewed_by;
```

### V089__create_sop_import_batches.sql

```sql
CREATE TABLE sop_import_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    error_rows INT NOT NULL DEFAULT 0,
    error_details JSON NULL COMMENT '[{"row":3,"field":"sku_code","error":"not found"}]',
    import_status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED'
      COMMENT 'UPLOADED|VALIDATING|VALIDATED|IMPORTING|COMPLETED|FAILED',
    imported_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_sib_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_sib UNIQUE (store_id, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V090__create_inventory_driven_promotions.sql

```sql
CREATE TABLE inventory_driven_promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    trigger_type VARCHAR(32) NOT NULL
      COMMENT 'EXPIRING_BATCH|OVERSTOCK|SLOW_MOVING',
    inventory_item_id BIGINT NOT NULL,
    batch_id BIGINT NULL,
    trigger_data JSON NOT NULL
      COMMENT '{"daysToExpiry":3,"remainingQty":50,"unit":"kg"}',
    affected_sku_ids JSON NOT NULL COMMENT '[101,102]',
    suggested_promotion_type VARCHAR(32) NOT NULL,
    suggested_discount_value INT NULL,
    suggested_start_at TIMESTAMP NOT NULL,
    suggested_end_at TIMESTAMP NOT NULL,
    draft_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
      COMMENT 'DRAFT|APPROVED|REJECTED|EXPIRED',
    promotion_rule_id BIGINT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    review_notes VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_idp_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_idp_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    INDEX idx_idp_status (store_id, draft_status),
    INDEX idx_idp_trigger (store_id, trigger_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## S4.2 后端核心逻辑

### 修饰符消耗差异计算

```
calculateConsumption(skuId, quantity, selectedModifierOptionIds[]):
  // 1. 基础配方
  baseRecipes = recipeRepo.findBySkuIdAndModifierOptionIdIsNull(skuId)

  // 2. 找所有 multiplier 类修饰符
  multiplier = 1.0
  extraRecipes = []

  for optionId in selectedModifierOptionIds:
    modRecipes = recipeRepo.findBySkuIdAndModifierOptionId(skuId, optionId)
    for r in modRecipes:
      如果 r.inventoryItemId 在 baseRecipes 中存在:
        // 同一原料 → 是倍率修饰
        multiplier *= r.consumptionMultiplier
      否则:
        // 新原料 → 是追加修饰
        extraRecipes.add(r)

  // 3. 计算最终消耗
  result = []
  for base in baseRecipes:
    result.add({
      inventoryItemId: base.inventoryItemId,
      qty: base.consumptionQty * multiplier * quantity,
      unit: base.consumptionUnit
    })
  for extra in extraRecipes:
    result.add({
      inventoryItemId: extra.inventoryItemId,
      qty: extra.consumptionQty * quantity,
      unit: extra.consumptionUnit
    })

  return result
```

### OCR 流程

```
scanInvoice(storeId, invoiceId, imageAssetId):
  1. invoice = loadInvoice(invoiceId)
  2. invoice.scanImageUrl = imageService.getUrl(imageAssetId)
  3. invoice.ocrStatus = 'PROCESSING'

  异步:
  4. imageBytes = imageService.download(imageAssetId)
  5. ocrResult = ocrClient.recognize(imageBytes)
     // ocrClient 接口: 抽象层，可接 Google Vision / AWS Textract / Tesseract
  6. matchedItems = autoMatchInventoryItems(storeId, ocrResult.items)
     // 按名称模糊匹配 + 历史匹配记录

  7. invoice.ocrRawResult = ocrResult.toJson()
  8. invoice.ocrConfidence = ocrResult.avgConfidence
  9. invoice.ocrStatus = 'COMPLETED'

  返回:
    OcrResultDTO {
      supplierName, invoiceDate, totalAmount,
      items: [{
        rawText, matchedInventoryItemId, matchedItemName,
        confidence, qty, unit, unitPriceCents, lineTotalCents
      }]
    }

confirmOcrResult(invoiceId, confirmedItems[]):
  事务:
    对每个 confirmedItem:
      INSERT/UPDATE purchase_invoice_items
      如果 inventoryItemId 匹配成功:
        触发入库流程 → 创建 inventory_batch + inventory_movement
    invoice.ocrReviewed = true
    invoice.ocrReviewedBy = currentUser
    invoice.ocrReviewedAt = NOW()
    invoice.invoiceStatus = 'CONFIRMED'
```

### 库存驱动促销扫描

```
scanInventoryForPromotions(storeId):  // cron 每天凌晨 + 每次入库后
  promotionDrafts = []

  // 1. 临期批次
  expiringBatches = inventoryBatchRepo.findExpiringSoon(storeId, warningDays)
  for batch in expiringBatches:
    affectedSkus = recipeRepo.findSkusByInventoryItemId(batch.inventoryItemId)
    daysToExpiry = daysBetween(NOW(), batch.expiryDate)
    discount = daysToExpiry <= 3 ? 30 : (daysToExpiry <= 7 ? 20 : 10)
    promotionDrafts.add({
      triggerType: 'EXPIRING_BATCH', batchId: batch.id,
      inventoryItemId: batch.inventoryItemId,
      affectedSkuIds: affectedSkus.ids,
      suggestedType: 'PERCENT_DISCOUNT', suggestedValue: discount,
      startAt: NOW(), endAt: batch.expiryDate
    })

  // 2. 积压
  overstockItems = inventoryItemRepo.findOverstock(storeId, thresholdMultiplier: 3)
  // current_stock > safety_stock * 3

  // 3. 滞销 (过去7天零销量的SKU的原料)
  slowMovingSkus = reportService.getZeroSalesSkus(storeId, days: 7)

  // 去重: 同一 inventory_item 不重复生成
  // 插入 inventory_driven_promotions (draft_status = 'DRAFT')
  // 通知店长
```

---

## S4.3 测试场景

| # | 场景 | 预期 |
|---|------|------|
| T60 | 点"大份牛腩饭" | 原料消耗 × 1.5 |
| T61 | 点"大份牛腩饭+加辣" | 原料 × 1.5 + 辣椒额外消耗 |
| T62 | OCR 扫描送货单 | 识别行项目，自动匹配原料 |
| T63 | OCR 匹配失败的行 | 标记低置信度，前端手动选择 |
| T64 | 导入 SOP CSV（全部有效） | success_rows = total_rows |
| T65 | 导入 SOP CSV（部分无效） | error_details 记录错误行 |
| T66 | 临期 3 天批次 | 生成 30% 折扣草案 |
| T67 | 积压原料 (>3倍安全库存) | 生成 15% 折扣草案 |
| T68 | 审批促销草案 | 创建 promotion_rule，draft_status=APPROVED |

---

# Sprint 5: 运营 + CRM

**目标：** 巡店 + 顾客反馈 + 第三方对接日志 + CCTV
**涉及 Gap：** G08, G09, G12, G13
**Maven 模块：** `pos-ops`（新建）, `pos-member`（反馈）, `pos-integration`（日志）
**无硬依赖，可与 S2/S3/S4 并行**

---

## S5.1 Flyway Migrations

### V093__create_inspection_records.sql

```sql
CREATE TABLE inspection_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    inspector_user_id BIGINT NOT NULL,
    inspection_date DATE NOT NULL,
    check_in_at TIMESTAMP NULL,
    check_out_at TIMESTAMP NULL,
    check_in_lat DECIMAL(10,7) NULL,
    check_in_lng DECIMAL(10,7) NULL,
    overall_score DECIMAL(3,1) NULL COMMENT '总分0-10',
    hygiene_score DECIMAL(3,1) NULL,
    service_score DECIMAL(3,1) NULL,
    food_quality_score DECIMAL(3,1) NULL,
    compliance_score DECIMAL(3,1) NULL,
    inspection_status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS'
      COMMENT 'IN_PROGRESS|COMPLETED|SUBMITTED',
    summary TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_ir_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ir_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_ir_date (store_id, inspection_date),
    INDEX idx_ir_inspector (inspector_user_id, inspection_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inspection_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL COMMENT 'HYGIENE|SERVICE|FOOD|COMPLIANCE|OTHER',
    item_description VARCHAR(512) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'INFO' COMMENT 'INFO|WARNING|CRITICAL',
    is_passed BOOLEAN NULL COMMENT 'NULL=未检查',
    finding_notes TEXT NULL,
    photo_urls JSON NULL,
    requires_followup BOOLEAN NOT NULL DEFAULT FALSE,
    followup_deadline DATE NULL,
    followup_status VARCHAR(32) NULL COMMENT 'PENDING|IN_PROGRESS|RESOLVED',
    resolved_by BIGINT NULL,
    resolved_at TIMESTAMP NULL,
    resolution_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ii_inspection FOREIGN KEY (inspection_id) REFERENCES inspection_records(id) ON DELETE CASCADE,
    INDEX idx_ii_followup (requires_followup, followup_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V094__create_customer_feedback.sql

```sql
CREATE TABLE customer_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    member_id BIGINT NULL,
    order_id BIGINT NULL,
    feedback_type VARCHAR(32) NOT NULL
      COMMENT 'REVIEW|COMPLAINT|SUGGESTION|WISH_LIST',
    overall_rating INT NULL COMMENT '1-5',
    food_rating INT NULL,
    service_rating INT NULL,
    ambience_rating INT NULL,
    content TEXT NULL,
    photo_urls JSON NULL,
    tags JSON NULL,
    wished_item_name VARCHAR(255) NULL,
    wish_vote_count INT NOT NULL DEFAULT 1,
    feedback_status VARCHAR(32) NOT NULL DEFAULT 'NEW'
      COMMENT 'NEW|ACKNOWLEDGED|IN_PROGRESS|RESOLVED|CLOSED',
    response_text TEXT NULL,
    responded_by BIGINT NULL,
    responded_at TIMESTAMP NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'QR_ORDER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cf_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_cf_type (store_id, feedback_type, created_at),
    INDEX idx_cf_status (store_id, feedback_status),
    INDEX idx_cf_rating (store_id, overall_rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V095__create_external_integration_logs.sql

```sql
CREATE TABLE external_integration_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NULL,
    merchant_id BIGINT NOT NULL,
    integration_type VARCHAR(64) NOT NULL
      COMMENT 'GRAB|FOODPANDA|GOOGLE|MALL_CRM|GTO|PAYMENT|OCR',
    direction VARCHAR(16) NOT NULL COMMENT 'OUTBOUND|INBOUND',
    http_method VARCHAR(16) NULL,
    request_url VARCHAR(1024) NULL,
    request_headers JSON NULL,
    request_body TEXT NULL,
    response_status INT NULL,
    response_body TEXT NULL,
    latency_ms INT NULL,
    result_status VARCHAR(32) NOT NULL COMMENT 'SUCCESS|FAILED|TIMEOUT|ERROR',
    error_message VARCHAR(512) NULL,
    business_type VARCHAR(64) NULL,
    business_ref VARCHAR(128) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_eil_type (merchant_id, integration_type, created_at),
    INDEX idx_eil_result (merchant_id, result_status, created_at),
    INDEX idx_eil_biz (business_type, business_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V096__create_cctv_events.sql

```sql
CREATE TABLE cctv_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    camera_id VARCHAR(128) NOT NULL,
    camera_location VARCHAR(255) NULL,
    event_type VARCHAR(64) NOT NULL
      COMMENT 'SLIP_FALL|STAFF_VIOLATION|CROWD_ANOMALY|FIRE_SMOKE|THEFT_SUSPECT|HYGIENE_VIOLATION|CUSTOM',
    severity VARCHAR(32) NOT NULL DEFAULT 'INFO'
      COMMENT 'INFO|WARNING|CRITICAL|EMERGENCY',
    event_at TIMESTAMP NOT NULL,
    snapshot_url VARCHAR(512) NULL,
    video_clip_url VARCHAR(512) NULL,
    ai_confidence DECIMAL(3,2) NULL,
    ai_description TEXT NULL,
    event_status VARCHAR(32) NOT NULL DEFAULT 'NEW'
      COMMENT 'NEW|ACKNOWLEDGED|INVESTIGATING|RESOLVED|FALSE_ALARM',
    handled_by BIGINT NULL,
    handled_at TIMESTAMP NULL,
    handling_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ce_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_ce_severity (store_id, severity, event_status),
    INDEX idx_ce_time (store_id, event_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## S5.2 测试场景

| # | 场景 | 预期 |
|---|------|------|
| T70 | 区域经理打卡巡店 | 记录 GPS + 时间 |
| T71 | 添加巡店问题(CRITICAL) + 拍照 | 照片 URL 存储，followup 生成 |
| T72 | 顾客 1 星评价 | 自动标记为 COMPLAINT，通知值班经理 |
| T73 | Wish List 相似项合并 | vote_count 递增 |
| T74 | 外部 API 调用失败 | 日志记录完整请求/响应 |
| T75 | 连续 3 次失败 | 触发告警 |
| T76 | CCTV EMERGENCY 事件 | 立即推送通知 |
| T77 | CCTV 标记误报 | event_status = FALSE_ALARM |

---

# Sprint 6: 报表 + KDS 增强

**目标：** 报表 AI 摘要 + 多店对比 + KDS 打印机回退
**涉及 Gap：** G11, G15, G16
**Maven 模块：** `pos-report`, `pos-kitchen`
**依赖 S4：** 库存数据纳入报表

---

## S6.1 Flyway Migrations

### V099__create_report_snapshots.sql

```sql
CREATE TABLE report_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    report_type VARCHAR(64) NOT NULL
      COMMENT 'DAILY_SUMMARY|WEEKLY_SUMMARY|MONTHLY_SUMMARY',
    report_date DATE NOT NULL,
    metrics_json JSON NOT NULL COMMENT '结构化指标数据',
    ai_summary TEXT NULL,
    ai_highlights JSON NULL COMMENT '["营收+15%","新增会员30"]',
    ai_warnings JSON NULL COMMENT '["库存低于安全线","差评增加"]',
    ai_suggestions JSON NULL COMMENT '["推出午市套餐","补货牛腩"]',
    ai_generated_at TIMESTAMP NULL,
    ai_model_version VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_rs UNIQUE (store_id, report_type, report_date),
    INDEX idx_rs_merchant (merchant_id, report_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### V100__alter_kitchen_stations_fallback.sql

```sql
ALTER TABLE kitchen_stations
  ADD COLUMN fallback_printer_ip VARCHAR(64) NULL
    COMMENT 'KDS故障时回退打印机' AFTER kds_display_id,
  ADD COLUMN kds_health_status VARCHAR(32) NOT NULL DEFAULT 'ONLINE'
    COMMENT 'ONLINE|OFFLINE|DEGRADED' AFTER fallback_printer_ip,
  ADD COLUMN last_heartbeat_at TIMESTAMP NULL AFTER kds_health_status,
  ADD COLUMN fallback_mode VARCHAR(32) NOT NULL DEFAULT 'AUTO'
    COMMENT 'AUTO|MANUAL|DISABLED' AFTER last_heartbeat_at;
```

---

## S6.2 metrics_json 结构定义

```json
{
  "revenue": {
    "totalCents": 1250000,
    "dineInCents": 900000,
    "buffetCents": 250000,
    "deliveryCents": 100000,
    "comparedToPrevDay": 0.15,
    "comparedToPrevWeek": 0.08
  },
  "orders": {
    "total": 145,
    "dineIn": 100,
    "buffet": 30,
    "delivery": 15,
    "avgOrderCents": 8621,
    "avgGuestsPerTable": 2.8,
    "tableTurnoverRate": 3.2
  },
  "hourlyRevenue": [
    { "hour": 11, "cents": 120000, "orders": 15 },
    { "hour": 12, "cents": 280000, "orders": 35 }
  ],
  "topSkus": [
    { "skuId": 101, "name": "招牌牛腩", "qty": 45, "revenue": 135000 }
  ],
  "members": {
    "newRegistrations": 12,
    "activeMembers": 89,
    "pointsIssued": 15000,
    "rechargeAmountCents": 500000
  },
  "inventory": {
    "lowStockAlerts": 3,
    "expiringBatches": 2,
    "wasteAmountCents": 15000,
    "topWasteItems": [{ "name": "鲜虾", "wasteCents": 8000 }]
  },
  "staff": {
    "totalHours": 48.5,
    "revenuePerLabourHour": 25773,
    "avgServiceTime": 12
  },
  "feedback": {
    "avgRating": 4.2,
    "totalReviews": 18,
    "complaints": 2
  }
}
```

---

## S6.3 测试场景

| # | 场景 | 预期 |
|---|------|------|
| T80 | 生成日报 | metrics_json 数据正确 |
| T81 | AI 摘要 | 摘要/亮点/警告/建议生成 |
| T82 | 多店对比 | 3 家店横向排名正确 |
| T83 | KDS 离线 90s | kds_health_status → OFFLINE |
| T84 | 离线后下一张 ticket | 自动走打印机 |
| T85 | KDS 恢复心跳 | 自动切回 KDS |
| T86 | fallback_mode=DISABLED | 离线后不切打印机，标记 DELIVERY_FAILED |

---

# 附录 A: 完整 Flyway Migration 清单

| Version | Sprint | 内容 |
|---------|--------|------|
| V070 | S1 | ALTER store_tables (zone, guests, qr, status) + ALTER stores (secret_key) |
| V071 | S1 | ALTER table_sessions (merged_into, total_guests) |
| V072 | S1 | CREATE table_merge_records |
| V073 | S1 | ALTER action_log (audit enhancement) |
| V077 | S2 | CREATE buffet_packages |
| V078 | S2 | CREATE buffet_package_items |
| V079 | S2 | ALTER table_sessions (buffet fields) |
| V080 | S2 | ALTER products (menu_modes) |
| V081 | S2 | ALTER order_items (buffet fields, both active + submitted) |
| V082 | S2 | CREATE menu_time_slots + menu_time_slot_products |
| V083 | S3 | CREATE payment_stacking_rules |
| V084 | S3 | ALTER settlement_records (stacking detail fields) |
| V085 | S3 | ALTER payment_attempts (retry/replace) |
| V086 | S3 | CREATE settlement_stacking_log |
| V087 | S4 | ALTER recipes (modifier consumption) |
| V088 | S4 | ALTER purchase_invoices (OCR fields) |
| V089 | S4 | CREATE sop_import_batches |
| V090 | S4 | CREATE inventory_driven_promotions |
| V093 | S5 | CREATE inspection_records + inspection_items |
| V094 | S5 | CREATE customer_feedback |
| V095 | S5 | CREATE external_integration_logs |
| V096 | S5 | CREATE cctv_events |
| V099 | S6 | CREATE report_snapshots |
| V100 | S6 | ALTER kitchen_stations (fallback) |

**总计:** 14 张新表 + 10 张改造表 = 24 个 migration

---

# 附录 B: Maven 模块 → Sprint 映射

| Module | S1 | S2 | S3 | S4 | S5 | S6 |
|--------|----|----|----|----|----|----|
| pos-common | ✅ 审计切面 | | | | | |
| pos-core | ✅ 桌台/并台/清台/QR | ✅ session 改造 | | | | |
| pos-catalog | | ✅ 菜单过滤 | | | | |
| pos-buffet | | ✅ 全部 | | | | |
| pos-settlement | | | ✅ 全部 | | | |
| pos-inventory | | | | ✅ 全部 | | |
| pos-promotion | | | | ✅ 库存促销 | | |
| pos-member | | | ✅ 积分/储值接口 | | ✅ 反馈 | |
| pos-ops | | | | | ✅ 巡店/CCTV | |
| pos-integration | | | | | ✅ 日志 | |
| pos-kitchen | | | | | | ✅ 回退 |
| pos-report | | | | | | ✅ 全部 |
| pos-auth | | | | | | |
| pos-ai | | | | | | ✅ 报表摘要 |

---

*End of sprint plan. 24 migrations, 14 new tables, 10 altered tables, 6 sprints, ~7 weeks.*
