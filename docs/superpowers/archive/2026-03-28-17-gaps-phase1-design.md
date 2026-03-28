# Phase 1 Release Audit And Review

## Review Verdict

Current verdict: `NOT APPROVED FOR RELEASE`

This repository is not in a releasable state yet. The blockers are not cosmetic. They are release-gating issues in:

- authorization boundaries
- payment/refund financial correctness
- payment-service completeness
- platform-admin deployment/runtime behavior
- production truthfulness of admin/ops surfaces

If these issues are not fixed before release, the most likely outcomes are:

- unauthenticated mutation of store state
- false refund success records without actual money return
- payment flows that cannot complete in production
- platform-admin routes failing on refresh/deep link
- operators seeing demo or mock data in production screens

## Scope

Audited modules:

- `pos-backend`
- `pos-payment-service`
- `pc-admin`
- `platform-admin`
- `qr-ordering-web`
- `android-preview-web`
- deployment files under `docker-compose.prod.yml` and `nginx/`

Not fully executed in this environment:

- Java test suite execution, because the audit environment currently has no Java runtime
- Android build/runtime validation

## Verification Performed

Builds executed successfully:

- `pc-admin`: `npm run build`
- `platform-admin`: `npm run build`
- `qr-ordering-web`: `npm run build`
- `android-preview-web`: `npm run build`

Observed build warnings:

- `pc-admin` production bundle is about `1.26 MB` minified
- `platform-admin` production bundle is about `993 KB` minified
- both should be considered for code-splitting, but this is not the primary release blocker

Could not execute:

- `pos-backend ./mvnw test`
- `pos-payment-service ./mvnw test`

Reason:

- local audit environment returned `Unable to locate a Java Runtime`

Existing automated test coverage in repo is also very limited:

- `pos-backend` tests cover MCP infrastructure only
- `pos-payment-service` has no test files under `src/test`
- frontend apps have no meaningful test suites

## Blocker Findings

### 1. P0: Core business APIs are publicly writable without authentication

Severity: `P0 / release blocker`

Evidence:

- [`pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java#L47)
- [`pos-backend/src/main/java/com/developer/pos/v2/store/interfaces/rest/TableOperationsV2Controller.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/store/interfaces/rest/TableOperationsV2Controller.java#L43)
- [`pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/TableSettlementV2Controller.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/TableSettlementV2Controller.java#L42)
- [`pos-backend/src/main/java/com/developer/pos/v2/member/interfaces/rest/MemberV2Controller.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/member/interfaces/rest/MemberV2Controller.java#L82)
- [`pos-backend/src/main/java/com/developer/pos/v2/report/interfaces/rest/ReportV2Controller.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/report/interfaces/rest/ReportV2Controller.java#L25)

What is wrong:

- `SecurityConfig` explicitly marks these paths as `permitAll`:
  - `/api/v2/stores/**`
  - `/api/v2/settlement/**`
  - `/api/v2/promotions/**`
  - `/api/v2/members/**`
  - `/api/v2/reports/**`
  - `/api/v2/shifts/**`
  - `/api/v2/ai/**`
- Those route groups are not read-only.
- Under `/api/v2/stores/**`, public callers can create reservations, update reservations, seat guests, and transfer tables.
- Under `/api/v2/stores/{storeId}/tables/{tableId}/payment`, public callers can move a table to payment pending, collect settlement, and create VibeCash attempts.
- Member write endpoints are also exposed behind a globally public matcher.

Why this blocks release:

- Anyone who can reach the backend can mutate live store state.
- This is not just “tablet convenience”; it is open write access to reservation, table, settlement, and member flows.
- Public financial and customer endpoints with write capability are unacceptable for production.

Required change for Claude:

- Redesign `SecurityConfig` so only truly public customer endpoints remain public:
  - QR browsing
  - QR submit, if intended
  - verified webhooks
  - static image serving if needed
- Move tablet/POS write APIs behind a real auth boundary.
- Add explicit role rules for merchant admin, platform admin, cashier/device, and webhook/internal callbacks.
- Add negative security tests proving anonymous callers cannot mutate reservations, settlements, members, or reports.

Acceptance criteria:

- Anonymous requests to reservation/table/payment/member write routes return `401` or `403`.
- Only intended device/admin roles can mutate those resources.
- Public routes are enumerated narrowly and justified one by one.

### 2. P0: Refund flow marks money as refunded without calling any payment provider

Severity: `P0 / release blocker`

Evidence:

- [`pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/RefundApplicationService.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/RefundApplicationService.java#L32)
- [`pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/RefundV2Controller.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/RefundV2Controller.java#L27)

What is wrong:

- `RefundApplicationService.createRefund()` never calls a payment provider, payment microservice, terminal SDK, or async refund workflow.
- It immediately writes:
  - `refund.setRefundStatus("COMPLETED")`
  - settlement `refundedAmountCents += refundAmount`
  - settlement `refundStatus = FULLY_REFUNDED / PARTIALLY_REFUNDED`
- This means the system can declare a refund finished even when no money has actually been returned.

Why this blocks release:

- Financial records diverge from the real world.
- Staff will believe a QR/card refund succeeded when the customer may never receive funds.
- This creates reconciliation risk, customer disputes, and accounting errors.

Required change for Claude:

- If only cash/manual refunds are supported in phase 1, then enforce that explicitly and reject non-cash refund attempts.
- If digital refunds are in scope, implement provider-backed refund orchestration with statuses such as:
  - `PENDING`
  - `PROCESSING`
  - `COMPLETED`
  - `FAILED`
- Only advance settlement refunded totals after provider confirmation.
- Persist provider refund identifiers and failure reasons.

Acceptance criteria:

- A digital refund cannot be marked completed without provider confirmation.
- Cash/manual refunds are either clearly modeled as manual, or the API rejects unsupported methods.
- Refund tests cover partial, full, duplicate, and provider-failure cases.

### 3. P1: `/auth/me` is broken for authenticated users

Severity: `P1 / high`

Evidence:

- [`pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java#L65)
- [`pos-backend/src/main/java/com/developer/pos/auth/security/JwtAuthFilter.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/auth/security/JwtAuthFilter.java#L33)
- [`pos-backend/src/main/java/com/developer/pos/auth/security/AuthenticatedActor.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/auth/security/AuthenticatedActor.java#L3)

What is wrong:

- `JwtAuthFilter` stores an `AuthenticatedActor` object as `Authentication.principal`.
- `AuthController.me()` then executes `Long.parseLong(auth.getPrincipal().toString())`.
- `AuthenticatedActor.toString()` is not a numeric user id, so this will fail at runtime.

Likely runtime result:

- authenticated `/api/v2/auth/me` requests will throw a `NumberFormatException`
- the request will fall into the global exception handler and surface as a server error

Why this matters:

- Session restoration and identity introspection are standard auth requirements.
- Even if the current frontends do not call `/me` yet, the API contract is already broken.

Required change for Claude:

- Read `AuthenticatedActor` from the principal and use `actor.userId()`.
- Add a controller/security test covering login -> authenticated `/me`.

Acceptance criteria:

- `/api/v2/auth/me` returns the correct user for a valid JWT.
- The endpoint has a regression test.

### 4. P1: `pos-payment-service` is not production-complete, but it is still wired into prod deployment

Severity: `P1 / high`

Evidence:

- [`pos-payment-service/src/main/java/com/developer/pos/payment/api/PaymentController.java`](/Users/ontanetwork/Documents/Codex/pos-payment-service/src/main/java/com/developer/pos/payment/api/PaymentController.java#L24)
- [`pos-payment-service/src/main/java/com/developer/pos/payment/config/SecurityConfig.java`](/Users/ontanetwork/Documents/Codex/pos-payment-service/src/main/java/com/developer/pos/payment/config/SecurityConfig.java#L49)
- [`pos-payment-service/src/main/java/com/developer/pos/payment/adapter/CashAdapter.java`](/Users/ontanetwork/Documents/Codex/pos-payment-service/src/main/java/com/developer/pos/payment/adapter/CashAdapter.java#L7)
- [`docker-compose.prod.yml`](/Users/ontanetwork/Documents/Codex/docker-compose.prod.yml#L42)

Repository evidence:

- The payment service exposes only:
  - create intent
  - get intent
  - cancel intent
  - health
- The security config explicitly reserves public paths for:
  - `/api/v1/payments/webhooks/**`
  - `/api/v1/payments/dcs/result`
- But there are no controllers implementing those routes in this module.
- The only provider adapter present is `CashAdapter`.
- Repo-wide search also found no caller in other modules sending `X-Service-Key` requests into this service.

Why this blocks release:

- If this microservice is part of the release architecture, digital payment completion cannot work end-to-end.
- External provider webhooks and Android terminal result callbacks have nowhere to land.
- Production deployment includes the service and public nginx routing for webhooks, which creates the impression that it is ready when it is not.

Required change for Claude:

- Make a hard decision:
  - either complete the payment microservice for phase 1
  - or remove it from the production path entirely
- “Complete” means:
  - implement actual VibeCash and/or DCS adapters
  - implement webhook/result controllers
  - implement signature verification and idempotency
  - implement backend caller integration with `X-Service-Key`
  - add tests for success, failure, duplicate callbacks, settlement retry

Acceptance criteria:

- Every route promised by security/nginx/docs exists and is covered by tests.
- At least one non-cash provider flow completes end-to-end, or the service is removed from release scope.

### 5. P1: platform-admin production image ignores its own nginx SPA config, so deep links/refreshes will fail

Severity: `P1 / high`

Evidence:

- [`platform-admin/src/main.tsx`](/Users/ontanetwork/Documents/Codex/platform-admin/src/main.tsx#L11)
- [`platform-admin/nginx.prod.conf`](/Users/ontanetwork/Documents/Codex/platform-admin/nginx.prod.conf#L7)
- [`platform-admin/Dockerfile.prod`](/Users/ontanetwork/Documents/Codex/platform-admin/Dockerfile.prod#L13)

What is wrong:

- The app is mounted under `BrowserRouter basename="/platform"`.
- The repo contains a custom nginx config intended to support `/platform/*`.
- But `Dockerfile.prod` never copies that config into the final image.
- The production image therefore uses default nginx behavior with no SPA fallback for routes like:
  - `/platform/login`
  - `/platform/dashboard`
  - `/platform/stores`

Why this blocks release:

- Direct navigation or browser refresh on any platform-admin route is likely to 404.
- This is a classic production-only break: build passes, local dev works, deployment fails on routed pages.

Required change for Claude:

- Copy the intended nginx config in the final image.
- Verify the root nginx proxy path and frontend basename agree.
- Add a deployment smoke check for:
  - `/platform/`
  - `/platform/login`
  - `/platform/dashboard`

Acceptance criteria:

- Refreshing any platform-admin route returns the SPA, not a 404.
- Container image contents match the intended nginx config.

## Major Product/Truthfulness Gaps

These may be acceptable only if they are explicitly out of scope for phase 1 and hidden from release users. If they remain visible in production, they are product integrity issues.

### 6. P2: platform-admin still contains large demo-only surfaces

Severity: `P2 / medium-high`

Evidence:

- [`platform-admin/src/pages/DevicesPage.tsx`](/Users/ontanetwork/Documents/Codex/platform-admin/src/pages/DevicesPage.tsx#L5)
- [`platform-admin/src/pages/PlatformUsersPage.tsx`](/Users/ontanetwork/Documents/Codex/platform-admin/src/pages/PlatformUsersPage.tsx#L5)
- [`platform-admin/src/pages/ConfigurationsPage.tsx`](/Users/ontanetwork/Documents/Codex/platform-admin/src/pages/ConfigurationsPage.tsx#L5)
- [`platform-admin/src/pages/SupportMonitoringPage.tsx`](/Users/ontanetwork/Documents/Codex/platform-admin/src/pages/SupportMonitoringPage.tsx#L5)
- [`pos-backend/src/main/java/com/developer/pos/v2/platform/application/service/PlatformDashboardService.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/platform/application/service/PlatformDashboardService.java#L22)

What is wrong:

- Multiple platform-admin pages render hardcoded arrays instead of real data.
- Backend dashboard metrics are also partially hardcoded:
  - `totalMerchants = 1`
  - `totalDevices = 0`
  - `systemStatus = "HEALTHY"`

Why this matters:

- A release user will interpret these as operational truth.
- This is worse than an obviously unfinished page because it looks legitimate while being fabricated.

Required change for Claude:

- Either wire these pages to real APIs now, or hide them from the release navigation until they are real.
- Remove fake health alerts and fake user/device records from production UI.

Acceptance criteria:

- Every visible platform-admin page is either backed by live data or explicitly marked unavailable/in development.

### 7. P2: merchant admin GTO page silently falls back to mock data on backend failure

Severity: `P2 / medium`

Evidence:

- [`pc-admin/src/api/config.ts`](/Users/ontanetwork/Documents/Codex/pc-admin/src/api/config.ts#L1)
- [`pc-admin/src/api/services/gtoService.ts`](/Users/ontanetwork/Documents/Codex/pc-admin/src/api/services/gtoService.ts#L22)

What is wrong:

- `USE_MOCK_API` is set to `false`.
- But `getGtoBatches()` catches any API failure and returns `mockApi.getGtoBatches()`.
- In production this means an outage, auth problem, or backend regression can be silently masked with fake GTO data.

Why this matters:

- Operational users will trust false exports/statuses.
- The UI will hide real production problems instead of surfacing them.

Required change for Claude:

- Remove silent mock fallback from production code paths.
- Show an error state instead.

Acceptance criteria:

- When the GTO API fails, the UI shows a failure state, not fabricated data.

## Additional Risks

### 8. P2: webhook signature verification is based on parsed JSON, not raw request body

Severity: `P2 / medium`

Evidence:

- [`pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java#L157)
- [`pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/VibeCashWebhookV2Controller.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/VibeCashWebhookV2Controller.java#L24)
- [`pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/internal/PaymentCallbackController.java`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/internal/PaymentCallbackController.java#L33)

What is wrong:

- Signature verification uses `payload.toString()` after JSON parsing.
- Real webhook providers usually sign the raw request body bytes.
- Re-serialization can change formatting and sometimes field order.

Why this matters:

- Provider webhooks may fail verification in production even when the provider sent the correct signature.

Required change for Claude:

- Verify against raw request body bytes, not a reserialized `JsonNode`.
- Add integration tests with captured webhook payloads.

### 9. P2: default backend config still points at `mock` profile with Flyway disabled

Severity: `P2 / medium`

Evidence:

- [`pos-backend/src/main/resources/application.yml`](/Users/ontanetwork/Documents/Codex/pos-backend/src/main/resources/application.yml#L7)

What is wrong:

- Default `spring.profiles.active` is `mock`
- default Flyway is disabled

Why this matters:

- The deployment path may override this correctly, but the repository default is still misleading and risky.
- Non-compose startup paths can drift away from production behavior.

Required change for Claude:

- Make runtime profile selection explicit from environment.
- Avoid repository defaults that imply a fake or partial runtime for the primary backend.

## Test Coverage Gaps

Current state:

- `pos-payment-service`: no tests
- frontend apps: effectively no tests
- `pos-backend`: tests exist only for MCP support classes, not auth, settlement, refunds, reports, or order flows

Required minimum before approval:

- auth integration tests
- security/authorization tests
- settlement and refund tests
- VibeCash webhook tests
- contract tests for merchant admin and platform admin critical APIs

## Release Decision

Decision: `REJECT / DO NOT RELEASE`

Primary reasons:

1. authorization is not safe
2. refund accounting is not financially safe
3. payment microservice is not complete enough to trust in production
4. platform-admin deployment/runtime path is not reliable
5. multiple operational surfaces still show fabricated data

## Exact Change Order I Want Claude To Follow

1. Lock down `pos-backend` route authorization first.
2. Fix refund semantics so the system cannot report money refunded unless money was actually refunded.
3. Decide whether payment microservice is in or out for phase 1, then implement or remove accordingly.
4. Fix platform-admin Docker/nginx routing and verify all `/platform/*` refresh paths.
5. Remove production mock fallbacks and hide demo-only pages that are not yet real.
6. Fix `/auth/me`.
7. Add automated tests around all of the above.

## Approval Condition

I will only approve release after:

- the P0 items are fully closed
- the P1 items are fully closed
- regression tests exist for auth, security, settlement, refund, and payment callback behavior
- visible production pages no longer rely on fabricated/demo data unless explicitly hidden from users
