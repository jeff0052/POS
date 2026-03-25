package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.v2.catalog.application.dto.AdminCatalogCategoryDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogProductDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogSkuDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductCategoryEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.StoreSkuAvailabilityEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductCategoryRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaStoreSkuAvailabilityRepository;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AdminCatalogWriteService implements UseCase {

    private final JpaStoreLookupRepository storeLookupRepository;
    private final JpaProductCategoryRepository categoryRepository;
    private final JpaProductRepository productRepository;
    private final JpaSkuRepository skuRepository;
    private final JpaStoreSkuAvailabilityRepository availabilityRepository;

    public AdminCatalogWriteService(
            JpaStoreLookupRepository storeLookupRepository,
            JpaProductCategoryRepository categoryRepository,
            JpaProductRepository productRepository,
            JpaSkuRepository skuRepository,
            JpaStoreSkuAvailabilityRepository availabilityRepository
    ) {
        this.storeLookupRepository = storeLookupRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional
    public AdminCatalogCategoryDto upsertCategory(
            Long categoryId,
            String storeCode,
            String categoryCode,
            String name,
            boolean enabled,
            Integer sortOrder
    ) {
        StoreEntity store = findStore(storeCode);
        String normalizedCode = normalizeCode(categoryCode, name, "category");
        ProductCategoryEntity entity = categoryId == null
                ? new ProductCategoryEntity(store.getId(), normalizedCode, name, enabled, defaultSortOrder(sortOrder))
                : categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        if (categoryId != null) {
            ensureStoreOwnership(store.getId(), entity.getStoreId(), "Category");
            entity.update(normalizedCode, name, enabled, defaultSortOrder(sortOrder));
        }

        ProductCategoryEntity saved = categoryRepository.save(entity);
        return new AdminCatalogCategoryDto(
                saved.getId(),
                saved.getCategoryName(),
                saved.getSortOrder(),
                saved.isActive() ? "ENABLED" : "DISABLED"
        );
    }

    @Transactional
    public AdminCatalogProductDto upsertProduct(
            Long productId,
            String storeCode,
            Long categoryId,
            String productCode,
            String name,
            String status,
            List<UpsertSkuCommand> skus
    ) {
        if (skus == null || skus.isEmpty()) {
            throw new IllegalArgumentException("At least one SKU is required");
        }

        StoreEntity store = findStore(storeCode);
        ProductCategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        ensureStoreOwnership(store.getId(), category.getStoreId(), "Category");

        String normalizedProductCode = normalizeCode(productCode, name, "product");
        ProductEntity entity = productId == null
                ? new ProductEntity(store.getId(), categoryId, normalizedProductCode, name, normalizeStatus(status))
                : productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (productId != null) {
            ensureStoreOwnership(store.getId(), entity.getStoreId(), "Product");
            entity.update(categoryId, normalizedProductCode, name, normalizeStatus(status));
        }

        ProductEntity savedProduct = productRepository.save(entity);
        List<SkuEntity> existingSkus = skuRepository.findByProductIdOrderByIdAsc(savedProduct.getId());
        Map<Long, SkuEntity> existingSkuMap = new HashMap<>();
        for (SkuEntity existing : existingSkus) {
            existingSkuMap.put(existing.getId(), existing);
        }

        List<AdminCatalogSkuDto> skuDtos = new ArrayList<>();
        for (int i = 0; i < skus.size(); i++) {
            UpsertSkuCommand command = skus.get(i);
            String normalizedSkuCode = normalizeCode(command.skuCode(), command.name(), "sku-" + (i + 1));
            SkuEntity skuEntity = command.skuId() == null
                    ? new SkuEntity(savedProduct.getId(), normalizedSkuCode, command.name(), command.priceCents(), normalizeStatus(command.status()))
                    : existingSkuMap.get(command.skuId());

            if (command.skuId() != null && skuEntity == null) {
                throw new IllegalArgumentException("SKU not found: " + command.skuId());
            }

            if (command.skuId() != null && !Objects.equals(skuEntity.getProductId(), savedProduct.getId())) {
                throw new IllegalArgumentException("SKU does not belong to product: " + command.skuId());
            }

            if (command.skuId() != null) {
                skuEntity.update(normalizedSkuCode, command.name(), command.priceCents(), normalizeStatus(command.status()));
            }

            SkuEntity savedSku = skuRepository.save(skuEntity);
            StoreSkuAvailabilityEntity availability = availabilityRepository.findByStoreIdAndSkuId(store.getId(), savedSku.getId())
                    .orElseGet(() -> new StoreSkuAvailabilityEntity(store.getId(), savedSku.getId(), command.available()));
            availability.updateAvailability(command.available());
            availabilityRepository.save(availability);

            skuDtos.add(new AdminCatalogSkuDto(
                    savedSku.getId(),
                    savedProduct.getId(),
                    savedSku.getSkuName(),
                    savedSku.getSkuCode(),
                    savedSku.getBasePriceCents(),
                    savedSku.getSkuStatus(),
                    availability.isAvailable()
            ));
        }

        skuDtos.sort(Comparator.comparing(AdminCatalogSkuDto::id));
        AdminCatalogSkuDto defaultSku = skuDtos.get(0);
        return new AdminCatalogProductDto(
                savedProduct.getId(),
                category.getId(),
                savedProduct.getProductName(),
                defaultSku.barcode(),
                defaultSku.priceCents(),
                999,
                savedProduct.getProductStatus(),
                category.getCategoryName(),
                skuDtos
        );
    }

    private StoreEntity findStore(String storeCode) {
        return storeLookupRepository.findByStoreCode(storeCode)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeCode));
    }

    private void ensureStoreOwnership(Long expectedStoreId, Long actualStoreId, String label) {
        if (!Objects.equals(expectedStoreId, actualStoreId)) {
            throw new IllegalArgumentException(label + " does not belong to store");
        }
    }

    private int defaultSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "ACTIVE" : status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACTIVE", "ENABLED" -> "ACTIVE";
            case "INACTIVE", "DISABLED" -> "INACTIVE";
            default -> normalized;
        };
    }

    private String normalizeCode(String candidate, String fallbackName, String fallbackPrefix) {
        if (candidate != null && !candidate.isBlank()) {
            return slug(candidate);
        }
        String slug = slug(fallbackName);
        return slug.isBlank() ? fallbackPrefix : slug;
    }

    private String slug(String raw) {
        String normalized = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized;
    }

    public record UpsertSkuCommand(
            Long skuId,
            String skuCode,
            String name,
            long priceCents,
            String status,
            boolean available
    ) {
    }
}
