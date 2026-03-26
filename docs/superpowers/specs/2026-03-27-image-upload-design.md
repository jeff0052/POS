# Image Upload System — Design Spec

Date: 2026-03-27
Status: Approved (rev 2 — post review)

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
           → Backend: validate file (size + magic bytes), extract merchantId from JWT
           → S3: PUT images/{merchantId}/{YYYY/MM}/{imageId}.{ext}
           → DB: INSERT image_assets (status=ACTIVE)
           → Response: { imageId: "IMG-xxx" }

Bind:
  pc-admin → PUT /api/v2/admin/catalog/products/{id} { imageId: "IMG-xxx" }
           → Backend: verify imageId belongs to merchantId from JWT
           → DB: UPDATE products SET image_id = "IMG-xxx"

Serve:
  Any frontend → GET /api/v2/images/{imageId}
               → Backend: lookup image_assets (status=ACTIVE), get s3Key
               → S3: GET object
               → Stream to client with Cache-Control, Content-Disposition, X-Content-Type-Options

Delete:
  pc-admin → DELETE /api/v2/admin/catalog/images/{imageId}
           → Backend: verify ownership, set status=DELETED
           → Image stops serving (404)
           → S3 object retained until cleanup job
```

## Threat Model

Menu images are non-sensitive (publicly visible in restaurant). The imageId space is 64-bit hex (2^64 possible values), making sequential enumeration infeasible. Accepted risk: if an imageId leaks via browser history or referrer headers, the worst case is an attacker sees a menu photo. This is explicitly acceptable for V1.

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

**Auth:** JWT with ADMIN or PLATFORM_ADMIN role.

**Request:** `multipart/form-data` with field name `file`.

**Validation:**
1. Max file size: 5MB (enforced by Spring multipart config)
2. Allowed MIME types: `image/jpeg`, `image/png`, `image/webp`
3. **Magic byte validation** (not just Content-Type header):
   - JPEG: starts with `FF D8 FF`
   - PNG: starts with `89 50 4E 47`
   - WebP: starts with `52 49 46 46` ... `57 45 42 50`
   - Reject if magic bytes don't match declared type

**Processing:**
1. Extract `merchantId` from JWT claims (never from request body)
2. Generate `imageId`: `IMG-` + 16 hex chars from UUID
3. Determine extension from validated content type
4. S3 key: `images/{merchantId}/{YYYY/MM}/{imageId}.{ext}`
5. Upload to S3 with content type header, no ACL
6. Insert into `image_assets` with status=ACTIVE
7. Return `{ imageId, contentType, fileSizeBytes }`

**Spring multipart config:**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

**Error handling:**
- If S3 upload fails → don't insert DB record, return 500
- If DB insert fails after S3 upload → orphaned S3 object (accepted, cleaned by lifecycle rule)

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

**Auth:** JWT with ADMIN or PLATFORM_ADMIN role.

**Processing:**
1. Lookup `image_assets` by `imageId`
2. Verify `merchant_id` matches JWT merchantId → 403 if not
3. Set `status = 'DELETED'`, `updated_at = now()`
4. Return 204 No Content
5. S3 object NOT deleted immediately — cleaned by lifecycle rule or future cleanup job

V1 does not auto-unbind deleted images from products/SKUs. Frontend handles gracefully: if image returns 404, show placeholder.

## Binding Images to Products/SKUs

### PUT /api/v2/admin/catalog/products/{id}

Existing endpoint. Add optional `imageId` field.

**Validation:**
- If `imageId` provided → verify exists in `image_assets` with `status=ACTIVE` and `merchant_id` matches JWT
- If ownership check fails → 403

### PUT /api/v2/admin/catalog/skus/{id}

Same as products.

### Image Priority (frontend read time)

1. If SKU has `image_id` → use it: `<img src="/api/v2/images/{imageId}" />`
2. Else if parent Product has `image_id` → use it
3. Else → placeholder (grey box + first letter of name)

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

## Frontend Changes

### pc-admin
- Product/SKU edit: "Upload Image" button → file picker → POST upload → show preview → save imageId
- Product/SKU list: show thumbnail

### android-preview-web (POS)
- `item.imageId` → `<img src="/api/v2/images/{imageId}" />`
- No imageId → placeholder

### qr-ordering-web (QR)
- Same as POS. Unsplash pool becomes fallback.

## Files Summary

| Type | Count | Description |
|------|-------|-------------|
| New backend files | 7 | Migration, entity, repo, upload service, upload controller, serve controller, S3 config |
| Modified backend | 6 | pom.xml, application.yml, ProductEntity, SkuEntity, catalog write/read services |
| Modified infra | 2 | docker-compose.prod.yml, .env.example |
| Modified frontend | 3 | pc-admin, android-preview-web, qr-ordering-web |
| **Total** | **~18** | |
