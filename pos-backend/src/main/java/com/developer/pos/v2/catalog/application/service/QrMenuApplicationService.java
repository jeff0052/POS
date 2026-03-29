package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.v2.catalog.application.dto.AdminCatalogAttributeGroupDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogComboSlotDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogModifierGroupDto;
import com.developer.pos.v2.catalog.application.dto.QrMenuDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaQrMenuRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.QrMenuProjection;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.image.application.service.ImageUploadService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QrMenuApplicationService implements UseCase {

    private final JpaQrMenuRepository qrMenuRepository;
    private final JpaProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final ImageUploadService imageUploadService;

    public QrMenuApplicationService(
            JpaQrMenuRepository qrMenuRepository,
            JpaProductRepository productRepository,
            ObjectMapper objectMapper,
            ImageUploadService imageUploadService
    ) {
        this.qrMenuRepository = qrMenuRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.imageUploadService = imageUploadService;
    }

    @Transactional(readOnly = true)
    public QrMenuDto getMenu(String storeCode) {
        List<QrMenuProjection> rows = qrMenuRepository.findQrMenuByStoreCode(storeCode);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No QR menu available for store: " + storeCode);
        }

        QrMenuProjection first = rows.get(0);
        Map<Long, CategoryAccumulator> categories = new LinkedHashMap<>();
        Map<Long, ProductEntity> productsById = productRepository.findByIdIn(
                        rows.stream().map(QrMenuProjection::getProductId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        // Batch-resolve all image URLs (CDN/nginx when configured)
        Set<String> allImageIds = new HashSet<>();
        for (QrMenuProjection row : rows) {
            if (row.getSkuImageId() != null) allImageIds.add(row.getSkuImageId());
            if (row.getProductImageId() != null) allImageIds.add(row.getProductImageId());
        }
        Map<String, String> imageUrlMap = imageUploadService.resolvePublicUrls(allImageIds);

        for (QrMenuProjection row : rows) {
            CategoryAccumulator category = categories.computeIfAbsent(
                    row.getCategoryId(),
                    ignored -> new CategoryAccumulator(
                            row.getCategoryId(),
                            row.getCategoryCode(),
                            row.getCategoryName(),
                            new ArrayList<>()
                    )
            );
            ProductEntity product = productsById.get(row.getProductId());

            // Image priority: SKU imageId → product imageId → null
            String effectiveImageId = row.getSkuImageId() != null ? row.getSkuImageId() : row.getProductImageId();
            String menuImageUrl = effectiveImageId != null ? imageUrlMap.get(effectiveImageId) : null;

            category.items().add(new QrMenuDto.MenuItemDto(
                    row.getProductId(),
                    row.getProductCode(),
                    row.getProductName(),
                    row.getSkuId(),
                    row.getSkuCode(),
                    row.getSkuName(),
                    row.getUnitPriceCents(),
                    menuImageUrl,
                    readList(product == null ? null : product.getAttributeConfigJson(), new TypeReference<List<AdminCatalogAttributeGroupDto>>() {}),
                    readList(product == null ? null : product.getModifierConfigJson(), new TypeReference<List<AdminCatalogModifierGroupDto>>() {}),
                    readList(product == null ? null : product.getComboSlotConfigJson(), new TypeReference<List<AdminCatalogComboSlotDto>>() {})
            ));
        }

        return new QrMenuDto(
                first.getStoreId(),
                first.getStoreCode(),
                first.getStoreName(),
                categories.values().stream()
                        .map(category -> new QrMenuDto.CategoryDto(
                                category.categoryId(),
                                category.categoryCode(),
                                category.categoryName(),
                                category.items()
                        ))
                        .toList()
        );
    }

    private record CategoryAccumulator(
            Long categoryId,
            String categoryCode,
            String categoryName,
            List<QrMenuDto.MenuItemDto> items
    ) {
    }

    private <T> List<T> readList(String raw, TypeReference<List<T>> typeReference) {
        try {
            return raw == null || raw.isBlank()
                    ? Collections.emptyList()
                    : objectMapper.readValue(raw, typeReference);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse QR menu config", exception);
        }
    }
}
