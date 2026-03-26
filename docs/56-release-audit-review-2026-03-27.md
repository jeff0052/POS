# Release Audit Review

Date: 2026-03-27  
Reviewer: Codex  
Scope: current repository state under `/Users/ontanetwork/Documents/Codex`

## Executive Summary

Current recommendation: `BLOCK RELEASE`

Reason:

1. The project is still in internal-preview shape rather than release-ready shape.
2. Authentication and authorization are not implemented as a real production control plane.
3. Several public endpoints can directly mutate business state without sufficient protection.
4. Core money flows still contain correctness risks.
5. Frontend/backend contracts are not fully aligned, so a successful build does not mean the system is actually operable.

This repo does contain meaningful progress:

- V2 backend domain structure is much clearer than the old mixed V1/V2 state.
- Flyway migrations exist and are actively used for V2 schema evolution.
- Platform admin, refund flow, and reporting surfaces are taking shape.
- A basic CI pipeline now exists.

But in its current form, this codebase should be treated as:

- internal preview / engineering prototype
- not safe for production release
- not safe for any release that implies trusted auth, trusted payments, or trusted financial records

## Audit Method

This review was done by reading the current codebase, recent changes, and representative runtime/configuration files. I also ran `platform-admin` production build successfully. I could not run backend Maven tests in this environment because `mvn` is not installed and the repo does not include a Maven wrapper.

Files and areas reviewed included:

- backend entry/config: `pos-backend/pom.xml`, `application.yml`, controllers, services, DTOs
- frontend integration surfaces: `pc-admin`, `platform-admin`
- CI/CD and deployment files: `.github/workflows/ci.yml`, `docker-compose.yml`, `docker-compose.prod.yml`, `RUN.md`, `DEPLOY_AWS.md`
- recent business additions: refund flow, platform admin merchant hierarchy

## Release Decision

### Release Status

`DO NOT APPROVE FOR RELEASE`

### Minimum Gate To Reconsider Release

All Critical findings below must be fixed and re-reviewed before any production or pilot release. High findings should also be fixed before external pilot unless explicitly risk-accepted in writing.

---

## Critical Findings

### C1. No real backend auth / authorization boundary exists

Severity: Critical  
Decision impact: hard blocker

Evidence:

- `pos-backend/pom.xml` only includes `spring-security-crypto`, not `spring-boot-starter-security`, so there is no real HTTP security stack in place.
- `pos-backend/src/main/java/com/developer/pos/auth/service/AuthService.java:11-16` returns a hard-coded `"mock-jwt-token"`.
- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:36-38` returns a hard-coded user in `/me`.
- I did not find any `SecurityFilterChain`, `HttpSecurity`, request authorization, or token validation in backend code.

Why this is a blocker:

- Every business endpoint is effectively open from the server perspective.
- There is no trustworthy identity boundary between cashier, merchant admin, platform admin, AI tool, or anonymous caller.
- Any later business rule that depends on `cashierId`, `merchantId`, `storeId`, `operatorName`, or actor metadata is not enforceable.

Required fix:

1. Add real backend security middleware.
2. Define auth model per client surface: merchant admin, platform admin, QR guest, webhook, MCP tool caller.
3. Implement token issuance and token validation.
4. Enforce authorization on every mutable endpoint.
5. Add integration tests proving protected endpoints reject unauthenticated access.

Relevant files:

- `pos-backend/pom.xml:23-75`
- `pos-backend/src/main/java/com/developer/pos/auth/service/AuthService.java:11-16`
- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:16-39`

### C2. PC admin auth flow is broken end-to-end even before security

Severity: Critical  
Decision impact: hard blocker

Evidence:

- Frontend sends login to `/api/v2/auth/login` via `pc-admin/src/api/client.ts:3-4, 36-40` and `pc-admin/src/api/services/authService.ts:17-21`.
- Backend exposes login at `/api/v1/auth/login` in `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:16-29`.
- Backend returns `LoginResponse(token, user)` in `pos-backend/src/main/java/com/developer/pos/auth/dto/LoginResponse.java:3-6`.
- Frontend expects `AuthUser` directly in `pc-admin/src/api/services/authService.ts:12-21`.
- Frontend stores only user object, not token, in `pc-admin/src/auth/authStorage.ts:3-16`.
- Frontend request client never attaches auth headers in `pc-admin/src/api/client.ts:6-13`.

Why this is a blocker:

- Login path is mismatched.
- Login response shape is mismatched.
- Even if path and DTO were fixed, token handling is incomplete.
- Protected frontend routing is client-only state, not real security.

Required fix:

1. Unify auth route versioning.
2. Unify login response contract.
3. Persist token securely and attach it on authenticated requests.
4. Add `/me` based session bootstrap instead of trusting local storage alone.
5. Add an auth contract test between frontend and backend.

Relevant files:

- `pc-admin/src/api/client.ts:3-4, 36-40`
- `pc-admin/src/api/services/authService.ts:12-21`
- `pc-admin/src/auth/AuthContext.tsx:21-25`
- `pc-admin/src/auth/authStorage.ts:3-16`
- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:16-39`
- `pos-backend/src/main/java/com/developer/pos/auth/service/AuthService.java:11-16`
- `pos-backend/src/main/java/com/developer/pos/auth/dto/LoginResponse.java:3-6`

### C3. VibeCash webhook accepts unsigned payment state changes when webhook secret is unset

Severity: Critical  
Decision impact: hard blocker

Evidence:

- `application.yml` defaults `vibecash.webhook-secret` to blank at `pos-backend/src/main/resources/application.yml:22-26`.
- `VibeCashPaymentApplicationService.handleWebhook(...)` only checks signature if the secret is non-blank at `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java:157-166`.
- The same method triggers settlement on `"payment.succeeded"` at `:193-208`.
- Webhook endpoint is public at `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/VibeCashWebhookV2Controller.java:14-28`.

Why this is a blocker:

- A caller can potentially forge a payment success event and drive settlement state changes if webhook secret is absent.
- This is especially dangerous because this project also lacks a broader auth boundary.

Required fix:

1. Fail application startup if payment webhook integration is enabled without a webhook secret.
2. Reject unsigned webhook requests unconditionally in non-mock environments.
3. Add tests for valid and invalid signatures.
4. Consider replay protection and idempotency assertions around provider event IDs.

Relevant files:

- `pos-backend/src/main/resources/application.yml:22-26`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java:157-208`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/VibeCashWebhookV2Controller.java:14-28`

### C4. MCP endpoint is an unauthenticated generic business mutation interface

Severity: Critical  
Decision impact: hard blocker

Evidence:

- `McpEndpointController` exposes `/api/v2/mcp/tools/{toolName}/execute` at `pos-backend/src/main/java/com/developer/pos/v2/mcp/interfaces/McpEndpointController.java:51-76`.
- Request body can override `ActionContext` at `:61-64`, which means caller-controlled actor metadata.
- Registered tools include real business mutations:
  - `toggle_sku_availability` in `pos-backend/src/main/java/com/developer/pos/v2/mcp/tools/CatalogTools.java:50-58`
  - `create_promotion_draft` in `pos-backend/src/main/java/com/developer/pos/v2/mcp/tools/PromotionTools.java:50-88`

Why this is a blocker:

- This is effectively a public command bus for business state changes.
- Audit logs are not a substitute for authorization.
- Because the caller can inject context, the audit trail can be spoofed.

Required fix:

1. Remove public exposure of this endpoint before release, or put it behind strict service-to-service auth.
2. Do not allow caller-supplied actor context without verification.
3. Split query tools and action tools if this surface must remain.
4. Add allowlists, approval workflow, and server-side actor derivation.

Relevant files:

- `pos-backend/src/main/java/com/developer/pos/v2/mcp/interfaces/McpEndpointController.java:51-76`
- `pos-backend/src/main/java/com/developer/pos/v2/mcp/tools/CatalogTools.java:50-58`
- `pos-backend/src/main/java/com/developer/pos/v2/mcp/tools/PromotionTools.java:50-88`

### C5. POS active-order write flow trusts client-supplied price and item snapshot

Severity: Critical  
Decision impact: hard blocker

Evidence:

- Request DTO accepts `skuCode`, `skuName`, and `unitPriceCents` from client in `pos-backend/src/main/java/com/developer/pos/v2/order/interfaces/rest/request/ReplaceActiveTableOrderItemsRequest.java:17-24`.
- Service copies those client values directly into persisted order items in `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:130-139`.
- Totals are computed directly from those same client values at `:146-154`.

Why this is a blocker:

- A manipulated client can lower price, rename items, or otherwise corrupt order snapshots.
- This is a financial integrity issue, not just a UX issue.

Important nuance:

- QR submit path eventually re-prices new items from SKU base price using `toPricedQrItem(...)` at `ActiveTableOrderApplicationService.java:368-380`.
- But the POS replace-items endpoint still trusts client price entirely, which is unacceptable for a server of record.

Required fix:

1. Resolve item name/code/price from authoritative SKU records on the server.
2. Reject requests whose provided item metadata does not match current SKU definitions.
3. Recompute all order amounts server-side.
4. Add tests for tampered price and tampered SKU metadata.

Relevant files:

- `pos-backend/src/main/java/com/developer/pos/v2/order/interfaces/rest/request/ReplaceActiveTableOrderItemsRequest.java:17-24`
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:130-154`

### C6. Refund flow can over-refund under concurrency

Severity: Critical  
Decision impact: hard blocker

Evidence:

- `RefundApplicationService.createRefund(...)` reads the settlement, derives `maxRefundable`, then writes refund + updates aggregate in one transaction.
- There is no pessimistic lock, no optimistic version field, and no atomic conditional update around the refund total.
- See `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/RefundApplicationService.java:34-84`.

Why this is a blocker:

- Two concurrent refund requests can both pass validation and exceed collected amount.
- This directly corrupts financial state.

Required fix:

1. Lock settlement row for update or add optimistic versioning with retry.
2. Add database-side constraint/guard where possible.
3. Add concurrent integration tests.

Relevant files:

- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/RefundApplicationService.java:34-84`

---

## High Findings

### H1. Settlement collection does not validate collected amount against payable amount

Severity: High

Evidence:

- Request requires only a positive `collectedAmountCents` in `CollectCashierSettlementRequest.java:7-10`.
- Service writes `command.collectedAmountCents()` straight into settlement record for both table-flow and active-order-flow:
  - `CashierSettlementApplicationService.java:193-205`
  - `CashierSettlementApplicationService.java:251-261`

Why this matters:

- Underpayment can still produce `SETTLED`.
- Overpayment/change handling is not modeled.
- Payment-method-specific validation is absent.

Required fix:

1. Enforce `collectedAmountCents >= payableAmountCents` for cash.
2. Enforce exact provider-confirmed amount for digital flows.
3. Define explicit business behavior for overpayment and change.

Relevant files:

- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/request/CollectCashierSettlementRequest.java:7-10`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:193-205`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:251-261`

### H2. Local compose/run flow is internally inconsistent and likely broken

Severity: High

Evidence:

- `docker-compose.yml` creates MySQL database `pos_db` at `docker-compose.yml:6-8`.
- Same file points backend to `pos_v2_db` at `docker-compose.yml:28-32`.
- `mysql/init/001-init.sql` seeds V1-style tables like `product_categories`, `products`, `orders`, `qr_table_orders` rather than the V2 Flyway schema.
- `RUN.md:32-41` instructs developers to use `docker compose up --build` as the normal startup path.

Why this matters:

- Local dev and verification can fail or drift depending on which schema actually exists.
- A broken local bootstrap path dramatically reduces trust in manual QA and release verification.

Required fix:

1. Make local compose use exactly one intended DB name.
2. Decide whether local startup is Flyway-first V2 or legacy seed-first demo mode.
3. Remove or isolate obsolete init SQL if V2 is the source of truth.
4. Update `RUN.md` accordingly.

Relevant files:

- `docker-compose.yml:6-8, 28-32`
- `mysql/init/001-init.sql:1-96`
- `RUN.md:32-41`

### H3. Merchant hierarchy migration is structurally incomplete

Severity: High

Evidence:

- `V055__merchant_hierarchy.sql` adds `stores.brand_id` and `stores.country_id` as nullable columns and only adds indexes.
- There are no foreign keys, no backfill, and no not-null migration plan.
- See `pos-backend/src/main/resources/db/migration/v2/V055__merchant_hierarchy.sql:32-37`.

Why this matters:

- Existing stores remain detached from the new hierarchy.
- Invalid `brand_id` / `country_id` values can be stored.
- Counts by brand and country can be misleading.

Required fix:

1. Define data migration/backfill strategy.
2. Add FK constraints once backfill is complete.
3. Decide whether `brand_id` and `country_id` are mandatory.
4. Add release migration validation for historical data.

Relevant files:

- `pos-backend/src/main/resources/db/migration/v2/V055__merchant_hierarchy.sql:32-37`

### H4. `pc-admin` and backend API contracts are not reliably aligned

Severity: High

Representative examples:

1. Refunds:
   - Frontend expects `{ list: [{ refundId, orderNo, status }] }` in `pc-admin/src/api/services/refundService.ts:6-15, 22-32`
   - Backend returns `Page<RefundRecordDto>` with fields like `id`, `settlementNo`, `refundStatus` in `RefundRecordDto.java:5-20`

2. Dashboard recent orders:
   - Frontend expects `paidAmountCents`, `cashier`, `printStatus`, `id` in `pc-admin/src/api/services/dashboardService.ts:6-24, 52-72`
   - Backend `MerchantAdminOrderDto` exposes `orderId`, `payableAmountCents`, no `cashier`, no `printStatus`, no numeric `id` in `MerchantAdminOrderDto.java:6-24`

Why this matters:

- The UI may compile while still failing at runtime or showing `NaN` / missing data.
- These mismatches are exactly the kind of issues that escape build-only CI.

Required fix:

1. Introduce explicit API contract snapshots or shared schemas.
2. Audit every frontend service against current backend DTOs.
3. Add integration smoke tests for key admin pages.

Relevant files:

- `pc-admin/src/api/services/refundService.ts:6-32`
- `pc-admin/src/api/services/dashboardService.ts:6-24, 52-72`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/dto/RefundRecordDto.java:5-20`
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/dto/MerchantAdminOrderDto.java:6-24`

---

## Medium Findings

### M1. Dashboard “real metrics” implementation is mostly hard-coded

Severity: Medium

Evidence:

- `PlatformDashboardService` only reads `totalStores`; other metrics are hard-coded:
  - `totalMerchants = 1`
  - `activeStores = totalStores`
  - `totalDevices = 0`
  - `systemStatus = "HEALTHY"`
- See `pos-backend/src/main/java/com/developer/pos/v2/platform/application/service/PlatformDashboardService.java:20-28`.

Why this matters:

- The code and the commit message suggest real ops visibility, but current output is partially synthetic.
- This can mislead platform operators and product stakeholders.

### M2. Merchant hierarchy UI is not complete through the Store level

Severity: Medium

Evidence:

- `platform-admin/src/pages/MerchantsPage.tsx` defines `Level = "merchants" | "brands" | "countries" | "stores"` but only renders through countries.
- Subtitle claims `Merchant → Brand → Country → Store`.
- `PlatformAdminController` only adds merchants / brands / countries routes.
- There is a separate `PlatformStoreV2Controller`, but it is not wired into this hierarchy drill-down.

Relevant files:

- `platform-admin/src/pages/MerchantsPage.tsx:7, 61-63, 91-103`
- `pos-backend/src/main/java/com/developer/pos/v2/platform/interfaces/rest/PlatformAdminController.java:36-77`
- `pos-backend/src/main/java/com/developer/pos/v2/platform/interfaces/rest/PlatformStoreV2Controller.java:13-26`

### M3. Test coverage is far below release confidence threshold

Severity: Medium

Evidence:

- Backend test tree currently contains only 4 files, all MCP-related:
  - `pos-backend/src/test/java/com/developer/pos/v2/mcp/tools/McpToolRegistryTest.java`
  - `pos-backend/src/test/java/com/developer/pos/v2/mcp/ActionContextHolderTest.java`
  - `pos-backend/src/test/java/com/developer/pos/v2/mcp/ActionContextAuditListenerTest.java`
  - `pos-backend/src/test/java/com/developer/pos/v2/mcp/McpToolRegistryIntegrationTest.java`
- Frontend package scripts contain only `dev`, `build`, `preview`; no `test`, no `lint`:
  - `pc-admin/package.json:6-9`
  - `platform-admin/package.json:6-9`

Why this matters:

- There is no meaningful automated coverage for orders, settlement, refunds, promotions, members, auth, or platform admin flows.

### M4. CI coverage is incomplete

Severity: Medium

Evidence:

- `.github/workflows/ci.yml` builds backend, `android-preview-web`, `pc-admin`, and `qr-ordering-web`.
- `platform-admin` is not included.

Relevant files:

- `.github/workflows/ci.yml:9-86`

### M5. Some frontend services still hard-wire mock behavior even with mock mode disabled

Severity: Medium

Evidence:

- `pc-admin/src/api/services/gtoService.ts:5-10` always returns mock data.
- `pc-admin/src/api/services/memberService.ts:38-43` always returns mock member tiers.

Why this matters:

- This hides backend incompleteness and makes demo behavior diverge from real integration behavior.

### M6. Default project posture is still preview-oriented

Severity: Medium

Evidence:

- Default Spring profile is `mock` and Flyway is disabled by default in `application.yml:7-10`.
- `RUN.md` and `DEPLOY_AWS.md` both describe internal preview / internal testing rather than release-hardened operation.

This is not inherently wrong, but it means release expectations and repository posture are still misaligned.

---

## Positive Notes

These are real strengths worth preserving while fixing the blockers:

1. V2 module layout in backend is significantly clearer than a flat mixed-domain structure.
2. Global exception handler exists and already covers several common bad-request / conflict paths.
3. Refund flow at least introduces explicit persistence, status tracking, and a dedicated API surface rather than burying refunds inside settlement mutation.
4. Merchant hierarchy work is directionally sound and a good foundation for future platform admin capabilities.
5. CI introduction is a step forward even though it is not complete.

---

## Recommended Fix Order For Claude

### Phase 1: Release blockers

1. Implement real auth/authz on backend.
2. Fix PC admin auth contract end-to-end.
3. Lock down MCP execution endpoint or remove it from public runtime.
4. Enforce signed VibeCash webhook behavior.
5. Re-price all mutable order inputs server-side.
6. Fix refund concurrency protections.
7. Add settlement amount validation.

### Phase 2: Integration correctness

1. Reconcile every frontend service DTO with backend DTOs.
2. Fix refund page integration.
3. Fix dashboard recent orders integration.
4. Remove hard-coded mock fallbacks where real integration is intended.
5. Complete or scope down platform hierarchy UX.

### Phase 3: Operational hardening

1. Fix local compose/bootstrap path.
2. Add platform-admin to CI.
3. Add backend wrapper or standardize dev tooling.
4. Add test and lint scripts for frontend.
5. Add backend integration tests for orders, settlement, refunds, auth.

---

## Suggested Verification Checklist After Fixes

Claude should not ask for release approval again until the following are demonstrated:

1. Unauthenticated requests to protected endpoints fail.
2. Login works end-to-end with the real route and real response contract.
3. Auth token is persisted and attached on subsequent requests.
4. Forged/unsigned payment webhook is rejected.
5. Tampered order price request is rejected or re-priced correctly.
6. Concurrent refund test proves no over-refund.
7. Settlement rejects invalid collected amounts.
8. `pc-admin` refund page renders correctly against the real refund API.
9. `pc-admin` dashboard renders correctly against the real orders API.
10. `docker compose up --build` or an updated documented local flow works from a clean machine.
11. CI covers every shipped frontend and the backend.

---

## Reviewer Conclusion

The repo is promising, but it is not release-safe yet.

My reviewer position is:

- I would approve this codebase for continued internal engineering iteration.
- I would not approve it for external pilot, merchant rollout, or any release that implies trusted payments or trusted financial records.

If Claude is going to act on this review, he should treat the auth boundary, webhook hardening, MCP exposure, pricing authority, and refund concurrency as non-negotiable first fixes.
