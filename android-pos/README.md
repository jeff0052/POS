# Android POS

Minimal Android cashier terminal skeleton for the POS project.

## Included

- Kotlin + Jetpack Compose setup
- Hilt dependency injection setup
- Room local database starter
- Retrofit network starter
- Simple navigation flow
- `Login -> Home -> Cashier -> Payment Confirm` screens
- Orders, order detail, settlement, and settings demo screens
- Cashier ViewModel and cart state
- Payment abstraction interface
- Printer abstraction interface
- POS-oriented package structure starter

## Next recommended steps

1. Add Gradle wrapper
2. Replace seeded local products with backend sync
3. Add login/session persistence
4. Add real order creation and payment confirm flow
5. Integrate payment SDK adapter
6. Integrate printer SDK adapter
7. Add refund flow and richer order states
