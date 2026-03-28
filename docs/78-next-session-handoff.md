# Next Session Handoff

**Date:** 2026-03-28
**From:** Claude Session (V20260328001 → V20260328010)
**Context:** 本 session context 已满，下一个 session 从这里接手

---

## Source of Truth

- **本地代码：** `/Users/ontanetwork/Documents/Codex/`
- **分支：** `codex/reservations-transfer-backend`
- **GitHub：** `jeff0052/POS` main 分支（已同步 @ da0ab1d）
- **AWS：** 54.237.230.5（跑的是旧版本，不自动同步）
- **版本号：** V20260328010
- **版本文件：** `/Users/ontanetwork/Documents/Codex/VERSION`

---

## 今天做了什么

### 代码
1. 全栈架构审查（8 CRITICAL, 12 HIGH）
2. 14 个 PR（安全/权限/班次/GTO/会员/促销/MCP/AI/Agent）
3. Cherry-pick 到 Codex 集成分支
4. Phase 1-3 robustness 修复（auth, webhook, pricing, refund, settlement）
5. 支付微服务拆分（pos-payment-service 子模块）
6. 平台管理后台（7 页）
7. V1 代码清理 + 统一到 v2mysql
8. CI/CD（GitHub Actions 6 jobs）
9. Image upload 系统（S3→改为本地存储，Tasks 1-10 完成，待部署）
10. AWS 部署（Docker 8 容器 + Nginx）

### 文档（今天新写的关键文档）
- `docs/65-milestone-and-roadmap-V20260328001.md` — 里程碑和路线图
- `docs/66-aurora-data-model-design.md` — 核心数据模型（自助餐/KDS/库存/推荐/外卖/候位）
- `docs/67-sku-centric-data-model.md` — SKU 三层模型（顾客/厨房/库存）
- `docs/68-rbac-data-model.md` — 统一用户 + 可自定义角色权限
- `docs/69-employee-management-data-model.md` — 员工全生命周期
- `docs/70-inventory-complete-data-model.md` — 库存完整链路（批次/采购/盘点/报损/调拨）
- `docs/71-crm-complete-data-model.md` — CRM 完整（优惠券/推荐/标签/画像/营销）
- `docs/72-points-and-balance-data-model.md` — 积分+储值独立体系
- `docs/73-channel-attribution-and-commission-data-model.md` — 渠道归因+分润
- `docs/74-mrd-v3.md` — MRD V3（AI 驱动餐饮系统）
- `docs/75-complete-database-schema.md` — 119 张表完整 DDL
- `docs/76-database-schema-readme.md` — Schema 导航
- `docs/77-updated-prd-v3.md` — PRD V3（15 个功能模块）

### 客户需求
- Aurora 客户 PRD：`/Users/ontanetwork/Documents/小航需求/65-aurora-restaurant-pos-detailed-prd.md`
- P0 gap analysis：10 项需求，4 项 0%，2 项 80%+

---

## 下一步待办（按优先级）

### 立即要做

#### 1. 补 17 个遗漏项（对比客户需求发现的 gap）

**P0 级别（必须补）：**
- **并台** — 两桌合成一桌，需要数据模型 + API
- **支付叠加规则** — 积分+储值+券能不能同时用、优先扣哪个、规则可配

**P1 级别：**
- 清台中间态（待清台状态）
- 支付失败重试+换支付方式
- 库存驱动促销（临期原料自动生成促销草案）
- 送货单 OCR 流程设计
- SOP 批量导入
- 巡店记录（区域经理打卡+问题登记+照片）
- 顾客反馈 / Wish List
- 动态二维码
- 报表自动摘要（接 AI Operator）
- 第三方对接日志表
- CCTV 事件表
- 不同规格 SOP 消耗差异
- 多店对比报表
- KDS 回退打印机
- 审计日志统一覆盖人工操作

#### 2. 开始写代码 — Phase 1 自助餐模式（Week 1-2）

按 `docs/65-milestone-and-roadmap-V20260328001.md` 的路线图：

| Task | Description | Est |
|------|-------------|-----|
| Buffet package model | 档位表 + 套餐-商品关联 + 价格规则 | 3d |
| Menu mode routing | QR 扫码选择"单点/自助" + 不同菜单展示 | 2d |
| Buffet timer | 开始计时 + 后台颜色提示 + 超时预警 | 2d |
| 套餐外加点 | 额外收费商品 + 差价计算 | 2d |
| 结账单据 | 套餐金额 + 加点金额分开显示 | 1d |

需要先把 `docs/66` 的 buffet 数据模型写成 Flyway migration（V070-V075）。

#### 3. Image Upload 收尾

Tasks 11-12（S3→本地存储切换 + 部署验证）还没做。需要：
- 把 S3ImageStorage 改成 LocalDiskStorage
- Docker volume 挂载 /data/founderpos/images
- 部署到 AWS 验证

---

## 关键架构决策（供下个 session 参考）

1. **SKU 是核心** — 三层模型（顾客/厨房/库存），所有链路交汇于 SKU
2. **价格不硬编码** — `sku_price_overrides` 四级 fallback：门店+场景 > 品牌+场景 > 门店基础价 > SKU 默认价
3. **积分分批过期** — `points_batches` FIFO，不是一个总数
4. **库存分批追踪** — `inventory_batches` FIFO + 保质期
5. **权限可自定义** — 商户自建角色 + 40+ 权限积木
6. **渠道可追踪** — 每笔订单归因 + 分润计算
7. **所有金额用分** — BIGINT cents
8. **auth_users + staff 合并为 users** — 一人一账号，密码+PIN 双登录

---

## 运行环境

### 本地开发
```bash
cd /Users/ontanetwork/Documents/Codex
# 后端
docker compose up -d pos-mysql pos-backend
# 前端
cd android-preview-web && npm run dev -- --port 5188
cd pc-admin && npm run dev -- --port 5187
cd qr-ordering-web && npm run dev -- --port 4183
cd platform-admin && npm run dev -- --port 5189
```

### AWS 生产
```bash
ssh -i ~/.ssh/founderPOS-aws.pem ec2-user@54.237.230.5
cd /home/ec2-user/founderpos
docker compose -f docker-compose.prod.yml up -d
```

### 版本号规范
`V{YYYYMMDD}{NNN}` — 每天从 001 开始递增

### Git 推送
```bash
cd /Users/ontanetwork/Documents/Codex
git add . && git commit -m "message"
git push origin codex/reservations-transfer-backend:main  # 推 GitHub
```

### 部署到 AWS（手动）
```bash
rsync -avz --exclude='.git' --exclude='node_modules' --exclude='target' \
  /Users/ontanetwork/Documents/Codex/ \
  -e "ssh -i ~/.ssh/founderPOS-aws.pem" \
  ec2-user@54.237.230.5:/home/ec2-user/founderpos/

ssh -i ~/.ssh/founderPOS-aws.pem ec2-user@54.237.230.5 \
  "cd /home/ec2-user/founderpos && docker compose -f docker-compose.prod.yml up -d --build"
```

---

## Memory 提醒

检查 `/Users/ontanetwork/.claude/projects/-Users-ontanetwork-Documents-Claude-Code/memory/` 里的 memory 文件，了解用户偏好和项目上下文。
