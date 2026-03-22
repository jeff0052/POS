# Release Notes

## Version
`v0.1.0-internal-preview`

## Date
`2026-03-23`

## Release Type
Internal preview

## Summary
This release packages the current restaurant POS preview and core project scaffolding into a reviewable internal milestone. It is intended for product review, design alignment, and early workflow walkthroughs. It is not production-ready.

## Included

### Android Preview Web
- Restaurant POS preview shell with fixed top and bottom navigation
- Table Management page with restaurant-oriented table states and color mapping
- Ordering page with menu, cart, summary, and action panel
- Order Review, Split Bill, Payment, and Payment Success pages aligned to the same shell

### Android Native App
- Base Android POS project structure
- Initial screen flow scaffold for cashier-side development

### Merchant Admin
- PC admin preview for merchant operations
- Login, dashboard, products, categories, orders, refunds, and reports pages

### Backend
- Spring Boot backend scaffold
- Core API and module structure for auth, store, category, product, order, and report domains

### Local Environment
- Docker Compose setup for MySQL, backend, and merchant admin

## Current Highlights
- Restaurant POS visual direction is established
- Fast preview workflow is in place through `android-preview-web`
- POS project structure and milestones are already tracked in FPMS
- Merchant admin and backend scaffolding are available for future integration

## Not Included Yet
- Real payment SDK integration
- Real printer SDK integration
- Production-grade backend business logic
- Full restaurant table workflow rules
- Mobile boss monitoring web
- Production deployment and security hardening

## Known Limitations
- Preview pages use demo state and mock interaction
- The Android preview web is for UI and flow validation only
- The backend is not ready for real store deployment
- Docker and local services are for internal development only

## Recommended Use
- Internal demo
- UI review
- Product walkthrough
- Design iteration
- Early stakeholder feedback

## Next Version Focus
- Continue redesigning all Android preview pages into one coherent restaurant POS system
- Strengthen Ordering, Order Review, and Payment flows
- Connect merchant admin to real backend endpoints
- Expand backend transaction modules for payment, refund, and print

## Internal Note
Do not use this release in production or merchant pilot stores.
