# 员工管理数据模型

**Version:** V20260328005
**Date:** 2026-03-28
**Status:** DRAFT

---

## 1. 设计范围

从入职到离职的完整员工生命周期：

```
入职建档 → 排班 → 打卡考勤 → 工时统计 → 人效分析 → 薪资计算 → 离职
```

与 doc/68 的 `users` 表关系：`users` 管登录和权限，`employees` 管人事和薪资。一人一条 user 记录 + 一条 employee 记录，通过 `user_id` 关联。

---

## 2. 新增表

### 2.1 employees（员工档案）

```sql
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    employee_no VARCHAR(64) NOT NULL,

    -- 人事信息
    full_name VARCHAR(128) NOT NULL,
    id_type VARCHAR(32) NULL,
    id_number VARCHAR(64) NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(16) NULL,
    nationality VARCHAR(64) NULL,
    phone VARCHAR(64) NULL,
    email VARCHAR(255) NULL,
    emergency_contact_name VARCHAR(128) NULL,
    emergency_contact_phone VARCHAR(64) NULL,
    address TEXT NULL,

    -- 雇佣信息
    employment_type VARCHAR(32) NOT NULL DEFAULT 'FULL_TIME',
    hire_date DATE NOT NULL,
    probation_end_date DATE NULL,
    termination_date DATE NULL,
    termination_reason VARCHAR(255) NULL,
    primary_store_id BIGINT NULL,
    department VARCHAR(64) NULL,
    position VARCHAR(64) NULL,

    -- 薪资信息
    salary_type VARCHAR(32) NOT NULL DEFAULT 'MONTHLY',
    base_salary_cents BIGINT DEFAULT 0,
    hourly_rate_cents BIGINT DEFAULT 0,
    overtime_rate_multiplier DECIMAL(3,2) DEFAULT 1.50,

    -- 状态
    employee_status VARCHAR(32) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_emp_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_emp_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_emp_store FOREIGN KEY (primary_store_id) REFERENCES stores(id),
    CONSTRAINT uk_emp_no UNIQUE (merchant_id, employee_no),
    CONSTRAINT uk_emp_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**employment_type：** `FULL_TIME` | `PART_TIME` | `TEMPORARY` | `INTERN`

**salary_type：** `MONTHLY`（月薪）| `HOURLY`（时薪）| `DAILY`（日薪）

**employee_status：** `ACTIVE` | `ON_LEAVE` | `PROBATION` | `TERMINATED`

### 2.2 shift_templates（班次模板）

商户预定义班次类型，排班时选择模板：

```sql
CREATE TABLE shift_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_minutes INT DEFAULT 0,
    color_hex VARCHAR(7) DEFAULT '#4A90D9',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_st_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_st UNIQUE (merchant_id, template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**示例数据：**

| template_code | template_name | start_time | end_time | break_minutes |
|---------------|---------------|------------|----------|---------------|
| MORNING | 早班 | 07:00 | 15:00 | 60 |
| AFTERNOON | 午班 | 11:00 | 19:00 | 60 |
| EVENING | 晚班 | 15:00 | 23:00 | 60 |
| FULL_DAY | 全天班 | 09:00 | 21:00 | 90 |
| SPLIT | 两头班 | 10:00 | 14:00 | 0 |

### 2.3 employee_schedules（排班表）

```sql
CREATE TABLE employee_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    shift_template_id BIGINT NULL,

    -- 实际排班时间（可覆盖模板）
    scheduled_start TIME NOT NULL,
    scheduled_end TIME NOT NULL,
    break_minutes INT DEFAULT 0,

    -- 排班类型
    schedule_type VARCHAR(32) DEFAULT 'NORMAL',

    -- 状态
    schedule_status VARCHAR(32) DEFAULT 'SCHEDULED',

    -- 调班记录
    swap_requested_by BIGINT NULL,
    swap_approved_by BIGINT NULL,
    original_employee_id BIGINT NULL,

    notes VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_es_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_es_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_es_template FOREIGN KEY (shift_template_id) REFERENCES shift_templates(id),
    CONSTRAINT uk_es UNIQUE (employee_id, store_id, schedule_date),
    INDEX idx_es_store_date (store_id, schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**schedule_type：** `NORMAL` | `OVERTIME` | `SWAP`（换班）| `COVER`（代班）

**schedule_status：** `SCHEDULED` | `CONFIRMED` | `CANCELLED` | `SWAP_PENDING`

### 2.4 attendance_records（考勤记录）

```sql
CREATE TABLE attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    schedule_id BIGINT NULL,
    attendance_date DATE NOT NULL,

    -- 打卡时间
    clock_in_at TIMESTAMP NULL,
    clock_out_at TIMESTAMP NULL,
    clock_in_method VARCHAR(32) NULL,
    clock_out_method VARCHAR(32) NULL,

    -- 计算结果
    scheduled_minutes INT DEFAULT 0,
    actual_minutes INT DEFAULT 0,
    overtime_minutes INT DEFAULT 0,
    break_minutes INT DEFAULT 0,
    late_minutes INT DEFAULT 0,
    early_leave_minutes INT DEFAULT 0,

    -- 状态
    attendance_status VARCHAR(32) DEFAULT 'PENDING',

    -- 异常处理
    exception_type VARCHAR(32) NULL,
    exception_reason VARCHAR(255) NULL,
    approved_by BIGINT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ar_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_ar_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ar_schedule FOREIGN KEY (schedule_id) REFERENCES employee_schedules(id),
    CONSTRAINT uk_ar UNIQUE (employee_id, store_id, attendance_date),
    INDEX idx_ar_store_date (store_id, attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**clock_in_method：** `PIN`（POS 终端 PIN 打卡）| `MANUAL`（店长手动录入）| `SYSTEM`（开班次自动打卡）

**attendance_status：** `PENDING`（未打卡）| `CLOCKED_IN`（已上班）| `COMPLETED`（已完成）| `ABSENT`（缺勤）| `LEAVE`（请假）

**exception_type：** `LATE`（迟到）| `EARLY_LEAVE`（早退）| `NO_SHOW`（未到）| `FORGOT_CLOCK`（忘打卡）

### 2.5 leave_requests（请假申请）

```sql
CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_days DECIMAL(4,1) NOT NULL,
    reason VARCHAR(512) NULL,

    -- 审批
    request_status VARCHAR(32) DEFAULT 'PENDING',
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    reject_reason VARCHAR(255) NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_lr_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    INDEX idx_lr_employee_date (employee_id, start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**leave_type：** `ANNUAL`（年假）| `SICK`（病假）| `PERSONAL`（事假）| `UNPAID`（无薪假）| `MATERNITY`（产假）

**request_status：** `PENDING` | `APPROVED` | `REJECTED` | `CANCELLED`

### 2.6 leave_balances（假期余额）

```sql
CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(32) NOT NULL,
    year INT NOT NULL,
    entitled_days DECIMAL(4,1) DEFAULT 0,
    used_days DECIMAL(4,1) DEFAULT 0,
    carried_over_days DECIMAL(4,1) DEFAULT 0,

    CONSTRAINT fk_lb_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT uk_lb UNIQUE (employee_id, leave_type, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.7 payroll_periods（薪资周期）

```sql
CREATE TABLE payroll_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    period_code VARCHAR(32) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    period_status VARCHAR(32) DEFAULT 'OPEN',
    closed_at TIMESTAMP NULL,
    closed_by BIGINT NULL,

    CONSTRAINT fk_pp_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_pp UNIQUE (merchant_id, period_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**period_status：** `OPEN`（当期）| `CALCULATING`（计算中）| `PENDING_APPROVAL`（待审批）| `APPROVED`（已审批）| `PAID`（已发放）

### 2.8 payroll_records（薪资明细）

```sql
CREATE TABLE payroll_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,

    -- 工时
    scheduled_hours DECIMAL(6,2) DEFAULT 0,
    actual_hours DECIMAL(6,2) DEFAULT 0,
    overtime_hours DECIMAL(6,2) DEFAULT 0,

    -- 薪资计算
    base_pay_cents BIGINT DEFAULT 0,
    overtime_pay_cents BIGINT DEFAULT 0,
    allowance_cents BIGINT DEFAULT 0,
    deduction_cents BIGINT DEFAULT 0,
    bonus_cents BIGINT DEFAULT 0,
    gross_pay_cents BIGINT DEFAULT 0,

    -- 扣款明细
    cpf_employee_cents BIGINT DEFAULT 0,
    cpf_employer_cents BIGINT DEFAULT 0,
    tax_cents BIGINT DEFAULT 0,
    other_deduction_cents BIGINT DEFAULT 0,
    net_pay_cents BIGINT DEFAULT 0,

    -- 人效
    revenue_during_period_cents BIGINT DEFAULT 0,
    revenue_per_hour_cents BIGINT DEFAULT 0,

    -- 状态
    payroll_status VARCHAR(32) DEFAULT 'DRAFT',

    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_pr_period FOREIGN KEY (period_id) REFERENCES payroll_periods(id),
    CONSTRAINT fk_pr_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_pr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_pr UNIQUE (period_id, employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**人效计算：** `revenue_per_hour_cents = revenue_during_period_cents / actual_hours`
- 该员工工作期间门店营业额 ÷ 该员工实际工时 = 人效

### 2.9 employee_performance_log（绩效/事件记录）

```sql
CREATE TABLE employee_performance_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    event_date DATE NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    recorded_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_epl_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_epl_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_epl_employee (employee_id, event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**event_type：** `PRAISE`（表扬）| `WARNING`（警告）| `TRAINING`（培训）| `PROMOTION`（晋升）| `DEMOTION`（降职）| `INCIDENT`（事件）

---

## 3. 与 cashier_shifts 的集成

现有 `cashier_shifts` 表记录的是收银班次（开班/关班/现金对账）。和员工考勤的关系：

```
员工打卡上班（attendance_records.clock_in_at）
     ↓
开班次（cashier_shifts.opened_at）
     ↓
... 工作 ...
     ↓
关班次（cashier_shifts.closed_at）
     ↓
员工打卡下班（attendance_records.clock_out_at）
```

**集成方式：** 开班次时自动记录考勤打卡（如果当天还没打卡），关班次时自动记录下班打卡（如果还没打）。

在 `cashier_shifts` 加一个字段关联：

```sql
ALTER TABLE cashier_shifts ADD COLUMN employee_id BIGINT NULL AFTER cashier_staff_id;
ALTER TABLE cashier_shifts ADD COLUMN attendance_id BIGINT NULL AFTER employee_id;
```

---

## 4. 业务场景

### 4.1 排班

```
店长打开排班页
 → 看到本周日历视图
 → 每天每个班次模板（早班/午班/晚班）
 → 拖拽员工到班次格子
 → 系统检查：该员工当天是否已排班/是否请假
 → 保存 → employee_schedules 写入
```

### 4.2 打卡

```
员工到店
 → POS 终端输入 PIN
 → 系统：
   a. 验证 PIN（users.pin_hash）
   b. 查 employee_schedules 当天是否有排班
   c. 创建 attendance_records（clock_in_at = now）
   d. 如果比 scheduled_start 晚 → 记录 late_minutes
   e. 如果当天有 cashier_shift 要开 → 自动开班次
```

### 4.3 工时统计

```
每日营业结束后自动计算：
 → attendance_records.actual_minutes = clock_out - clock_in - break
 → overtime = actual_minutes - scheduled_minutes（如果 > 0）
 → 汇总到 payroll_records
```

### 4.4 人效报表

```
报表页选择时间段
 → payroll_records.revenue_during_period_cents（该员工班次期间营业额）
 → payroll_records.actual_hours（实际工时）
 → 人效 = 营业额 ÷ 工时
 → 排行：谁人效最高，谁最低
```

---

## 5. 新增表清单

| 表 | 用途 |
|----|------|
| employees | 员工档案（人事信息 + 薪资配置） |
| shift_templates | 班次模板（早班/午班/晚班） |
| employee_schedules | 排班表 |
| attendance_records | 考勤记录（打卡） |
| leave_requests | 请假申请 |
| leave_balances | 假期余额 |
| payroll_periods | 薪资周期 |
| payroll_records | 薪资明细（含人效） |
| employee_performance_log | 绩效/事件记录 |

**9 张新表 + 1 张改动（cashier_shifts 加 employee_id）**

**累计：76 (doc/68) + 9 = 85 张表**
