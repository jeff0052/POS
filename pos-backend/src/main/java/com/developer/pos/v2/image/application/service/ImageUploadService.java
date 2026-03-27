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

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp"
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
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !MAGIC_BYTES.containsKey(contentType)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: JPEG, PNG, WebP");
        }
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

        String imageId = "IMG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String ext = EXTENSIONS.get(contentType);
        LocalDate now = LocalDate.now();
        String s3Key = String.format("images/%d/%d/%02d/%s.%s",
                merchantId, now.getYear(), now.getMonthValue(), imageId, ext);

        s3Storage.upload(s3Key, data, contentType);

        ImageAssetEntity entity = new ImageAssetEntity(
                imageId, merchantId, s3Key, file.getOriginalFilename(), contentType, file.getSize()
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
