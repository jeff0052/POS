# Payment Service — External Team Handoff Document

Version: 1.0
Date: 2026-03-27
Author: FounderPOS Engineering

---

## 1. Overview

FounderPOS has a dedicated payment microservice (`pos-payment-service`) that handles all payment provider integrations. Your team's job is to implement the **VibeCash QR adapter** and **DCS card adapter** inside this service.

The skeleton is already built and deployed. Cash payments work end-to-end. You only need to implement two adapter classes.

### Architecture

```
┌─────────────────┐     ┌────────────────────┐     ┌──────────────────┐
│  POS Backend     │────▶│  Payment Service    │────▶│  Providers       │
│  (orders/tables) │     │  (port 8081)        │     │                  │
│  port 8080       │◀────│                     │◀────│  VibeCash API    │
└─────────────────┘     │  Orchestrator        │     │  DCS SDK (via    │
   callback via         │  ├── CashAdapter ✅   │     │    Android POST) │
   HMAC-signed POST     │  ├── VibeCashAdapter ❌│     └──────────────────┘
                        │  └── DcsCardAdapter ❌ │
                        └────────────────────┘
```

✅ = implemented and tested
❌ = your team implements

---

## 2. What's Already Done (Don't Touch)

| Component | Location | Description |
|-----------|----------|-------------|
| PaymentIntentEntity | `core/PaymentIntentEntity.java` | DB entity for payment lifecycle |
| PaymentStatus | `core/PaymentStatus.java` | CREATED → PENDING → SUCCEEDED / FAILED / EXPIRED / CANCELLED |
| PaymentProviderAdapter | `core/PaymentProviderAdapter.java` | Interface your adapters implement |
| PaymentOrchestrator | `orchestrator/PaymentOrchestrator.java` | Routes to correct adapter, manages state |
| CallbackNotifier | `orchestrator/CallbackNotifier.java` | Notifies POS backend when payment succeeds |
| PaymentController | `api/PaymentController.java` | REST API for creating/querying intents |
| CashAdapter | `adapter/CashAdapter.java` | Reference implementation (immediate success) |
| Flyway V001 | `db/migration/V001__payment_service_init.sql` | DB schema |
| Dockerfile | `Dockerfile` | Build & deploy |

---

## 3. What You Need to Implement

### 3.1 VibeCashAdapter

**File:** `src/main/java/com/developer/pos/payment/adapter/VibeCashAdapter.java`

**What it does:** Integrates with VibeCash QR payment gateway.

**VibeCash API:**
- Base URL: configurable via `providers.vibecash.api-url`
- Auth: Bearer token via `providers.vibecash.secret`
- Webhook secret: `providers.vibecash.webhook-secret`

**createPayment flow:**

```
POST ${VIBECASH_API_URL}/v1/payment_links
Authorization: Bearer ${VIBECASH_SECRET}
Content-Type: application/json

{
  "amount": 5000,                    // cents from intent.getAmountCents()
  "currency": "SGD",                 // from intent.getCurrency()
  "name": "Table X payment",
  "description": "POS payment",
  "paymentMethodTypes": ["wechat"],  // map from intent.getPaymentScheme()
  "metadata": {
    "paymentIntentId": "PI-xxx",     // from intent.getIntentId()
    "storeId": "1",
    "tableId": "42"
  }
}
```

**Response:**
```json
{
  "data": {
    "id": "vibecash-tx-123",
    "url": "https://checkout.vibecash.dev/xxx",
    "status": "open"
  }
}
```

**Map to CreatePaymentResult:**
```java
return new CreatePaymentResult(
    true,
    response.data.id,           // providerTransactionId
    response.data.url,          // checkoutUrl (customer scans this QR)
    response.data.status,       // providerStatus
    null, null                  // no error
);
```

**Scheme mapping:**
| PaymentScheme | VibeCash paymentMethodTypes |
|---------------|---------------------------|
| WECHAT_QR | `["wechat"]` |
| ALIPAY_QR | `["alipay"]` |
| PAYNOW_QR | `["paynow"]` |
| GRABPAY_QR | `["grabpay"]` |
| SHOPEEPAY_QR | `["shopeepay"]` |

**supports() method:**
```java
return "QR".equalsIgnoreCase(paymentMethod);
```

### 3.2 VibeCash Webhook Handler

**File:** `src/main/java/com/developer/pos/payment/webhook/VibeCashWebhookHandler.java`

**What it does:** Receives VibeCash callback when customer completes payment.

**Webhook endpoint:** `POST /api/v1/payments/webhooks/vibecash`

**Signature verification:**
```java
String expected = new HmacUtils("HmacSHA256", webhookSecret).hmacHex(rawPayload);
// Compare with VibeCash-Signature header using MessageDigest.isEqual()
```

**Webhook payload:**
```json
{
  "type": "payment.succeeded",
  "data": {
    "object": {
      "id": "vibecash-tx-123",
      "status": "completed",
      "metadata": {
        "paymentIntentId": "PI-xxx"
      }
    }
  }
}
```

**Processing:**
1. Verify HMAC signature (reject if invalid or secret not configured)
2. Store raw payload in `webhook_events` table
3. Extract `paymentIntentId` from metadata
4. If `type == "payment.succeeded"` → call `orchestrator.handleProviderSuccess(intentId, providerTxId)`
5. If `type == "payment.failed"` → call `orchestrator.handleProviderFailure(intentId, errorCode, errorMsg)`
6. If `type == "checkout.session.expired"` → call `orchestrator.handleProviderFailure(intentId, "EXPIRED", "Checkout session expired")`

**The orchestrator automatically notifies POS backend via CallbackNotifier.** You don't need to do that.

### 3.3 DcsCardAdapter

**File:** `src/main/java/com/developer/pos/payment/adapter/DcsCardAdapter.java`

**What it does:** Receives card payment results from the Android POS terminal.

**How DCS works:**
- DCS SDK runs on the Sunmi Android terminal
- Android app calls DCS SDK → gets transaction result
- Android app POSTs the result to Payment Service

**createPayment flow:**
- DCS is **terminal-initiated**, not server-initiated
- `createPayment` should return status `PENDING` with no checkoutUrl
- The actual card tap/swipe happens on the terminal
- Android sends result to a dedicated endpoint

**Android result endpoint:** `POST /api/v1/payments/dcs/result`

**Request from Android:**
```json
{
  "paymentIntentId": "PI-xxx",
  "success": true,
  "providerTransactionId": "DCS-tx-456",
  "cardType": "VISA",
  "cardNo": "****1234",
  "authNo": "123456",
  "referenceNo": "REF789",
  "voucherNo": "VCH001",
  "amount": 5000,
  "errorCode": null,
  "errorMessage": null
}
```

**Processing:**
1. Look up PaymentIntent by `paymentIntentId`
2. If `success == true` → call `orchestrator.handleProviderSuccess(intentId, providerTransactionId)`
3. If `success == false` → call `orchestrator.handleProviderFailure(intentId, errorCode, errorMessage)`

**supports() method:**
```java
return "CARD".equalsIgnoreCase(paymentMethod);
```

### 3.4 Refund Methods

Each adapter should implement `refund()`:

**VibeCashAdapter.refund():**
```
POST ${VIBECASH_API_URL}/v1/refunds
Authorization: Bearer ${VIBECASH_SECRET}
{
  "paymentLinkId": providerTransactionId,
  "amount": amountCents,
  "reason": reason
}
```

**DcsCardAdapter.refund():**
- DCS refund is terminal-initiated (like payment)
- Return `PENDING` status
- Provide endpoint for Android to POST refund result

**CashAdapter.refund():**
- Already implemented — returns immediate success

---

## 4. Configuration

**File:** `src/main/resources/application.yml`

```yaml
providers:
  vibecash:
    enabled: true                          # set to true when ready
    api-url: https://api.vibecash.dev      # or production URL
    secret: YOUR_VIBECASH_API_KEY
    webhook-secret: YOUR_VIBECASH_WEBHOOK_SECRET
    currency: SGD
  dcs:
    enabled: true                          # set to true when ready
  cash:
    enabled: true
```

**Environment variables on AWS:**
```
VIBECASH_ENABLED=true
VIBECASH_API_URL=https://api.vibecash.dev
VIBECASH_SECRET=xxx
VIBECASH_WEBHOOK_SECRET=xxx
DCS_ENABLED=true
```

---

## 5. PaymentProviderAdapter Interface

```java
public interface PaymentProviderAdapter {

    /** Provider identifier: "CASH", "VIBECASH", "DCS" */
    String providerCode();

    /** Create a payment with the provider. Return result immediately. */
    CreatePaymentResult createPayment(PaymentIntentEntity intent);

    /** Query payment status from provider. */
    QueryPaymentResult queryPayment(String providerTransactionId);

    /** Request refund from provider. */
    RefundResult refund(String providerTransactionId, long amountCents, String reason);

    /** Does this adapter handle the given method/scheme? */
    boolean supports(String paymentMethod, String paymentScheme);

    record CreatePaymentResult(
        boolean success,
        String providerTransactionId,  // provider's transaction ID
        String checkoutUrl,            // customer-facing URL (QR) or null (card/cash)
        String providerStatus,         // provider's native status string
        String errorCode,
        String errorMessage
    ) {}

    record QueryPaymentResult(
        String providerTransactionId,
        String providerStatus,
        long confirmedAmountCents
    ) {}

    record RefundResult(
        boolean success,
        String providerRefundId,
        String errorCode,
        String errorMessage
    ) {}
}
```

**Rules:**
1. Your adapter class must be annotated `@Component` so Spring auto-discovers it
2. The Orchestrator auto-collects all `PaymentProviderAdapter` beans
3. When POS creates an intent, the Orchestrator calls `supports()` on each adapter to find the right one
4. `createPayment()` should be synchronous — make the HTTP call and return the result
5. If the payment is async (QR/Card), return `checkoutUrl` (for QR) or null (for card) and status will be `PENDING`
6. The Orchestrator handles state transitions — you just return the provider result

---

## 6. Payment Flow (End to End)

### Cash (reference — already works)
```
POS UI → "Pay with Cash"
  → POST /payment-api/v1/payments/intents { paymentMethod: "CASH" }
  → Orchestrator → CashAdapter.createPayment() → immediate SUCCEEDED
  → CallbackNotifier → POST to POS backend → settlement created → table cleared
```

### QR (your implementation)
```
POS UI → "Pay with WeChat/Alipay/PayNow"
  → POST /payment-api/v1/payments/intents { paymentMethod: "QR", paymentScheme: "WECHAT_QR" }
  → Orchestrator → VibeCashAdapter.createPayment() → returns checkoutUrl
  → POS UI shows QR code from checkoutUrl
  → Customer scans and pays
  → VibeCash sends webhook → VibeCashWebhookHandler processes
  → orchestrator.handleProviderSuccess() → status = SUCCEEDED
  → CallbackNotifier → POST to POS backend → settlement created → table cleared
  → POS UI polls intent status → sees SUCCEEDED → shows success
```

### Card (your implementation)
```
POS UI → "Pay with Card"
  → POST /payment-api/v1/payments/intents { paymentMethod: "CARD", paymentScheme: "VISA" }
  → Orchestrator → DcsCardAdapter.createPayment() → returns PENDING (no checkoutUrl)
  → POS UI tells Android WebView to start DCS SDK
  → Customer taps/inserts card on Sunmi terminal
  → DCS SDK returns result to Android
  → Android POSTs result to /payment-api/v1/payments/dcs/result
  → DcsCardAdapter processes → orchestrator.handleProviderSuccess()
  → CallbackNotifier → POST to POS backend → settlement → table cleared
```

---

## 7. Database

**Payment Service uses its own database:** `pos_payment_db`

| Table | Purpose |
|-------|---------|
| `payment_intents` | One row per payment attempt. Tracks full lifecycle. |
| `webhook_events` | Raw webhook payloads for audit. |

You don't need to add tables. The existing schema handles all three providers.

**Key fields on payment_intents:**

| Field | Description |
|-------|-------------|
| intent_id | Unique ID (PI-xxx), generated by orchestrator |
| payment_method | CASH / QR / CARD |
| payment_scheme | WECHAT_QR / ALIPAY_QR / PAYNOW_QR / VISA / MASTERCARD / ... |
| provider_code | CASH / VIBECASH / DCS |
| status | CREATED / PENDING / SUCCEEDED / FAILED / EXPIRED / CANCELLED |
| provider_transaction_id | Provider's transaction reference |
| checkout_url | QR checkout URL (VibeCash only) |
| callback_url | POS backend URL to notify on success |

---

## 8. Local Development Setup

```bash
# 1. Start MySQL
docker compose up -d mysql

# 2. Create payment database
docker exec pos-mysql mysql -uroot -proot -e 'CREATE DATABASE IF NOT EXISTS pos_payment_db;'

# 3. Run payment service locally
cd pos-payment-service
./mvnw spring-boot:run

# 4. Test health
curl http://localhost:8081/api/v1/payments/health

# 5. Test cash intent
curl -X POST http://localhost:8081/api/v1/payments/intents \
  -H 'Content-Type: application/json' \
  -d '{"merchantId":1,"storeId":1,"tableId":1,"amountCents":5000,"paymentMethod":"CASH","paymentScheme":"CASH"}'
```

---

## 9. Testing Checklist

### VibeCash Adapter

| # | Test | Expected |
|---|------|----------|
| 1 | Create QR intent (WECHAT_QR) | Returns checkoutUrl, status=PENDING |
| 2 | Create QR intent (ALIPAY_QR) | Returns checkoutUrl, status=PENDING |
| 3 | Create QR intent (PAYNOW_QR) | Returns checkoutUrl, status=PENDING |
| 4 | Webhook: payment.succeeded | Intent moves to SUCCEEDED, callback sent to POS |
| 5 | Webhook: payment.failed | Intent moves to FAILED |
| 6 | Webhook: checkout.session.expired | Intent moves to EXPIRED |
| 7 | Webhook with invalid signature | Rejected with 403 |
| 8 | Webhook with empty secret | Rejected (don't skip verification) |
| 9 | Duplicate webhook (same event) | Idempotent — no duplicate processing |
| 10 | Refund via VibeCash API | RefundResult.success=true |

### DCS Card Adapter

| # | Test | Expected |
|---|------|----------|
| 1 | Create CARD intent (VISA) | Returns status=PENDING, no checkoutUrl |
| 2 | Android posts success result | Intent moves to SUCCEEDED, callback sent |
| 3 | Android posts failure result | Intent moves to FAILED |
| 4 | Android posts result for unknown intent | Returns error |
| 5 | Duplicate success post | Idempotent |
| 6 | Refund via DCS | Returns PENDING (terminal-initiated) |

### Integration

| # | Test | Expected |
|---|------|----------|
| 1 | Cash end-to-end | Intent SUCCEEDED → POS callback → table cleared |
| 2 | QR end-to-end | Intent → QR → customer pays → webhook → SUCCEEDED → table cleared |
| 3 | Card end-to-end | Intent → terminal → SDK result → SUCCEEDED → table cleared |
| 4 | Concurrent payments same table | Only one succeeds, others fail gracefully |

---

## 10. Files You Need to Create

```
pos-payment-service/src/main/java/com/developer/pos/payment/
├── adapter/
│   ├── VibeCashAdapter.java           ← YOU CREATE
│   └── DcsCardAdapter.java            ← YOU CREATE
├── webhook/
│   └── VibeCashWebhookHandler.java    ← YOU CREATE (controller + handler)
└── api/
    └── DcsResultController.java       ← YOU CREATE (receives Android POST)
```

**Total: 4 files.** Everything else is already done.

---

## 11. Don't Do These Things

1. **Don't modify PaymentOrchestrator** — it handles state transitions correctly
2. **Don't modify CallbackNotifier** — the callback to POS backend is already working
3. **Don't modify PaymentController** — the API is stable
4. **Don't modify CashAdapter** — it's the reference implementation
5. **Don't add new tables** — the existing schema handles all providers
6. **Don't skip webhook signature verification** — reject if secret is empty
7. **Don't trust client-supplied amounts** — always verify with provider response

---

## 12. Contact

Questions about:
- **Payment Service architecture** → Jeff (founder)
- **VibeCash API credentials** → Jeff → VibeCash account manager
- **DCS SDK** → Jeff → DCS/Sunmi contact
- **POS backend integration** → Check `PaymentCallbackController.java` in pos-backend
- **Android terminal code** → Check `android-pos/` in the repo
