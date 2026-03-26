# Image Upload System — Design Spec

Date: 2026-03-27
Status: Approved

---

## Problem

Products and SKUs have no image support. POS and QR frontends show broken image placeholders. Merchants cannot upload product photos.

## Solution

A controlled image upload system with:
- S3 private storage (no public-read ACL)
- Backend proxy for serving images
- Merchant-scoped ownership enforcement
- Independent `image_assets` table (not embedding URLs in product/SKU rows)

## Architecture

```
Upload:
  pc-admin → POST /api/v2/admin/catalog/images/upload (multipart)
           → Backend: validate file, extract merchantId from JWT
           → S3: PUT images/{merchantId}/{imageId}.{ext}
           → DB: INSERT image_assets
           → Response: { imageId: "IMG-xxx" }

Bind:
  pc-admin → PUT /api/v2/admin/catalog/products/{id} { imageId: "IMG-xxx" }
           → Backend: verify imageId belongs to merchantId
           → DB: UPDATE products SET image_id = "IMG-xxx"

Serve:
  Any frontend → GET /api/v2/images/{imageId}
               → Backend: lookup image_assets, get s3Key
               → S3: GET object
               → Stream to client with Cache-Control: max-age=86400
```

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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_image_id UNIQUE (image_id),
    INDEX idx_image_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Altered tables

```sql
ALTER TABLE products ADD COLUMN image_id VARCHAR(64) NULL AFTER product_name;
ALTER TABLE skus ADD COLUMN image_id VARCHAR(64) NULL AFTER sku_name;
```

No foreign keys — `image_id` is a soft reference resolved at read time. This keeps the image system decoupled from catalog domain.

## Upload Endpoint

### POST /api/v2/admin/catalog/images/upload

**Auth:** Requires JWT with ADMIN or PLATFORM_ADMIN role.

**Request:** `multipart/form-data` with field name `file`.

**Validation:**
- Max file size: 5MB
- Allowed types: `image/jpeg`, `image/png`, `image/webp`
- Reject all others

**Processing:**
1. Extract `merchantId` from JWT claims (never from request body)
2. Generate `imageId`: `IMG-` + 16 hex chars from UUID
3. Determine extension from content type: jpeg→jpg, png→png, webp→webp
4. S3 key: `images/{merchantId}/{imageId}.{ext}`
5. Upload to S3 with content type header, no ACL (bucket default is private)
6. Insert into `image_assets`
7. Return `{ imageId, contentType, fileSizeBytes }`

**Error cases:**
- 400: file too large, wrong type, missing file
- 401: not authenticated
- 403: not authorized (role check)
- 500: S3 upload failure

## Image Serving Endpoint

### GET /api/v2/images/{imageId}

**Auth:** Public (no JWT required). Images are served to POS tablets, QR pages, and unauthenticated contexts.

**Processing:**
1. Lookup `image_assets` by `imageId`
2. If not found → 404
3. Get S3 object by `s3Key`
4. Stream response with headers:
   - `Content-Type`: from DB record
   - `Cache-Control: public, max-age=86400` (1 day)
   - `ETag`: S3 object ETag

**Performance:** The 1-day cache header means browsers and nginx won't re-fetch for 24 hours. For a restaurant menu with ~50 items, this is negligible load.

## Binding Images to Products/SKUs

### PUT /api/v2/admin/catalog/products/{id}

Existing endpoint. Add optional `imageId` field.

**Validation:**
- If `imageId` is provided, verify it exists in `image_assets` and `merchant_id` matches the caller's merchant
- If ownership check fails → 403

### PUT /api/v2/admin/catalog/skus/{id}

Same as products.

### Image Priority (read time)

When rendering a menu item:
1. If SKU has `image_id` → use it
2. Else if parent Product has `image_id` → use it
3. Else → frontend shows placeholder (grey box + first letter)

This logic lives in the frontend, not the backend. The API returns both `sku.imageId` and `product.imageId`.

## Image Replacement and Cleanup

**Replace:** Upload new image → bind new imageId → old image becomes orphaned.

**Cleanup:** Orphaned images (in `image_assets` but not referenced by any product or SKU) are NOT automatically deleted. A future admin endpoint or scheduled job can clean them. For V1, manual cleanup is acceptable — a restaurant menu has dozens of images, not millions.

## S3 Configuration

**Bucket:** `founderpos-images` (private, no public access)
**Region:** `us-east-1` (same as EC2)
**Access:** IAM credentials via environment variables

```yaml
aws:
  s3:
    bucket: ${S3_BUCKET:founderpos-images}
    region: ${S3_REGION:us-east-1}
```

Docker compose env:
```yaml
AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
S3_BUCKET: ${S3_BUCKET:-founderpos-images}
S3_REGION: ${S3_REGION:-us-east-1}
```

## Frontend Changes

### pc-admin (Merchant Admin)

Product/SKU edit forms:
- Add "Upload Image" button
- On click → file picker → POST to upload endpoint → receive imageId
- Show preview thumbnail
- Save imageId with product/SKU update

### android-preview-web (POS)

Menu item rendering:
- If `item.imageId` → `<img src="/api/v2/images/{imageId}" />`
- Else → placeholder div with first letter of item name

### qr-ordering-web (QR)

Same as POS. Current Unsplash pool becomes the fallback when no imageId.

## Security Summary

| Concern | Mitigation |
|---------|------------|
| Arbitrary URL injection | imageId is a controlled reference, not a URL |
| Cross-merchant access | Upload: merchantId from JWT. Bind: ownership check. |
| File type attack | Whitelist MIME types, reject others |
| File size DoS | 5MB limit enforced at Spring multipart config |
| S3 public exposure | Private bucket, no ACL, backend proxy only |
| Cache poisoning | ETag + fixed Cache-Control, no query param cache key |

## Files to Create/Modify

### New files (~8)
- `V065__image_assets.sql` — migration
- `ImageAssetEntity.java` — entity
- `ImageAssetRepository.java` — repository
- `ImageUploadService.java` — S3 upload + validation
- `ImageUploadController.java` — upload endpoint
- `ImageServeController.java` — proxy serve endpoint
- `S3ClientConfig.java` — AWS S3 client bean

### Modified files (~8)
- `pom.xml` — add AWS S3 SDK
- `application.yml` — add aws.s3 config
- `ProductEntity.java` — add imageId field
- `SkuEntity.java` — add imageId field
- `AdminCatalogWriteService.java` — accept imageId in create/update
- `AdminCatalogReadService.java` — return imageId in DTOs
- `docker-compose.prod.yml` — add AWS env vars
- `.env.example` — add S3 config

### Frontend modifications (~3)
- `pc-admin` — product/SKU form: upload button + preview
- `android-preview-web` — menu item: use imageId or placeholder
- `qr-ordering-web` — menu item: use imageId or fallback to pool
