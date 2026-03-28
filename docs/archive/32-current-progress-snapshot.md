# Current Progress Snapshot

## Snapshot Date
- 2026-03-26

## Overall Progress
- Overall project progress: **65% - 70%**
- `Stage 0 Prototype and Validation`: **Completed**
- `Stage 1 Store Transaction MVP`: **80% - 85%**
- `Stage 2 Merchant Operations Expansion`: **40%**
- `Stage 3 Platform Control Center`: **20%**
- `Stage 4 Production Hardening`: **10%**

## Core Module Progress
- Ordering: **100%**
- QR Ordering: **90%**
- Cashier Settlement: **85%**
- Member Foundation: **75%**
- Promotion Foundation: **65%**
- Merchant Admin V2 Bridge: **75%**
- SKU / Catalog Admin Foundation: **85%**
- Reports Foundation: **50%**
- Platform Admin Skeleton: **30%**
- Native Android: **60%**
- Reservations / Transfer Table Preview Flow: **70%**

## What Is Already Real
- POS ordering, send-to-kitchen, multi-round ordering, and unified payment
- QR ordering with direct kitchen submission
- POS and QR running on the same V2 transaction backbone
- Member create, search, bind, unbind, and base discount
- Member recharge, points adjustment, and ledgers
- Minimal promotion rule create and update
- Settlement preview
- Merchant admin reading real V2 data for key areas
- Merchant admin category and product save flow running on V2 catalog admin APIs
- Merchant admin product configuration now supports attribute groups, modifier groups, and combo slots
- QR ordering and POS preview can read SKU configuration and place configured items with remarks
- Platform admin running as a standalone frontend skeleton
- Native Android V2 bridge connected at code level for home, orders, order detail, and payment entry
- Native Android debug APK compile verification completed
- Native Android WebView shell APK is installable and usable as a tablet ordering shell
- Reservations and Transfer Table are now usable in preview flow

## What Is Not Done Yet
- Kitchen / KDS
- Shift handover
- Refund V2 mainline
- Full native runtime smoke test and provider-grade payment validation
- Richer reports and operational KPIs
- Reservations / Transfer Table backend V2 formalization
- CRM detail / edit / tier rules completion
- Platform admin real data integration
- Production hardening

## Current Assessment
The project has moved out of prototype mode and into structured V2 delivery. The store transaction backbone remains the strongest area, and the project has now crossed into the stage where SKU configurability, tablet preview completeness, CRM usability, and platform/runtime hardening determine the remaining execution path.

## Delivery Notes
- Ordering is treated as complete within the current scope: POS ordering, QR ordering, send to kitchen, multi-submission flow, payment, and collect payment are all running on the V2 transaction backbone.
- Native Android is now compile-verified and APK-installable. The current blocker is not compilation but provider-side payment integration, especially DCS / Sunmi runtime matching and VibeCash credential availability.
- SKU / catalog foundation has moved well beyond documentation: V2 catalog admin backend exists, `pc-admin` can configure categories, products, SKUs, and product-level attribute / modifier / combo-slot JSON, and QR / preview ordering already consume those configurations.
- Reservations and Transfer Table are usable in preview as real front-of-house flows, but still need V2 backend formalization before they can be considered complete.
