# Release Re-Review

Date: 2026-03-27  
Reviewer: Codex  
Scope: post-fix re-review after Claude remediation work in `/Users/ontanetwork/Documents/Codex`

## Executive Summary

Current recommendation: `BLOCK RELEASE`

This re-review is materially better than the first audit in `56-release-audit-review-2026-03-27.md`.

Several major blockers from the previous pass were actually addressed:

1. backend HTTP auth and role gating now exist
2. `pc-admin` now persists token and attaches `Authorization: Bearer`
3. VibeCash webhook no longer accepts blank-secret unsigned events
4. POS replace-items flow now re-prices from server-side SKU data
5. refund flow now uses a pessimistic lock when loading settlement records

However, release cannot yet be approved. The remaining issues are narrower, but still serious enough to block any production or pilot rollout:

1. `platform-admin` does not implement the auth flow required by the backend
2. the auth migration seeds known default credentials (`admin123`)
3. JWT secret is now required for startup, but deployment/runtime wiring was not updated
4. the old settlement collect endpoint still bypasses the new amount validation

I also found several medium-severity issues introduced or left behind during the fix pass:

1. CSP was loosened to a nearly no-op policy
2. security-related exceptions still map to the wrong HTTP statuses
3. one commit claims to change platform CSP but actually deletes `marketing/index.html`

Release posture after this re-review:

- previous status: broad systemic release block
- current status: focused release block with 4 remaining high-severity gates
- conclusion: not releasable yet, but much closer than the previous revision

## Review Scope

I reviewed the recent fix commits and the current repository state, focusing on the issues previously called out in document 56.

Commits reviewed included:

- `babf7a9` - Phase 1 release blockers fix
- `be13219` - exception handler, refund validation, pessimistic lock
- `0a18986` - platform admin pages and robustness changes
- `c18b2e1` - CSP change in frontend entry pages
- `caa33c5` - commit message says platform-admin CSP removal
- `87390f2` - platform admin router basename fix

Verification performed:

- `git status --short` -> clean working tree
- `npm run build` in `pc-admin` -> pass
- `npm run build` in `platform-admin` -> pass
- backend compile/test verification could not be performed because `mvn` is not installed in this environment and the repo still does not include `mvnw`

## What Was Successfully Fixed

### F1. Backend auth boundary now exists

Evidence:

- `pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java`
- `/api/v2/platform/**` now requires `PLATFORM_ADMIN`
- `/api/v2/mcp/**` now requires `ADMIN` or `PLATFORM_ADMIN`
- everything else defaults to authenticated except the explicitly public surfaces

Assessment:

This is a major improvement over the previous state. The project now has a real request security boundary instead of only mock auth responses.

### F2. `pc-admin` auth flow is mostly closed end-to-end

Evidence:

- `pc-admin/src/api/client.ts` now loads token from storage
- authenticated requests now attach `Authorization: Bearer ...`
- `401` clears auth state and redirects to `/login`

Assessment:

This resolves the biggest contract gap from the previous review for the merchant admin frontend.

### F3. VibeCash webhook now rejects unsigned events when secret is missing or invalid

Evidence:

- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java`
- `handleWebhook(...)` now throws if `webhookSecret` is blank
- invalid signature now throws instead of silently continuing

Assessment:

This closes one of the most dangerous money-state mutation problems from the previous audit.

### F4. POS replace-items flow no longer trusts client price

Evidence:

- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java`
- `replaceItems(...)` now looks up SKU by `skuId`
- persisted price/name/code snapshots now come from server-side SKU data

Assessment:

This is the correct server-of-record direction and removes a direct client-side amount tampering path.

### F5. Refund flow now uses a lock when loading the settlement

Evidence:

- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/RefundApplicationService.java`
- `JpaSettlementRecordRepository.findByIdForUpdate(...)`

Assessment:

This is the right fix direction for the previously reported concurrent over-refund risk.

---

## Remaining High-Severity Findings

### H1. `platform-admin` is incompatible with the backend auth model

Severity: High  
Release impact: blocker

Evidence:

- backend requires `PLATFORM_ADMIN` for `/api/v2/platform/**` in `pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java`
- `platform-admin/src/api/client.ts` never attaches an `Authorization` header
- `platform-admin/src/router/AppRouter.tsx` has routes only; there is no login route, auth guard, or session bootstrap
- repository search of `platform-admin/src` shows no login, token, or auth implementation

Why this still blocks release:

The backend is now correctly locked down, but the platform frontend was not updated to match. In a real environment, platform pages that call `/api/v2/platform/**` will fail authorization. This is not just missing polish; it means the platform admin product is not actually operable against the secured backend.

Required fix:

1. add platform-admin login page and authenticated session model
2. persist token and attach `Authorization: Bearer`
3. add route guard and `/auth/me` bootstrap flow
4. verify platform-only role enforcement with an actual platform admin user

### H2. Auth migration seeds known default passwords into the database

Severity: High  
Release impact: blocker

Evidence:

- `pos-backend/src/main/resources/db/migration/v2/V060__auth_users.sql`
- migration seeds:
  - `admin / admin123`
  - `store_admin / admin123`
  - `cashier / admin123`

Why this still blocks release:

A release artifact must not create known live credentials by default. Anyone with environment access, leaked docs, or source access immediately knows the initial passwords. Even if operators are "supposed" to change them later, this is not a safe default for a shipped system.

Required fix:

1. remove hard-coded seeded passwords from release migration path
2. if bootstrap users are required, generate them from environment variables or a one-time setup command
3. add forced password rotation or disable seeded accounts by default
4. document the operational bootstrap path explicitly

### H3. JWT secret is mandatory now, but runtime/deployment wiring was not updated

Severity: High  
Release impact: blocker

Evidence:

- `pos-backend/src/main/java/com/developer/pos/auth/security/JwtProvider.java` throws if secret is blank or shorter than 32 chars
- `pos-backend/src/main/resources/application.yml` still defaults `auth.jwt.secret` from `${JWT_SECRET:}`
- repository search found no `JWT_SECRET` wiring in:
  - `docker-compose.yml`
  - `docker-compose.prod.yml`
  - `.env.example`
  - `DEPLOY_AWS.md`
  - `RUN.md`

Why this still blocks release:

The security fix is correct, but the operational contract is incomplete. Based on the current repo state, existing documented startup paths are very likely broken because the app now requires a secret that the deployment/configuration layer does not provide.

Required fix:

1. add `JWT_SECRET` to local and production runtime config
2. update `.env.example`
3. update deployment docs and runbook
4. fail fast with a clear operator-facing startup message if the secret is missing

### H4. Legacy settlement collect path still bypasses the new amount validation

Severity: High  
Release impact: blocker

Evidence:

- `CashierSettlementApplicationService.collectForTable(...)` validates collected amount against payable amount
- `CashierSettlementApplicationService.collect(...)` still writes `command.collectedAmountCents()` directly to the settlement record
- `CashierSettlementV2Controller` still exposes `POST /api/v2/cashier-settlement/{activeOrderId}/collect`

Why this still blocks release:

The money-integrity fix is only partial. Any client or integration still calling the legacy endpoint can bypass the new rules. As long as the old entry point is live, the issue is not fully resolved.

Required fix:

1. either delete the old endpoint or route it through the same validation logic
2. ensure both cash and digital collection rules are enforced consistently
3. add tests that hit both entry paths and assert identical amount validation

---

## Medium-Severity Findings

### M1. CSP was weakened to an almost unrestricted policy

Severity: Medium  
Release impact: should fix before external exposure

Evidence:

- `platform-admin/index.html`
- `qr-ordering-web/index.html`
- `android-preview-web/index.html`

Current policy pattern:

- `default-src * 'self' 'unsafe-inline' 'unsafe-eval' data: blob:;`

Why this matters:

This effectively removes most CSP value. It may have been added to unbreak local/frontend tooling, but as a release posture it is much too permissive, especially after introducing real auth tokens into the system.

Required fix:

1. tighten CSP to actual required origins and directives
2. avoid `unsafe-eval` unless there is a proven unavoidable dependency
3. keep dev and prod CSP separate if necessary

### M2. Security-related failures still return misleading HTTP status codes

Severity: Medium  
Release impact: should fix before broad testing

Evidence:

- `VibeCashPaymentApplicationService.handleWebhook(...)` throws `SecurityException`
- `pos-backend/src/main/java/com/developer/pos/common/config/GlobalExceptionHandler.java` has no explicit `SecurityException` handler
- `/auth/me` throws `IllegalStateException("Not authenticated")` in `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java`
- `GlobalExceptionHandler` maps `IllegalStateException` to HTTP `409`

Why this matters:

Invalid signatures and unauthenticated access should not look like generic server failures or conflict states. This creates confusing client behavior, noisy monitoring, and weakens contract clarity for frontend and integration consumers.

Required fix:

1. map `SecurityException` to `401` or `403`
2. return `401` for unauthenticated `/auth/me`
3. add endpoint tests for those error contracts

### M3. Commit `caa33c5` has suspicious collateral damage

Severity: Medium  
Release impact: investigate before release notes / branch approval

Evidence:

- commit message says: `fix: remove CSP from platform admin - internal system doesn't need it`
- `git show caa33c5` shows deletion of `marketing/index.html`

Why this matters:

This is either:

1. an accidental deletion that creates a silent product regression, or
2. an intentional change with an incorrect commit description

Either case is a review problem. Release history and audit trail need to accurately reflect what changed.

Required fix:

1. confirm whether `marketing/index.html` was intentionally removed
2. restore it if removal was accidental
3. if intentional, update commit narrative and release notes

---

## Claude Next-Round Fix Checklist

This is the minimum next pass I would ask Claude to implement before requesting another release review:

1. implement full auth flow in `platform-admin`
2. remove hard-coded seeded passwords from `V060__auth_users.sql`
3. add `JWT_SECRET` to compose/env/docs and document bootstrap requirements
4. unify settlement amount validation across both collect endpoints
5. replace current CSP with environment-specific, minimally permissive policies
6. add explicit security/unauthenticated exception mapping
7. investigate and correct the `marketing/index.html` deletion

## Re-Review Gate

I will re-open release approval only after all of the following are true:

1. all four High findings above are fixed in code
2. backend startup path is documented and reproducible with required secrets present
3. `platform-admin` can authenticate and successfully call secured platform APIs
4. legacy and new settlement collection paths share the same amount validation
5. seeded default credentials are removed from release migration path
6. backend compile or test verification becomes possible with either `mvn` installed or a committed Maven wrapper

## Final Reviewer Position

This repository is now in a meaningfully better state than the first audit. The direction of Claude's fixes is mostly correct.

But the current branch still does not meet release approval bar.

Decision: `BLOCK RELEASE`

Reason:

- the remaining problems are no longer broad architectural concerns
- they are now concrete, fixable, release-gating defects
- once those are closed, the project should be ready for another serious approval pass
