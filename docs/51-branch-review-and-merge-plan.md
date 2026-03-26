# Branch Review And Merge Plan

## Current Reality

This document records the actual branch state after the demo build-out, so review and merge work can continue from facts instead of the earlier parallel plan assumptions.

## Branch Status Summary

| Branch | Ahead of `main` | Current Role | Notes |
| --- | ---: | --- | --- |
| `codex/reservations-transfer-backend` | 2 commits | Current integration branch | Contains reservations + transfer API plus the larger demo/admin/backend integration work landed during demo preparation. This is now the most complete branch. |
| `codex/crm-completion-pass` | 0 commits | Stale placeholder | No unique commits relative to `main`. CRM-related code is currently represented in the integration branch rather than this branch. |
| `codex-platform-admin-real-data` | 0 commits | Stale placeholder | No unique commits relative to `main`. Platform admin real-data work is currently represented in the integration branch. |
| `codex-reports-deepening-pass` | 0 commits | Stale placeholder | No unique commits relative to `main`. Reports changes are currently represented in the integration branch. |
| `codex/shift-handover-foundation` | 1 commit | Misaligned branch | This branch name suggests shift work, but its unique diff is reservation/transfer backend code. It should not be treated as the source-of-truth shift branch. |
| `fix/critical-review-fixes` | 4 commits | Security / hardening branch | Contains high-value fixes, but it should not be merged directly without a focused review because JWT, auth paths, and CORS alignment still need care. |

## What The Integration Branch Actually Contains

`codex/reservations-transfer-backend` currently bundles these areas together:

- Reservations + transfer table backend APIs
- Tablet / WebView demo shell and UI refinements
- Merchant admin visual refresh and feature pass
- Product / SKU / publish-unpublish improvements
- CRM detail/edit groundwork
- Platform admin store real-data pass
- Reports redesign and report DTO/controller additions
- Shift foundation DTO/controller/service additions
- Android WebView shell, app icon, and DCS-related integration work
- Updated roadmap/progress docs

This means it is no longer a narrow reservations-only branch. It is the effective integration branch for the recent demo push.

## Review Priority

### 1. Review `codex/reservations-transfer-backend` first

Reason:
- It is the only branch that actually contains the broad product work completed for the demo.
- It includes the live demo refinements that were already build-validated.
- It currently supersedes the stale CRM / Platform / Reports placeholders.

### 2. Hold `codex/crm-completion-pass`, `codex-platform-admin-real-data`, and `codex-reports-deepening-pass`

Reason:
- They have no unique commits.
- They should either be retired or recreated later from fresh branch cuts after the integration branch is merged.

### 3. Treat `codex/shift-handover-foundation` as invalid naming, not as the shift source-of-truth

Reason:
- Its unique diff is reservation/transfer code, not shift-specific work.
- Do not merge it as “shift work.”

### 4. Review `fix/critical-review-fixes` separately after product integration stabilizes

Reason:
- It contains important fixes for security, QR price validation, webhook signature verification, locking, and idempotency.
- It also introduces auth/security behavior that can break current flows if merged carelessly.

## Recommended Merge Order

1. `codex/reservations-transfer-backend`
2. Re-cut feature branches from updated `main` if CRM / Platform / Reports / Shift still need isolated follow-up work
3. `fix/critical-review-fixes` after explicit auth/CORS/frontend-token review

## Review Checklist For `codex/reservations-transfer-backend`

When reviewing this branch, explicitly check:

1. Demo-critical flows still work
- Tablet / WebView POS
- QR ordering
- Reservations
- Transfer table
- Payment collection demo flow

2. Product / catalog rules still behave correctly
- Product publish/unpublish
- SKU status and availability
- QR/preview menu filtering

3. Merchant admin regressions
- Categories save
- Products save
- CRM page loads and updates
- Reports page renders correctly

4. Backend additions are coherent
- Reservation API
- Transfer API
- Report DTO/controller additions
- Platform store overview additions
- Shift foundation additions

5. Android shell remains installable
- App icon
- `UNIWEB POS` naming
- WebView startup path

## Follow-Up Cleanup After Merge

After the integration branch is merged:

- delete or archive stale placeholder branches
- recreate isolated branches only for unfinished modules
- split remaining work by real scope, not by the older branch names
- re-evaluate whether `fix/critical-review-fixes` should be merged whole or cherry-picked in parts
