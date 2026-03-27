# Image Upload System — Design Spec

Date: 2026-03-27
Status: Rev 4 — all P1/P2 review findings addressed

---

## Problem

Products and SKUs have no image support. POS and QR frontends show broken image placeholders. Merchants cannot upload product photos.

## Solution

A controlled image upload system with:
- S3 private storage (no public-read ACL)
- Backend proxy for serving images
- Merchant-scoped ownership enforcement (images shared across stores within a merchant)
- Independent `image_assets` table (not embedding URLs in product/SKU rows)
- Magic byte validation (not just MIME type header)

## Scoping Decision: Merchant vs Store

Images are **merchant-scoped**, not store-scoped. A merchant with multiple stores can share product images across all stores. This is intentional — restaurant chains use the same menu photos across locations. The catalog (products/SKUs) is store-scoped, but images are a shared asset at the merchant level.

## Architecture

```
Upload:
  pc-admin → POST /api/v2/admin/catalog/images/upload (multipart)
           → Backend: validate file (size + magic bytes), extract merchantId from AuthContext
           → S3: PUT images/{merchantId}/{YYYY/MM}/{imageId}.{ext}
           → DB: INSERT image_assets (status=ACTIVE)
           → Response: { imageId: "IMG-xxx" }

Bind:
  pc-admin → PUT /api/v2/admin/catalog/products/{id}
           → Existing upsert payload adds optional imageId field
           → Backend: verify imageId belongs to merchant from AuthContext
           → DB: UPDATE products SET image_id = "IMG-xxx"
  (SKU images set via the same product upsert — SKU is a nested payload, not a separate endpoint)

Serve:
  Any frontend → GET /api/v2/images/{imageId}
               → Backend: lookup image_assets (status=ACTIVE), get s3Key
               → S3: GET object
               → Stream to client with Cache-Control, Content-Disposition, X-Content-Type-Options

Delete:
  pc-admin → DELETE /api/v2/admin/catalog/images/{imageId}
           → Backend: verify ownership via AuthContext
           → Check: if image is referenced by any product or SKU → 409 Conflict
           → If unreferenced: set status=DELETED
           → S3 object retained until cleanup job
```

## Threat Model

Menu images are non-sensitive (publicly visible in restaurant). The imageId space is 64-bit hex (2^64 possible values), making sequential enumeration infeasible.

**Accepted risk:** All ACTIVE images are publicly accessible via the serving endpoint, including images that have been uploaded but not yet bound to a product. The worst case is an attacker sees a menu photo that hasn't been published yet. This is explicitly acceptable for V1 because:
1. The content is non-sensitive (food photos)
2. The 64-bit ID space makes guessing infeasible
3. Adding a "bound-only" check would require joins on every image serve request, hurting latency

## Prerequisite: AuthContext

**Current gap:** `JwtAuthFilter` puts only `userId` and `role` into the SecurityContext. But image upload/bind needs `merchantId` to enforce ownership.

**Required work item (must be done before image feature):**

1. Create `AuthenticatedActor` record:
```java
public record AuthenticatedActor(
    Long userId, String username, String role,
    Long merchantId, Long storeId
) {}
```

2. Modify `JwtAuthFilter` to extract all claims and store `AuthenticatedActor` as the principal:
```java
// In JwtAuthFilter.doFilterInternal:
Long merchantId = claims.get("merchantId", Long.class);
Long storeId = claims.get("storeId", Long.class);
AuthenticatedActor actor = new AuthenticatedActor(
    Long.parseLong(userId), username, role, merchantId, storeId
);
UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
    actor, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
);
```

3. Create `AuthContext` utility:
```java
public class AuthContext {
    public static AuthenticatedActor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (AuthenticatedActor) auth.getPrincipal();
    }
}
```

This eliminates the need to manually parse JWT claims in every service method.

## Security: Endpoint Authorization

**Current SecurityConfig (line 52):** `anyRequest().authenticated()` — all non-public endpoints only require a valid JWT, regardless of role.

**Required additions to SecurityConfig:**
```java
// All admin catalog operations (including image upload/delete AND product upsert) require ADMIN or PLATFORM_ADMIN
.requestMatchers("/api/v2/admin/**").hasAnyRole("ADMIN", "PLATFORM_ADMIN")
```

This covers:
- `/api/v2/admin/catalog/images/**` — upload and delete
- `/api/v2/admin/catalog/products/**` — product upsert (which includes image binding)
- `/api/v2/admin/catalog/categories/**` — category management
- `/api/v2/admin/orders` — merchant order view

This must be added BEFORE the `.anyRequest().authenticated()` line. Without this, a CASHIER role user could upload/delete images AND bind/unbind images via product upsert.

## Data Model

### New table: image_assets

```sql
CREATE TABLE image_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255),
    content_type VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_image_id UNIQUE (image_id),
    INDEX idx_image_merchant (merchant_id),
    INDEX idx_image_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Altered tables

```sql
ALTER TABLE products ADD COLUMN image_id VARCHAR(64) NULL AFTER product_name;
ALTER TABLE skus ADD COLUMN image_id VARCHAR(64) NULL AFTER sku_name;
```

No foreign keys — `image_id` is a soft reference resolved at read time.

## Upload Endpoint

### POST /api/v2/admin/catalog/images/upload

**Auth:** JWT with ADMIN or PLATFORM_ADMIN role (enforced by SecurityConfig).

**Request:** `multipart/form-data` with field name `file`.

**Validation:**
1. Max file size: 5MB (enforced by Spring multipart config)
2. Allowed MIME types: `image/jpeg`, `image/png`, `image/webp`
3. **Magic byte validation** (not just Content-Type header):
   - JPEG: starts with `FF D8 FF`
   - PNG: starts with `89 50 4E 47`
   - WebP: starts with `52 49 46 46` ... `57 45 42 50`
   - Reject if magic bytes don't match declared type → 400

**Processing:**
1. Extract `merchantId` from `AuthContext.current().merchantId()` (never from request body)
2. Generate `imageId`: `IMG-` + 16 hex chars from UUID
3. Determine extension from validated content type
4. S3 key: `images/{merchantId}/{YYYY/MM}/{imageId}.{ext}`
5. Upload to S3 with content type header, no ACL
6. Insert into `image_assets` with status=ACTIVE
7. Return `{ imageId, contentType, fileSizeBytes }`

**Spring multipart config (add to application.yml):**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

**Error handling:**
- If S3 upload fails → don't insert DB record, return 500
- If DB insert fails after S3 upload → orphaned S3 object (accepted, cleaned by S3 lifecycle rule)

## Image Serving Endpoint

### GET /api/v2/images/{imageId}

**Auth:** Public. Images served to POS tablets, QR pages, unauthenticated contexts.

**Processing:**
1. Lookup `image_assets` by `imageId` WHERE `status = 'ACTIVE'`
2. If not found → 404
3. Get S3 object by `s3Key`
4. Stream response with headers:
   - `Content-Type`: from DB record
   - `Content-Disposition: inline; filename="{imageId}.{ext}"`
   - `Cache-Control: public, max-age=86400`
   - `X-Content-Type-Options: nosniff`
   - `Content-Security-Policy: default-src 'none'`
   - `ETag`: from S3 response

## Image Delete Endpoint

### DELETE /api/v2/admin/catalog/images/{imageId}

**Auth:** JWT with ADMIN or PLATFORM_ADMIN role (enforced by SecurityConfig).

**Processing:**
1. Lookup `image_assets` by `imageId`
2. Verify `merchant_id` matches `AuthContext.current().merchantId()` → 403 if not
3. **Reference check:** Query `products` and `skus` tables for any row with `image_id = :imageId`
   - If any reference exists → **409 Conflict** with message "Image is still used by N product(s) and M SKU(s). Unbind before deleting."
4. If unreferenced: Set `status = 'DELETED'`, `updated_at = now()`
5. Return 204 No Content
6. S3 object NOT deleted immediately — cleaned by lifecycle rule

**Why "referenced = blocked":** Images are merchant-scoped shared assets. Deleting while bound would silently break other stores' menus. Merchant must unbind first (remove imageId from product/SKU via the catalog upsert endpoint).

## Binding Images to Products/SKUs

### Via existing product upsert endpoint

**Endpoint:** `PUT /api/v2/admin/catalog/products/{id}` (existing)

The existing product upsert payload is extended with optional `imageId` field:
```json
{
  "productName": "Signature Fried Rice",
  "imageId": "IMG-a1b2c3d4e5f6g7h8",
  "skus": [
    {
      "skuName": "Regular",
      "imageId": "IMG-x9y8z7w6v5u4t3s2"
    }
  ]
}
```

**There is NO separate `PUT /api/v2/admin/catalog/skus/{id}` endpoint.** SKU images are set through the product upsert payload, same as all other SKU fields. This matches the existing code in `AdminCatalogV2Controller` and `AdminCatalogWriteService`.

**Validation (3-layer check):**

1. **Store-merchant boundary:** Before any product write, verify `store.merchantId == AuthContext.current().merchantId()`. The current code resolves by `storeCode` but does NOT verify the store belongs to the caller's merchant. This must be added to `AdminCatalogWriteService` as a prerequisite for this feature.

2. **Image ownership:** If `imageId` provided → verify exists in `image_assets` with `status=ACTIVE` and `merchant_id == AuthContext.current().merchantId()` → 403 if not

3. **Null semantics:**
   - If `imageId` is null or omitted → leave existing image unchanged
   - If `imageId` is empty string → unbind (set to null)

### Image Priority (frontend read time)

1. If SKU has `image_id` → use it
2. Else if parent Product has `image_id` → use it
3. Else → placeholder (grey box + first letter of name)

## Backend DTO Changes (explicit list)

### DTOs that need `imageId` or `imageUrl` added:

| DTO | New field | Used by |
|-----|-----------|---------|
| `QrMenuDto.MenuItemDto` | `String imageUrl` | QR frontend menu display |
| `AdminCatalogProductDto` | `String imageId`, `String imageUrl` | pc-admin product list/detail |
| `AdminCatalogSkuDto` | `String imageId`, `String imageUrl` | pc-admin SKU detail |

`imageUrl` is computed at read time: if `imageId` is non-null, set to `/api/v2/images/{imageId}`. If null, set to null.

### Frontend type changes:

| File | Change |
|------|--------|
| `pc-admin/src/api/services/productService.ts` Product type | Add `imageId?: string`, `imageUrl?: string` |
| `pc-admin/src/api/services/productService.ts` SKU type | Add `imageId?: string`, `imageUrl?: string` |
| `qr-ordering-web` menu item type | Add `imageUrl?: string` |
| `android-preview-web` menu item type | Add `imageUrl?: string` |

## S3 Configuration

**Bucket:** `founderpos-images` (private, no public access)
**Region:** `us-east-1` (same as EC2)
**Key pattern:** `images/{merchantId}/{YYYY/MM}/{imageId}.{ext}`

```yaml
aws:
  s3:
    bucket: ${S3_BUCKET:founderpos-images}
    region: ${S3_REGION:us-east-1}
```

Docker compose:
```yaml
AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
S3_BUCKET: ${S3_BUCKET:-founderpos-images}
S3_REGION: ${S3_REGION:-us-east-1}
```

## Files Summary

| Type | Count | Description |
|------|-------|-------------|
| New backend files | 9 | Migration, entity, repo, upload service, upload controller, serve controller, S3 config, AuthenticatedActor, AuthContext |
| Modified backend | 8 | pom.xml, application.yml, SecurityConfig, JwtAuthFilter, ProductEntity, SkuEntity, catalog write/read services, QrMenuDto |
| Modified infra | 2 | docker-compose.prod.yml, .env.example |
| Modified frontend | 3 | pc-admin (product types + upload UI), android-preview-web (image display), qr-ordering-web (image display) |
| **Total** | **~22** | |
