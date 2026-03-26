# Image Upload Feature Plan

Date: 2026-03-27

---

## Goal

让商户可以给每个 SKU 上传图片，POS/QR 前端显示真实菜品图。

## Architecture

```
商户后台 (pc-admin)
  │ Upload image file
  ▼
Backend API
  POST /api/v2/admin/catalog/images/upload
  │ Validate (size, type)
  │ Generate unique filename
  ▼
S3 Bucket (pos-images-prod)
  │ Store file
  │ Return public URL
  ▼
Backend saves URL to DB
  products.image_url / skus.image_url
  ▼
POS / QR frontend
  <img src={imageUrl} />
```

## Changes

### 1. Database (Migration V065)

```sql
ALTER TABLE products ADD COLUMN image_url VARCHAR(512) NULL AFTER product_name;
ALTER TABLE skus ADD COLUMN image_url VARCHAR(512) NULL AFTER sku_name;
```

### 2. Backend — S3 Image Service

**New files:**
- `ImageUploadService.java` — S3 upload, validate, generate URL
- `ImageUploadController.java` — `POST /api/v2/admin/catalog/images/upload`

**Dependencies:**
- `software.amazon.awssdk:s3` (AWS SDK v2)

**Upload flow:**
1. Accept multipart file (max 5MB, jpg/png/webp only)
2. Generate unique key: `images/{merchantId}/{UUID}.{ext}`
3. Upload to S3 with public-read ACL
4. Return public URL: `https://pos-images-prod.s3.amazonaws.com/images/1/xxx.jpg`

**Configuration:**
```yaml
aws:
  s3:
    bucket: ${S3_BUCKET:pos-images-prod}
    region: ${S3_REGION:ap-southeast-1}
    access-key: ${AWS_ACCESS_KEY_ID:}
    secret-key: ${AWS_SECRET_ACCESS_KEY:}
```

### 3. Backend — Entity Updates

**ProductEntity:** add `imageUrl` field + getter/setter
**SkuEntity:** add `imageUrl` field + getter/setter

**Catalog DTOs:** add `imageUrl` to all product/SKU response DTOs

**Admin write endpoints:** accept `imageUrl` in create/update product/SKU requests

### 4. Backend — API Endpoints

```
POST   /api/v2/admin/catalog/images/upload
       Request: multipart/form-data (file)
       Response: { "imageUrl": "https://..." }

PUT    /api/v2/admin/catalog/products/{id}
       Body: { ..., "imageUrl": "https://..." }

PUT    /api/v2/admin/catalog/skus/{id}
       Body: { ..., "imageUrl": "https://..." }
```

### 5. Frontend — Merchant Admin (pc-admin)

**Product/SKU edit page:**
- Add image preview area
- Add "Upload Image" button
- On click → file picker → POST to upload endpoint → get URL → save with product/SKU

**Product/SKU list page:**
- Show thumbnail in table/grid

### 6. Frontend — POS (android-preview-web)

- When rendering menu items, use `item.imageUrl` if available
- Fallback to placeholder (grey + first letter) when no image

### 7. Frontend — QR (qr-ordering-web)

- Same: use `item.imageUrl` if available
- Fallback to Unsplash pool (current behavior) when no image

### 8. AWS S3 Setup

```bash
aws s3 mb s3://pos-images-prod --region ap-southeast-1
aws s3api put-bucket-policy --bucket pos-images-prod --policy '{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "PublicRead",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::pos-images-prod/*"
  }]
}'
```

Or use CloudFront for CDN (optional, later).

### 9. Docker Compose

Add AWS credentials to `pos-backend` environment:

```yaml
AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
S3_BUCKET: ${S3_BUCKET:-pos-images-prod}
S3_REGION: ${S3_REGION:-ap-southeast-1}
```

---

## File Changes

| Phase | File | Change |
|-------|------|--------|
| DB | V065__image_url.sql | ADD COLUMN to products + skus |
| Backend | pom.xml | Add AWS S3 SDK |
| Backend | ImageUploadService.java | New: S3 upload logic |
| Backend | ImageUploadController.java | New: upload endpoint |
| Backend | ProductEntity.java | Add imageUrl field |
| Backend | SkuEntity.java | Add imageUrl field |
| Backend | AdminCatalogWriteService.java | Accept imageUrl in create/update |
| Backend | AdminCatalogReadService.java | Return imageUrl in DTOs |
| Backend | application.yml | Add aws.s3 config |
| Frontend | pc-admin: ProductForm | Add image upload component |
| Frontend | pc-admin: SKU list | Show thumbnail |
| Frontend | android-preview-web: App.tsx | Use imageUrl, fallback to placeholder |
| Frontend | qr-ordering-web: App.tsx | Use imageUrl, fallback to pool |
| Infra | docker-compose.prod.yml | Add AWS credentials |
| Infra | .env.example | Add S3 config |

**Total: ~15 files, 1 migration**

---

## Execution Order

1. Migration + entity fields (5 min)
2. S3 service + upload endpoint (30 min)
3. Catalog write/read endpoints accept/return imageUrl (20 min)
4. pc-admin upload component (30 min)
5. POS + QR fallback rendering (15 min)
6. S3 bucket setup on AWS (10 min)
7. Deploy + test (15 min)

---

## Alternative: No S3 (Simpler)

If you don't want to set up S3 right now, we can:
- Store images on the backend server filesystem (`/uploads/images/`)
- Serve via nginx (`location /uploads/`)
- Migrate to S3 later

This is faster (no AWS credentials needed) but not production-grade for scale.

---

## Decision Needed

1. **S3 vs local filesystem?** — S3 recommended for production
2. **Do you have an AWS IAM key for S3?** — I need access key + secret key
3. **S3 bucket region?** — ap-southeast-1 (Singapore) recommended
