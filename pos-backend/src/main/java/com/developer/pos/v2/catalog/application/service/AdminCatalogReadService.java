package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.v2.catalog.application.dto.AdminCatalogCategoryDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogAttributeGroupDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogComboSlotDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogModifierGroupDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogProductDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogSkuDto;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.image.application.service.ImageUploadService;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductCategoryEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.StoreSkuAvailabilityEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductCategoryRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaStoreSkuAvailabilityRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCatalogReadService implements UseCase {

    private final JpaStoreLookupRepository storeLookupRepository;
    private final JpaProductCategoryRepository categoryRepository;
    private final JpaProductRepository productRepository;
    private final JpaSkuRepository skuRepository;
    private final JpaStoreSkuAvailabilityRepository availabilityRepository;
    private final ObjectMapper objectMapper;
    private final ImageUploadService imageUploadService;

    public AdminCatalogReadService(
            JpaStoreLookupRepository storeLookupRepository,
            JpaProductCategoryRepository categoryRepository,
            JpaProductRepository productRepository,
            JpaSkuRepository skuRepository,
            JpaStoreSkuAvailabilityRepository availabilityRepository,
            ObjectMapper objectMapper,
            ImageUploadService imageUploadService
    ) {
        this.storeLookupRepository = storeLookupRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.availabilityRepository = availabilityRepository;
        this.objectMapper = objectMapper;
        this.imageUploadService = imageUploadService;
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogProductDto> getProducts(String storeCode) {
        StoreEntity store = findStore(storeCode);
        List<ProductCategoryEntity> categories = categoryRepository.findByStoreIdOrderBySortOrderAscCategoryNameAsc(store.getId());
        Map<Long, ProductCategoryEntity> categoryMap = new HashMap<>();
        for (ProductCategoryEntity category : categories) {
            categoryMap.put(category.getId(), category);
        }

        List<ProductEntity> products = productRepository.findByStoreIdOrderByProductNameAsc(store.getId());
        List<Long> productIds = products.stream().map(ProductEntity::getId).toList();
        Map<Long, List<AdminCatalogSkuDto>> skuMap = getSkusByProduct(store.getId(), productIds);

        return products.stream()
                .map(product -> {
                    ProductCategoryEntity category = categoryMap.get(product.getCategoryId());
                    List<AdminCatalogSkuDto> skus = skuMap.getOrDefault(product.getId(), Collections.emptyList());
                    AdminCatalogSkuDto defaultSku = skus.isEmpty() ? null : skus.get(0);
                    return new AdminCatalogProductDto(
                            product.getId(),
                            product.getCategoryId(),
                            product.getProductName(),
                            defaultSku == null ? "" : defaultSku.barcode(),
                            defaultSku == null ? 0L : defaultSku.priceCents(),
                            999,
                            normalizeStatus(product.getProductStatus()),
                            category == null ? "-" : category.getCategoryName(),
                            product.getImageId(),
                            imageUrl(product.getImageId()),
                            skus,
                            readList(product.getAttributeConfigJson(), new TypeReference<List<AdminCatalogAttributeGroupDto>>() {}),
                            readList(product.getModifierConfigJson(), new TypeReference<List<AdminCatalogModifierGroupDto>>() {}),
                            readList(product.getComboSlotConfigJson(), new TypeReference<List<AdminCatalogComboSlotDto>>() {})
                    );
                })
                .sorted(Comparator.comparing(AdminCatalogProductDto::categoryName).thenComparing(AdminCatalogProductDto::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogCategoryDto> getCategories(String storeCode) {
        StoreEntity store = findStore(storeCode);
        return categoryRepository.findByStoreIdOrderBySortOrderAscCategoryNameAsc(store.getId()).stream()
                .map(category -> new AdminCatalogCategoryDto(
                        category.getId(),
                        category.getCategoryName(),
                        category.getSortOrder(),
                        category.isActive() ? "ENABLED" : "DISABLED"
                ))
                .sorted(Comparator.comparing(AdminCatalogCategoryDto::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogSkuDto> getSkus(String storeCode, Long productId) {
        StoreEntity store = findStore(storeCode);
        return getSkusByProduct(store.getId(), List.of(productId)).getOrDefault(productId, Collections.emptyList());
    }

    private StoreEntity findStore(String storeCode) {
        return storeLookupRepository.findByStoreCode(storeCode)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeCode));
    }

    private Map<Long, List<AdminCatalogSkuDto>> getSkusByProduct(Long storeId, List<Long> productIds) {
        Map<Long, List<AdminCatalogSkuDto>> result = new HashMap<>();
        if (productIds.isEmpty()) {
            return result;
        }
        List<SkuEntity> skus = skuRepository.findByProductIdInOrderByProductIdAscIdAsc(productIds);
        List<Long> skuIds = skus.stream().map(SkuEntity::getId).toList();
        Map<Long, Boolean> availabilityMap = new HashMap<>();
        for (StoreSkuAvailabilityEntity availability : availabilityRepository.findByStoreIdAndSkuIdIn(storeId, skuIds)) {
            availabilityMap.put(availability.getSkuId(), availability.isAvailable());
        }
        for (SkuEntity sku : skus) {
            result.computeIfAbsent(sku.getProductId(), ignored -> new java.util.ArrayList<>())
                    .add(new AdminCatalogSkuDto(
                            sku.getId(),
                            sku.getProductId(),
                            sku.getSkuName(),
                            sku.getSkuCode(),
                            sku.getBasePriceCents(),
                            normalizeStatus(sku.getSkuStatus()),
                            availabilityMap.getOrDefault(sku.getId(), true),
                            sku.getImageId(),
                            imageUrl(sku.getImageId())
                    ));
        }
        return result;
    }

    private String imageUrl(String imageId) {
        return imageId != null ? imageUploadService.resolvePublicUrl(imageId) : null;
    }

    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null) {
            return "DISABLED";
        }
        return switch (rawStatus.trim().toUpperCase()) {
            case "ACTIVE", "ENABLED" -> "ENABLED";
            default -> "DISABLED";
        };
    }

    private <T> List<T> readList(String raw, TypeReference<List<T>> typeReference) {
        try {
            return raw == null || raw.isBlank()
                    ? List.of()
                    : objectMapper.readValue(raw, typeReference);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse catalog config", exception);
        }
    }
}
