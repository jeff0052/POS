# Image Upload System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add product/SKU image support with S3 storage, backend proxy serving, and merchant-scoped ownership.

**Architecture:** Private S3 bucket + backend upload/serve proxy + independent `image_assets` table. Images are merchant-scoped shared assets. Auth enforced via JWT claims extracted into `AuthenticatedActor` principal. Three-layer validation on binding: store→merchant boundary, image ownership, null semantics.

**Tech Stack:** Spring Boot 3.3.3, AWS SDK v2, MySQL 8 (Flyway), existing React frontends (pc-admin, android-preview-web, qr-ordering-web).

**Spec:** `docs/superpowers/specs/2026-03-27-image-upload-design.md` (rev 5)

---

## File Map

### New files (9)
| File | Responsibility |
|------|---------------|
| `pos-backend/src/main/resources/db/migration/v2/V065__image_assets.sql` | Migration: create `image_assets` table, add `image_id` to products/skus |
| `pos-backend/src/main/java/com/developer/pos/auth/security/AuthenticatedActor.java` | Record holding userId, username, role, merchantId, storeId |
| `pos-backend/src/main/java/com/developer/pos/auth/security/AuthContext.java` | Static utility to get current AuthenticatedActor from SecurityContext |
| `pos-backend/src/main/java/com/developer/pos/v2/image/infrastructure/persistence/entity/ImageAssetEntity.java` | JPA entity for `image_assets` table |
| `pos-backend/src/main/java/com/developer/pos/v2/image/infrastructure/persistence/repository/JpaImageAssetRepository.java` | Spring Data repo for ImageAssetEntity |
| `pos-backend/src/main/java/com/developer/pos/v2/image/infrastructure/s3/S3ImageStorage.java` | S3 upload/download/delete operations |
| `pos-backend/src/main/java/com/developer/pos/v2/image/application/service/ImageUploadService.java` | Upload, delete, validate, reference-check logic |
| `pos-backend/src/main/java/com/developer/pos/v2/image/interfaces/rest/ImageUploadController.java` | POST upload, DELETE endpoints (admin-scoped) |
| `pos-backend/src/main/java/com/developer/pos/v2/image/interfaces/rest/ImageServeController.java` | GET public serving endpoint |

### Modified files (11)
| File | Change |
|------|--------|
| `pos-backend/pom.xml` | Add `software.amazon.awssdk:s3` dependency |
| `pos-backend/src/main/resources/application.yml` | Add `aws.s3.*` config + multipart limits |
| `pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java` | Add `/api/v2/images/**` permitAll + `/api/v2/admin/**` ADMIN role |
| `pos-backend/src/main/java/com/developer/pos/auth/security/JwtAuthFilter.java` | Extract merchantId/storeId into AuthenticatedActor principal |
| `pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/entity/ProductEntity.java` | Add `imageId` field + getter/setter |
| `pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/entity/SkuEntity.java` | Add `imageId` field + getter/setter |
| `pos-backend/src/main/java/com/developer/pos/v2/catalog/interfaces/rest/request/UpsertCatalogProductRequest.java` | Add `imageId` to product + SKU records |
| `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/AdminCatalogWriteService.java` | Store-merchant boundary check + image ownership validation + imageId persistence |
| `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/AdminCatalogReadService.java` | Populate imageId/imageUrl in DTOs |
| `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/dto/AdminCatalogProductDto.java` | Add `imageId`, `imageUrl` fields |
| `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/dto/AdminCatalogSkuDto.java` | Add `imageId`, `imageUrl` fields |
| `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/dto/QrMenuDto.java` | Add `imageUrl` to MenuItemDto |

### Infrastructure (2)
| File | Change |
|------|--------|
| `docker-compose.prod.yml` | Add AWS env vars to backend service |
| `.env.example` | Add AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, S3_BUCKET, S3_REGION |

---

## Task 1: AuthenticatedActor + AuthContext (prerequisite)

**Files:**
- Create: `pos-backend/src/main/java/com/developer/pos/auth/security/AuthenticatedActor.java`
- Create: `pos-backend/src/main/java/com/developer/pos/auth/security/AuthContext.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/auth/security/JwtAuthFilter.java`

- [ ] **Step 1: Create AuthenticatedActor record**

```java
package com.developer.pos.auth.security;

public record AuthenticatedActor(
        Long userId,
        String username,
        String role,
        Long merchantId,
        Long storeId
) {}
```

- [ ] **Step 2: Create AuthContext utility**

```java
package com.developer.pos.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthContext {
    public static AuthenticatedActor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedActor)) {
            throw new IllegalStateException("No authenticated actor in security context");
        }
        return (AuthenticatedActor) auth.getPrincipal();
    }
}
```

- [ ] **Step 3: Modify JwtAuthFilter to populate AuthenticatedActor**

In `JwtAuthFilter.java`, replace lines 36-43:

```java
// OLD:
String userId = claims.getSubject();
String role = claims.get("role", String.class);
UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
);

// NEW:
String userId = claims.getSubject();
String username = claims.get("username", String.class);
String role = claims.get("role", String.class);
Long merchantId = claims.get("merchantId", Long.class);
Long storeId = claims.get("storeId", Long.class);

AuthenticatedActor actor = new AuthenticatedActor(
        Long.parseLong(userId), username, role, merchantId, storeId
);
UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        actor, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
);
```

- [ ] **Step 4: Verify existing tests still pass**

Run: `cd /Users/ontanetwork/Documents/Codex/pos-backend && ./mvnw test -pl . 2>&1 | tail -20`
Expected: Existing tests pass (or no tests exist yet — confirm no compilation errors).

- [ ] **Step 5: Commit**

```bash
git add pos-backend/src/main/java/com/developer/pos/auth/security/AuthenticatedActor.java \
       pos-backend/src/main/java/com/developer/pos/auth/security/AuthContext.java \
       pos-backend/src/main/java/com/developer/pos/auth/security/JwtAuthFilter.java
git commit -m "feat(auth): add AuthenticatedActor + AuthContext for JWT claim extraction"
```

---

## Task 2: SecurityConfig updates

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java`

- [ ] **Step 1: Add image serving permitAll and admin role restriction**

In `SecurityConfig.java`, add these lines BEFORE `.anyRequest().authenticated()` (before line 52):

```java
// Public image serving (QR/POS need unauthenticated access)
.requestMatchers("/api/v2/images/**").permitAll()
// All admin operations require ADMIN or PLATFORM_ADMIN
.requestMatchers("/api/v2/admin/**").hasAnyRole("ADMIN", "PLATFORM_ADMIN")
```

The full `authorizeHttpRequests` block should now be:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/login", "/api/v2/auth/login").permitAll()
    .requestMatchers("/api/v1/auth/logout", "/api/v2/auth/logout").permitAll()
    .requestMatchers("/api/v1/auth/bootstrap", "/api/v2/auth/bootstrap").permitAll()
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/api/v2/qr-ordering/**").permitAll()
    .requestMatchers("/api/v2/payments/vibecash/webhook").permitAll()
    .requestMatchers("/api/v2/internal/**").permitAll()
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    // NEW: public image serving
    .requestMatchers("/api/v2/images/**").permitAll()
    // NEW: admin operations require ADMIN role
    .requestMatchers("/api/v2/admin/**").hasAnyRole("ADMIN", "PLATFORM_ADMIN")
    // Existing
    .requestMatchers("/api/v2/mcp/**").hasAnyRole("ADMIN", "PLATFORM_ADMIN")
    .requestMatchers("/api/v2/platform/**").hasRole("PLATFORM_ADMIN")
    .anyRequest().authenticated()
)
```

- [ ] **Step 2: Commit**

```bash
git add pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java
git commit -m "feat(auth): add image serving permitAll + admin role restriction"
```

---

## Task 3: Database migration + entity + repository

**Files:**
- Create: `pos-backend/src/main/resources/db/migration/v2/V065__image_assets.sql`
- Create: `pos-backend/src/main/java/com/developer/pos/v2/image/infrastructure/persistence/entity/ImageAssetEntity.java`
- Create: `pos-backend/src/main/java/com/developer/pos/v2/image/infrastructure/persistence/repository/JpaImageAssetRepository.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/entity/ProductEntity.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/entity/SkuEntity.java`

- [ ] **Step 1: Create migration V065**

```sql
-- V065__image_assets.sql

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

ALTER TABLE products ADD COLUMN image_id VARCHAR(64) NULL AFTER product_name;
ALTER TABLE skus ADD COLUMN image_id VARCHAR(64) NULL AFTER sku_name;
```

- [ ] **Step 2: Create ImageAssetEntity**

```java
package com.developer.pos.v2.image.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "image_assets")
public class ImageAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_id", nullable = false, unique = true)
    private String imageId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ImageAssetEntity() {}

    public ImageAssetEntity(String imageId, Long merchantId, String s3Key,
                            String originalName, String contentType, long fileSizeBytes) {
        this.imageId = imageId;
        this.merchantId = merchantId;
        this.s3Key = s3Key;
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getImageId() { return imageId; }
    public Long getMerchantId() { return merchantId; }
    public String getS3Key() { return s3Key; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void markDeleted() {
        this.status = "DELETED";
        this.updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create JpaImageAssetRepository**

```java
package com.developer.pos.v2.image.infrastructure.persistence.repository;

import com.developer.pos.v2.image.infrastructure.persistence.entity.ImageAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaImageAssetRepository extends JpaRepository<ImageAssetEntity, Long> {
    Optional<ImageAssetEntity> findByImageIdAndStatus(String imageId, String status);
    Optional<ImageAssetEntity> findByImageId(String imageId);
}
```

- [ ] **Step 4: Add imageId to ProductEntity**

In `ProductEntity.java`, add after `productName` field:

```java
@Column(name = "image_id")
private String imageId;
```

Add getter and setter:
```java
public String getImageId() { return imageId; }
public void setImageId(String imageId) { this.imageId = imageId; }
```

- [ ] **Step 5: Add imageId to SkuEntity**

In `SkuEntity.java`, add after `skuName` field:

```java
@Column(name = "image_id")
private String imageId;
```

Add getter and setter:
```java
public String getImageId() { return imageId; }
public void setImageId(String imageId) { this.imageId = imageId; }
```

- [ ] **Step 6: Commit**

```bash
git add pos-backend/src/main/resources/db/migration/v2/V065__image_assets.sql \
       pos-backend/src/main/java/com/developer/pos/v2/image/ \
       pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/entity/ProductEntity.java \
       pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/entity/SkuEntity.java
git commit -m "feat(image): add image_assets table, entity, repo + imageId on product/sku"
```

---

## Task 4: S3 storage service

**Files:**
- Create: `pos-backend/src/main/java/com/developer/pos/v2/image/infrastructure/s3/S3ImageStorage.java`
- Modify: `pos-backend/pom.xml`
- Modify: `pos-backend/src/main/resources/application.yml`

- [ ] **Step 1: Add AWS SDK v2 dependency to pom.xml**

Add inside `<dependencies>`:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.16</version>
</dependency>
```

- [ ] **Step 2: Add S3 + multipart config to application.yml**

Add at the top level (not under any profile):
```yaml
aws:
  s3:
    bucket: ${S3_BUCKET:founderpos-images}
    region: ${S3_REGION:us-east-1}

spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

- [ ] **Step 3: Create S3ImageStorage**

```java
package com.developer.pos.v2.image.infrastructure.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

@Component
public class S3ImageStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3ImageStorage(
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region
    ) {
        this.bucket = bucket;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public void upload(String key, byte[] data, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data)
        );
    }

    public S3ObjectResponse download(String key) {
        var response = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
        return new S3ObjectResponse(
                response,
                response.response().eTag()
        );
    }

    public record S3ObjectResponse(InputStream inputStream, String eTag) {}
}
```

- [ ] **Step 4: Commit**

```bash
git add pos-backend/pom.xml \
       pos-backend/src/main/resources/application.yml \
       pos-backend/src/main/java/com/developer/pos/v2/image/infrastructure/s3/
git commit -m "feat(image): add S3 storage service + AWS SDK dependency"
```

---

## Task 5: Image upload + delete service

**Files:**
- Create: `pos-backend/src/main/java/com/developer/pos/v2/image/application/service/ImageUploadService.java`

- [ ] **Step 1: Create ImageUploadService**

```java
package com.developer.pos.v2.image.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.image.infrastructure.persistence.entity.ImageAssetEntity;
import com.developer.pos.v2.image.infrastructure.persistence.repository.JpaImageAssetRepository;
import com.developer.pos.v2.image.infrastructure.s3.S3ImageStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageUploadService {

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final JpaImageAssetRepository imageRepo;
    private final JpaProductRepository productRepo;
    private final JpaSkuRepository skuRepo;
    private final S3ImageStorage s3Storage;

    public ImageUploadService(JpaImageAssetRepository imageRepo,
                              JpaProductRepository productRepo,
                              JpaSkuRepository skuRepo,
                              S3ImageStorage s3Storage) {
        this.imageRepo = imageRepo;
        this.productRepo = productRepo;
        this.skuRepo = skuRepo;
        this.s3Storage = s3Storage;
    }

    @Transactional
    public ImageAssetEntity upload(MultipartFile file) throws IOException {
        Long merchantId = AuthContext.current().merchantId();

        // Validate size
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File exceeds 5MB limit");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !MAGIC_BYTES.containsKey(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Allowed: JPEG, PNG, WebP");
        }

        // Validate magic bytes
        byte[] data = file.getBytes();
        byte[] expected = MAGIC_BYTES.get(contentType);
        if (data.length < expected.length) {
            throw new IllegalArgumentException("File too small to be a valid image");
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[i] != expected[i]) {
                throw new IllegalArgumentException("File content does not match declared type " + contentType);
            }
        }

        // Generate IDs
        String imageId = "IMG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String ext = EXTENSIONS.get(contentType);
        LocalDate now = LocalDate.now();
        String s3Key = String.format("images/%d/%d/%02d/%s.%s",
                merchantId, now.getYear(), now.getMonthValue(), imageId, ext);

        // Upload to S3
        s3Storage.upload(s3Key, data, contentType);

        // Save to DB
        ImageAssetEntity entity = new ImageAssetEntity(
                imageId, merchantId, s3Key,
                file.getOriginalFilename(), contentType, file.getSize()
        );
        return imageRepo.save(entity);
    }

    @Transactional
    public void delete(String imageId) {
        Long merchantId = AuthContext.current().merchantId();

        ImageAssetEntity image = imageRepo.findByImageId(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));

        if (!image.getMerchantId().equals(merchantId)) {
            throw new SecurityException("Not authorized to delete this image");
        }

        // Reference check
        long productRefs = productRepo.countByImageId(imageId);
        long skuRefs = skuRepo.countByImageId(imageId);
        if (productRefs > 0 || skuRefs > 0) {
            throw new IllegalStateException(String.format(
                    "Image is still used by %d product(s) and %d SKU(s). Unbind before deleting.",
                    productRefs, skuRefs));
        }

        image.markDeleted();
        imageRepo.save(image);
    }

    @Transactional(readOnly = true)
    public ImageAssetEntity getActiveImage(String imageId) {
        return imageRepo.findByImageIdAndStatus(imageId, "ACTIVE").orElse(null);
    }

    @Transactional(readOnly = true)
    public void validateImageOwnership(String imageId, Long merchantId) {
        ImageAssetEntity image = imageRepo.findByImageIdAndStatus(imageId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Image not found or deleted: " + imageId));
        if (!image.getMerchantId().equals(merchantId)) {
            throw new SecurityException("Image does not belong to your merchant");
        }
    }
}
```

- [ ] **Step 2: Add countByImageId to product and SKU repositories**

In `JpaProductRepository.java`, add:
```java
long countByImageId(String imageId);
```

In `JpaSkuRepository.java`, add:
```java
long countByImageId(String imageId);
```

- [ ] **Step 3: Commit**

```bash
git add pos-backend/src/main/java/com/developer/pos/v2/image/application/service/ \
       pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/repository/
git commit -m "feat(image): add ImageUploadService with upload, delete, magic byte validation"
```

---

## Task 6: Upload + delete controller

**Files:**
- Create: `pos-backend/src/main/java/com/developer/pos/v2/image/interfaces/rest/ImageUploadController.java`

- [ ] **Step 1: Create ImageUploadController**

```java
package com.developer.pos.v2.image.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.image.application.service.ImageUploadService;
import com.developer.pos.v2.image.infrastructure.persistence.entity.ImageAssetEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v2/admin/catalog/images")
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    public ImageUploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping("/upload")
    public ApiResponse<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        ImageAssetEntity saved = imageUploadService.upload(file);
        return ApiResponse.success(new UploadResponse(
                saved.getImageId(),
                saved.getContentType(),
                saved.getFileSizeBytes()
        ));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable String imageId) {
        imageUploadService.delete(imageId);
        return ResponseEntity.noContent().build();
    }

    public record UploadResponse(String imageId, String contentType, long fileSizeBytes) {}
}
```

- [ ] **Step 2: Commit**

```bash
git add pos-backend/src/main/java/com/developer/pos/v2/image/interfaces/rest/ImageUploadController.java
git commit -m "feat(image): add upload + delete controller (admin-scoped)"
```

---

## Task 7: Public image serving controller

**Files:**
- Create: `pos-backend/src/main/java/com/developer/pos/v2/image/interfaces/rest/ImageServeController.java`

- [ ] **Step 1: Create ImageServeController**

```java
package com.developer.pos.v2.image.interfaces.rest;

import com.developer.pos.v2.image.application.service.ImageUploadService;
import com.developer.pos.v2.image.infrastructure.persistence.entity.ImageAssetEntity;
import com.developer.pos.v2.image.infrastructure.s3.S3ImageStorage;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v2/images")
public class ImageServeController {

    private final ImageUploadService imageUploadService;
    private final S3ImageStorage s3Storage;

    public ImageServeController(ImageUploadService imageUploadService, S3ImageStorage s3Storage) {
        this.imageUploadService = imageUploadService;
        this.s3Storage = s3Storage;
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> serve(@PathVariable String imageId) throws IOException {
        ImageAssetEntity image = imageUploadService.getActiveImage(imageId);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        S3ImageStorage.S3ObjectResponse s3Response = s3Storage.download(image.getS3Key());
        byte[] data = s3Response.inputStream().readAllBytes();

        String ext = image.getContentType().split("/")[1];
        if ("jpeg".equals(ext)) ext = "jpg";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(image.getContentType()));
        headers.set("Content-Disposition", "inline; filename=\"" + imageId + "." + ext + "\"");
        headers.setCacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePublic());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "default-src 'none'");
        if (s3Response.eTag() != null) {
            headers.setETag(s3Response.eTag());
        }

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add pos-backend/src/main/java/com/developer/pos/v2/image/interfaces/rest/ImageServeController.java
git commit -m "feat(image): add public image serving endpoint with security headers"
```

---

## Task 8: Catalog write service — store-merchant boundary + image binding

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/catalog/interfaces/rest/request/UpsertCatalogProductRequest.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/AdminCatalogWriteService.java`

- [ ] **Step 1: Add imageId to UpsertCatalogProductRequest**

Add `String imageId` field to the main record (after `status`):
```java
public record UpsertCatalogProductRequest(
        @NotBlank String storeCode,
        @NotNull Long categoryId,
        String productCode,
        @NotBlank String name,
        @NotBlank String status,
        String imageId,                              // NEW
        @Valid @NotEmpty List<UpsertCatalogSkuItemRequest> skus,
        // ... rest unchanged
) {
    public record UpsertCatalogSkuItemRequest(
            Long skuId,
            String skuCode,
            @NotBlank String name,
            @Min(0) long priceCents,
            @NotBlank String status,
            @NotNull Boolean available,
            String imageId                           // NEW
    ) {}
    // ... rest unchanged
}
```

- [ ] **Step 2: Modify AdminCatalogWriteService.upsertProduct**

Add `ImageUploadService` injection to constructor. Then in `upsertProduct`:

1. After resolving the store from `storeCode`, add merchant boundary check:
```java
Long authMerchantId = AuthContext.current().merchantId();
if (!store.getMerchantId().equals(authMerchantId)) {
    throw new SecurityException("Store does not belong to your merchant");
}
```

2. After product save, handle image binding:
```java
// Image binding (product level)
if (imageId != null) {
    if (imageId.isEmpty()) {
        product.setImageId(null);
    } else {
        imageUploadService.validateImageOwnership(imageId, authMerchantId);
        product.setImageId(imageId);
    }
}
```

3. Same pattern inside the SKU loop for each `skuCmd.imageId()`.

4. **Add `String imageId` to the `UpsertSkuCommand` record** (inner record of `AdminCatalogWriteService`):
```java
public record UpsertSkuCommand(Long skuId, String skuCode, String name,
        long priceCents, String status, boolean available, String imageId) {}
```

5. **Update controller mapping** in `AdminCatalogV2Controller.upsertProduct` to pass `skuReq.imageId()` when constructing `UpsertSkuCommand`.

6. **Update ALL `AdminCatalogProductDto` constructor calls** in both `AdminCatalogWriteService` and `AdminCatalogReadService` to include the 2 new fields (`imageId`, `imageUrl`). Since Java records have positional constructors, every existing call site MUST be updated or compilation will fail.

7. **Update ALL `AdminCatalogSkuDto` constructor calls** — same issue. Both write and read services construct this DTO and must include `imageId` and `imageUrl` parameters.

- [ ] **Step 3: Commit**

```bash
git add pos-backend/src/main/java/com/developer/pos/v2/catalog/interfaces/rest/request/UpsertCatalogProductRequest.java \
       pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/AdminCatalogWriteService.java
git commit -m "feat(image): add store-merchant boundary check + image binding in product upsert"
```

---

## Task 9: Read DTOs — add imageId/imageUrl

**Files:**
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/dto/AdminCatalogProductDto.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/dto/AdminCatalogSkuDto.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/dto/QrMenuDto.java`
- Modify: `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/AdminCatalogReadService.java`

- [ ] **Step 1: Update AdminCatalogProductDto**

Add `String imageId` and `String imageUrl` fields:
```java
public record AdminCatalogProductDto(
        Long id,
        Long categoryId,
        String name,
        String barcode,
        long priceCents,
        int stockQty,
        String status,
        String categoryName,
        String imageId,      // NEW
        String imageUrl,     // NEW
        List<AdminCatalogSkuDto> skus,
        List<AdminCatalogAttributeGroupDto> attributeGroups,
        List<AdminCatalogModifierGroupDto> modifierGroups,
        List<AdminCatalogComboSlotDto> comboSlots
) {}
```

- [ ] **Step 2: Update AdminCatalogSkuDto**

Add `String imageId` and `String imageUrl` fields:
```java
public record AdminCatalogSkuDto(
        Long id,
        Long productId,
        String name,
        String barcode,
        long priceCents,
        String status,
        boolean available,
        String imageId,      // NEW
        String imageUrl      // NEW
) {}
```

- [ ] **Step 3: Update QrMenuDto.MenuItemDto**

Add `String imageUrl` field:
```java
public record MenuItemDto(
        Long productId,
        String productCode,
        String productName,
        Long skuId,
        String skuCode,
        String skuName,
        long unitPriceCents,
        String imageUrl,     // NEW
        List<AdminCatalogAttributeGroupDto> attributeGroups,
        List<AdminCatalogModifierGroupDto> modifierGroups,
        List<AdminCatalogComboSlotDto> comboSlots
) {}
```

- [ ] **Step 4: Update AdminCatalogReadService to populate image fields**

Add a helper method:
```java
private String imageUrl(String imageId) {
    return imageId != null ? "/api/v2/images/" + imageId : null;
}
```

Use it wherever DTOs are constructed — pass `product.getImageId()` and `imageUrl(product.getImageId())` for products, same pattern for SKUs. For QR menu items, use image priority: SKU imageId first, then product imageId.

- [ ] **Step 5: Commit**

```bash
git add pos-backend/src/main/java/com/developer/pos/v2/catalog/application/dto/ \
       pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/AdminCatalogReadService.java
git commit -m "feat(image): add imageId/imageUrl to catalog DTOs + QR menu"
```

---

## Task 10: Infrastructure — Docker + env

**Files:**
- Modify: `docker-compose.prod.yml`
- Modify: `.env.example` (or create)

- [ ] **Step 1: Add AWS env vars to docker-compose.prod.yml**

In the backend service `environment` section, add:
```yaml
AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
S3_BUCKET: ${S3_BUCKET:-founderpos-images}
S3_REGION: ${S3_REGION:-us-east-1}
```

- [ ] **Step 2: Update .env.example**

Add:
```
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
S3_BUCKET=founderpos-images
S3_REGION=us-east-1
```

- [ ] **Step 3: Commit**

```bash
git add docker-compose.prod.yml .env.example
git commit -m "infra: add AWS S3 env vars for image upload"
```

---

## Task 11: Create S3 bucket on AWS

- [ ] **Step 1: Create bucket via AWS CLI**

```bash
ssh -i ~/.ssh/founderPOS-aws.pem ec2-user@54.237.230.5 \
  "aws s3 mb s3://founderpos-images --region us-east-1"
```

- [ ] **Step 2: Verify bucket is private (no public access)**

```bash
ssh -i ~/.ssh/founderPOS-aws.pem ec2-user@54.237.230.5 \
  "aws s3api get-public-access-block --bucket founderpos-images"
```

Expected: All four `Block*` settings are `true`.

- [ ] **Step 3: Add env vars to production .env**

```bash
ssh -i ~/.ssh/founderPOS-aws.pem ec2-user@54.237.230.5 \
  "echo 'S3_BUCKET=founderpos-images' >> /home/ec2-user/founderpos/.env && \
   echo 'S3_REGION=us-east-1' >> /home/ec2-user/founderpos/.env"
```

Note: AWS credentials should already be available via EC2 instance role or env vars.

- [ ] **Step 4: Commit note (no code change)**

No git commit needed — this is infrastructure setup.

---

## Task 12: Build, deploy, verify

- [ ] **Step 1: Build backend Docker image**

```bash
cd /Users/ontanetwork/Documents/Codex && docker compose -f docker-compose.prod.yml build pos-backend
```

- [ ] **Step 2: Deploy to AWS**

```bash
# Push updated code + rebuild on AWS
ssh -i ~/.ssh/founderPOS-aws.pem ec2-user@54.237.230.5 \
  "cd /home/ec2-user/founderpos && git pull && docker compose -f docker-compose.prod.yml up -d --build pos-backend"
```

- [ ] **Step 3: Verify migration ran**

```bash
ssh -i ~/.ssh/founderPOS-aws.pem ec2-user@54.237.230.5 \
  "docker exec pos-mysql mysql -uroot -p'...' pos_v2_db -e 'DESCRIBE image_assets;'"
```

Expected: Table exists with all columns.

- [ ] **Step 4: Verify upload endpoint**

```bash
# Create a test JPEG (1x1 pixel)
echo -ne '\xff\xd8\xff\xe0\x00\x10JFIF' > /tmp/test.jpg

# Login to get token
TOKEN=$(curl -s http://54.237.230.5/api/v2/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"FounderPOS2026!"}' | jq -r '.token')

# Upload
curl -X POST http://54.237.230.5/api/v2/admin/catalog/images/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/tmp/test.jpg"
```

Expected: `{ "imageId": "IMG-...", "contentType": "image/jpeg", ... }`

- [ ] **Step 5: Verify serve endpoint (public, no auth)**

```bash
curl -I http://54.237.230.5/api/v2/images/IMG-xxxxx
```

Expected: 200 with `Content-Type: image/jpeg`, `X-Content-Type-Options: nosniff`.

- [ ] **Step 6: Verify delete with reference check**

```bash
# Bind image to product first, then try to delete — should get 409
curl -X DELETE http://54.237.230.5/api/v2/admin/catalog/images/IMG-xxxxx \
  -H "Authorization: Bearer $TOKEN"
```

Expected: 409 Conflict if bound, 204 No Content if unbound.

- [ ] **Step 7: Commit verification note**

```bash
git commit --allow-empty -m "verify: image upload system deployed and tested on AWS"
```

---

## Task 13: Frontend integration (deferred)

> **Note:** Frontend changes are explicitly deferred to a follow-up plan. The backend is fully functional and testable via curl/API without frontend work. Frontend tasks will be:
>
> 1. `pc-admin`: Add `imageId?` and `imageUrl?` to Product/SKU TypeScript types in `productService.ts`. Add "Upload Image" button to product edit form.
> 2. `android-preview-web`: Use `imageUrl` from menu API to display product images. Fall back to placeholder.
> 3. `qr-ordering-web`: Same as POS — use `imageUrl` from QR menu API.
>
> These changes are small (~20 lines each) and can be done after backend verification.
