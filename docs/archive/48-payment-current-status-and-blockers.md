# 48. Payment Current Status and Blockers

## Purpose

This document captures the current implementation state of the unified payment workstream in the new V2 system, including what has already been designed, what has already been built, what has been verified, and what is currently blocked.

The goal is to let the team pause payment work without losing technical context, then resume quickly once the correct device SDK or external gateway credentials are available.

## Current Position

At the architecture level, payment has moved from an undefined POS collection flow into a unified payment model.

The system now distinguishes:

- `provider`
  - `DCS`
  - `VIBECASH`
  - `CASH_MANUAL`
- `method`
  - `CARD`
  - `QR`
  - `CASH`
- `scheme`
  - `VISA`
  - `MASTERCARD`
  - `WECHAT_PAY`
  - `ALIPAY`
  - `PAYNOW`
  - and future schemes

This means the system is no longer designed around a single SDK. Payment now has a defined abstraction boundary.

## Documents Already Completed

The current payment design baseline is already covered by:

- `39-unified-payment-architecture.md`
- `40-payment-integration-requirements.md`
- `41-payment-adapter-design.md`
- `42-current-milestone-v2-and-payments.md`

These documents already define:

- unified payment domain concepts
- provider and method separation
- DCS as card terminal provider
- VibeCash as QR gateway provider
- manual cash as a separate provider path
- payment orchestrator and adapter pattern

## What Has Already Been Implemented

### 1. Unified Payment Structure

The Android native POS app now routes payment through a unified payment service instead of a single mock flow.

Implemented components include:

- `PaymentService`
- `UnifiedPaymentService`
- `CashPaymentService`
- `DcsPaymentService`
- `VibeCashPaymentService`

This means the app already supports a routed payment selection flow:

- `Card Terminal`
- `WeChat QR`
- `Alipay QR`
- `PayNow QR`
- `Cash`

### 2. DCS Integration Path

The Android app already contains a real DCS adapter path based on the provided AAR.

Implemented capabilities in the current DCS adapter:

- connect to DCS SDK
- initialize country and currency
- set merchant parameters
- sign in
- start card sale
- query transaction by order ID
- sale void
- card refund
- terminal settlement
- terminal sign off

Additional implementation details:

- AAR dependency has been wired into the Android app
- `MID` and `TID` were configured
- Gradle wrapper was added to the repo
- Android app compile verification was completed successfully
- `app-debug.apk` was built successfully

### 3. VibeCash Integration Path

The new system also has a first-pass VibeCash integration path.

Implemented components include:

- backend payment attempt persistence
- backend endpoint to create VibeCash payment attempt / QR payment link
- backend endpoint to query payment attempt
- backend webhook endpoint for VibeCash callback handling
- Android app path changed so the client no longer calls VibeCash directly with secrets
- Android now calls the app backend instead

The implemented VibeCash direction is:

- backend creates payment link / checkout session
- Android receives action URL
- customer scans or completes QR payment
- backend eventually receives webhook
- system updates payment attempt and later settlement state

### 4. Backend Payment Foundation

Payment persistence and integration points in the V2 backend now include:

- settlement records
- payment attempts
- payment preview flow
- table-level payment state progression

This means payment is no longer just a UI concept. It now has backend structure and persistence.

## What Has Already Been Verified

### Verified at Design Level

- unified payment architecture
- payment integration scope
- adapter design

### Verified at Build Level

- Android native app compile succeeded with Gradle wrapper
- APK was built successfully
- backend Docker build succeeded for payment-related changes

### Verified on Real Device

The Android APK was successfully:

- installed on a SUNMI Android device
- launched on device
- navigated into the native app
- taken into the payment flow

This means the native delivery path itself is working.

## Current DCS Blocker

The DCS work did not fail because the app is broken.

The app is correctly calling:

- `DCSPaymentApi.getInstance()`
- `connectDCSPayment(...)`

But on the real device, the result returned is:

- `DCS payment service not found on this device`

### Root Cause

The provided SDK and the device runtime service do not match.

The provided AAR belongs to the `com.sunmi.dcspayment` family.

However, the physical SUNMI device is running a different payment service stack:

- package: `com.sunmi.pay.hardware_v3`
- service action: `sunmi.intent.action.PAY_HARDWARE`

Device-side inspection also showed capability names aligned with another SDK family:

- `BasicOptV2`
- `EMVOptV2`
- `PinPadOptV2`

This strongly indicates that the terminal is using a `Sunmi Pay Hardware V2 / POS API` stack, not the `com.sunmi.dcspayment` stack expected by the provided DCS AAR.

### Practical Meaning

The blocker is not:

- missing APK
- wrong app package
- wrong payment page wiring
- wrong native payment routing

The blocker is:

- SDK family mismatch between the AAR and the installed device payment service

### Current Conclusion for DCS

The DCS implementation is complete enough for the current SDK family, but it cannot be completed on this device until the correct matching SDK is provided.

The team should not continue trying random versions of the same `dcspayment` AAR family unless the vendor confirms that this device actually supports that service stack.

## Current VibeCash Blocker

VibeCash is architecturally cleaner than DCS for the current system because it is gateway based.

However, the real payment path is currently blocked by credentials and environment setup.

Still needed:

- `VIBECASH_SECRET`
- optional `VIBECASH_WEBHOOK_SECRET`

Without those, the team cannot run a real QR payment lifecycle against VibeCash production or sandbox accounts.

### Current Conclusion for VibeCash

The implementation path is in place, but real integration testing is blocked by missing account credentials rather than code structure.

## Current Cash Status

Cash is the least blocked payment path.

Current status:

- available in native payment selection
- no external SDK required
- no gateway credential required
- ready as the simplest fallback payment method

## Recommended Pause State

The payment stream is currently in a good pause state because:

- architecture is defined
- adapters are separated
- Android compile is verified
- APK is buildable and installable
- device validation has already isolated the DCS blocker
- VibeCash path is already prepared for future credential-based testing

This means the work does not need to be redone later. It only needs the correct external inputs.

## What We Need Before Resuming

### To Resume DCS

We need one of the following:

- the correct SDK for the current SUNMI device
- explicit confirmation that the device should use `com.sunmi.pay.hardware_v3`
- corresponding AAR / docs for `Sunmi Pay Hardware V2 / POS API`

### To Resume VibeCash

We need:

- `VIBECASH_SECRET`
- optionally `VIBECASH_WEBHOOK_SECRET`

## Recommended Next Step When Payment Work Resumes

Resume in this order:

1. verify the correct SUNMI payment SDK family
2. replace or add a new native adapter for the actual device service
3. run DCS or SUNMI terminal real-device transaction validation
4. add real VibeCash credentials
5. run QR gateway flow end-to-end
6. connect successful provider results into final settlement verification

## Summary

The payment workstream is no longer undefined.

It now has:

- architecture
- requirements
- adapter design
- Android native routing
- backend payment structure
- APK compile verification
- real-device verification results
- clearly isolated blockers

The two current blockers are external:

- wrong or mismatched SUNMI payment SDK for this device
- missing VibeCash credentials

That means the payment stream can pause safely and later resume with very little rediscovery cost.
