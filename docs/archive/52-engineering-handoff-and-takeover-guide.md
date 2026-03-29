# Engineering Handoff And Takeover Guide

## Purpose

This document is the operational handoff for the next engineer taking over the POS project after the recent demo push.

It focuses on:

- the current source-of-truth branch
- what is already working
- which branches are real vs stale
- which changes are safe to merge
- what should not be merged yet
- how to boot the current environments
- what work is still open

## Source Of Truth Right Now

The effective integration branch is:

- `codex/reservations-transfer-backend`

Latest important commits on that branch:

- `1df5ffd` `feat: finalize demo flows and admin operations foundations`
- `d780b97` `docs: add branch review and merge plan`
- `8507554` `fix: port safe critical review fixes`

This branch is no longer a narrow reservations-only branch. It is the current integration branch for the latest demo-ready product work.

## Branch Reality

### Real working branch

- `codex/reservations-transfer-backend`
  - contains the broadest product integration work
  - contains the demo flows used recently
  - contains the safe subset of critical backend fixes

### Branches that exist but are not the real source of truth

- `codex/crm-completion-pass`
- `codex-platform-admin-real-data`
- `codex-reports-deepening-pass`

These currently do not carry their own meaningful unique work beyond what already lives in the integration branch.

### Branch to treat carefully

- `codex/shift-handover-foundation`

This branch name does not cleanly match its content history. Do not assume it is the clean shift branch.

### Security / hardening branch

- `fix/critical-review-fixes`

This branch contains high-value ideas, but should **not** be merged as-is.

Reason:

- JWT / Spring Security is incomplete for the current frontends
- auth path alignment is not safe yet
- current frontends do not inject Bearer tokens
- CORS defaults in that branch were not aligned with the current demo ports

Instead, a safe subset has already been manually ported into `codex/reservations-transfer-backend`.

## Safe Critical Fixes Already Ported

The following fixes from `fix/critical-review-fixes` have already been ported into the integration branch:

1. QR pricing now uses server-side SKU prices, not client-submitted prices
2. Active table order lookup now supports pessimistic write locking for table-level order mutation
3. VibeCash webhook signature validation is implemented
4. Settlement idempotency has been improved with:
   - repository lookup by `active_order_id`
   - duplicate handling in service layer
   - database uniqueness constraint via migration
5. CORS has been tightened from `*` to an allow-list matching active local ports
6. SQL show-logging has been disabled in the MySQL profiles

Files:

- [pom.xml](/Users/ontanetwork/Documents/Codex/pos-backend/pom.xml)
- [CorsConfig.java](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/common/config/CorsConfig.java)
- [ActiveTableOrderApplicationService.java](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java)
- [JpaActiveTableOrderRepository.java](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/order/infrastructure/persistence/repository/JpaActiveTableOrderRepository.java)
- [CashierSettlementApplicationService.java](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java)
- [VibeCashPaymentApplicationService.java](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java)
- [JpaSettlementRecordRepository.java](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/repository/JpaSettlementRecordRepository.java)
- [application.yml](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/resources/application.yml)
- [V019__settlement_record_idempotency.sql](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/resources/db/migration/v2/V019__settlement_record_idempotency.sql)

## What Is Working

### Tablet / POS demo

- WebView POS shell
- table management
- ordering
- send to kitchen
- transfer table
- reservations
- payment collection demo UI
- discount / open discount / open amount demo logic
- promotion demo rule for spend-X-save-Y

### QR

- QR menu loading
- QR order submit
- shared V2 backend integration

### Merchant admin

- categories
- products
- SKU basics
- product publish / unpublish
- CRM basics
- reports first-pass redesign

### Backend

- reservations API
- table transfer API
- reports first-pass DTO/controller additions
- CRM/service groundwork
- safe critical hardening subset listed above

## Important Current Product Caveats

1. Android in the integration branch is currently a **WebView shell path for demo use**
- it is not a restored full native POS branch
- do not merge this into `main` blindly if the target is the long-term native app

2. Payment is not part of the mainline ownership anymore
- payment was handed off separately
- DCS integration reached host response level during testing
- current payment stabilization should be treated as a separate workstream

3. Reports are still not fully trustworthy as production business reporting
- the reports page was improved for demo/admin presentation
- reporting semantics still need deeper review before being treated as finance-grade output

## Current Running Environment

At the time of handoff, the active local ports are:

### Frontends

- POS / Tablet preview: `5188`
- QR ordering: `4183`
- Merchant admin: `5187`

### Backends

- Main V2 backend: `8098`
- Reservation variant backend: `8099`

### Database

- MySQL: `3306`

Typical local URLs:

- POS: [http://localhost:5188/](http://localhost:5188/)
- QR: [http://localhost:4183/](http://localhost:4183/)
- Admin: [http://localhost:5187/](http://localhost:5187/)

## How To Start The Current Stack

### Backend containers

Use Docker Compose in the repo root:

```bash
docker compose up -d pos-mysql pos-backend-v2-live pos-backend-v2-reservation
```

### Frontends

#### Tablet / POS preview

```bash
cd /Users/ontanetwork/Documents/Codex/android-preview-web
npm run dev -- --host 0.0.0.0 --port 5188
```

#### QR ordering

```bash
cd /Users/ontanetwork/Documents/Codex/qr-ordering-web
npm run dev -- --host 0.0.0.0 --port 4183
```

#### Merchant admin

```bash
cd /Users/ontanetwork/Documents/Codex/pc-admin
npm run dev -- --host 0.0.0.0 --port 5187
```

All three frontends currently expect `/api` to resolve to the V2 backend on `8098` by default.

## Validation Commands

### Backend build

```bash
docker compose build pos-backend
```

### Frontend builds

```bash
cd /Users/ontanetwork/Documents/Codex/android-preview-web && npm run build
cd /Users/ontanetwork/Documents/Codex/qr-ordering-web && npm run build
cd /Users/ontanetwork/Documents/Codex/pc-admin && npm run build
cd /Users/ontanetwork/Documents/Codex/platform-admin && npm run build
```

## What Should Not Be Merged Yet

Do **not** merge `fix/critical-review-fixes` wholesale yet.

Specifically hold these items until a dedicated auth pass is done:

- Spring Security global enforcement
- JWT path protection
- frontend token injection rollout
- any auth change that assumes `/api/auth/**` instead of the real `/api/v1/auth/**`

Also do not assume the current integration branch is ready for direct merge to `main` without another pass, because it bundles:

- Android WebView shell work
- demo UI refinements
- admin redesign
- reservation backend
- reports additions
- CRM groundwork
- platform work

This should still be treated as an integration branch, not a clean single-scope feature branch.

## Suggested Merge Strategy

1. Review and stabilize `codex/reservations-transfer-backend`
2. Decide whether to split out:
   - reservation / transfer backend
   - demo UI work
   - admin/report styling
   - Android WebView shell
3. Merge only after deciding whether `main` should remain native-first or demo-shell-first
4. Revisit `fix/critical-review-fixes` later and port the remaining auth pieces in a dedicated branch

## Highest Priority Open Work

1. Refund business flow
2. Kitchen / KDS
3. CRM deepening
4. Platform admin deepening
5. Reports deepening
6. Staff / Roles / Permissions
7. GTO tax export
8. Production hardening

Payment stabilization is being handled separately and should not block the rest of the product stream.

## Practical Advice For The Next Engineer

- Start from `codex/reservations-transfer-backend`, not from one of the stale placeholder branches
- Treat the current branch as an integration branch
- Keep demo stability separate from long-term architecture cleanup
- If you touch auth, do it in a fresh branch and update every frontend together
- If you touch reporting, re-check the business data source semantics before trusting output
- If you touch Android, be explicit whether the target is:
  - demo WebView shell
  - long-term native POS

## Last Known Git State

Current important branch:

- `codex/reservations-transfer-backend`

Latest safe-fix commit:

- `8507554` `fix: port safe critical review fixes`

Untracked local file intentionally left out of commits:

- `android-pos/app/libs/DCSPaymentLib-release-v1.0.6.aar`

Do not accidentally commit that old AAR during cleanup.
