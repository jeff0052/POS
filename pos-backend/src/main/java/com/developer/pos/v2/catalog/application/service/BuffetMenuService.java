package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuCategoryDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuProductDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuSkuDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageItemEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductCategoryEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageItemRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductCategoryRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.image.application.service.ImageUploadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BuffetMenuService implements UseCase {

    private final JpaBuffetPackageRepository packageRepository;
    private final JpaBuffetPackageItemRepository itemRepository;
    private final JpaSkuRepository skuRepository;
    private final JpaProductRepository productRepository;
    private final JpaProductCategoryRepository categoryRepository;
    private final ImageUploadService imageUploadService;

    public BuffetMenuService(JpaBuffetPackageRepository packageRepository,
                             JpaBuffetPackageItemRepository itemRepository,
                             JpaSkuRepository skuRepository,
                             JpaProductRepository productRepository,
                             JpaProductCategoryRepository categoryRepository,
                             ImageUploadService imageUploadService) {
        this.packageRepository = packageRepository;
        this.itemRepository = itemRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageUploadService = imageUploadService;
    }

    @Transactional(readOnly = true)
    public MenuQueryResultDto getBuffetMenu(Long storeId, Long packageId) {
        // 1. Load package and verify storeId match + ACTIVE status
        BuffetPackageEntity pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Buffet package not found: " + packageId));
        if (!pkg.getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("Package " + packageId + " does not belong to store " + storeId);
        }
        if (!"ACTIVE".equalsIgnoreCase(pkg.getPackageStatus())) {
            throw new IllegalArgumentException("Package " + packageId + " is not active");
        }

        // 2. Load all INCLUDED + SURCHARGE items (excludes EXCLUDED)
        List<BuffetPackageItemEntity> items = itemRepository
                .findByPackageIdAndInclusionTypeNotOrderBySortOrderAsc(packageId, "EXCLUDED");
        if (items.isEmpty()) {
            return new MenuQueryResultDto(List.of());
        }

        // 3. Batch-load SKUs
        List<Long> skuIds = items.stream().map(BuffetPackageItemEntity::getSkuId).toList();
        Map<Long, SkuEntity> skuMap = skuRepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(SkuEntity::getId, Function.identity()));

        // 4. Batch-load Products
        Set<Long> productIds = skuMap.values().stream()
                .map(SkuEntity::getProductId)
                .collect(Collectors.toSet());
        Map<Long, ProductEntity> productMap = productRepository.findByIdIn(new ArrayList<>(productIds)).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        // 5. Batch-load Categories
        Set<Long> categoryIds = productMap.values().stream()
                .map(ProductEntity::getCategoryId)
                .filter(cid -> cid != null)
                .collect(Collectors.toSet());
        Map<Long, ProductCategoryEntity> categoryMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(ProductCategoryEntity::getId, Function.identity()));

        // 6. Batch-resolve image URLs
        Set<String> allImageIds = new HashSet<>();
        for (ProductEntity p : productMap.values()) {
            if (p.getImageId() != null) allImageIds.add(p.getImageId());
        }
        for (SkuEntity s : skuMap.values()) {
            if (s.getImageId() != null) allImageIds.add(s.getImageId());
        }
        Map<String, String> imageUrlMap = imageUploadService.resolvePublicUrls(allImageIds);

        // 7. Build response grouped by category
        // Use LinkedHashMap to preserve insertion order (by item sort order)
        Map<Long, Map<Long, List<MenuSkuDto>>> categoryProductSkus = new LinkedHashMap<>();
        Map<Long, ProductEntity> productLookup = new LinkedHashMap<>();

        for (BuffetPackageItemEntity item : items) {
            SkuEntity sku = skuMap.get(item.getSkuId());
            if (sku == null) continue;

            ProductEntity product = productMap.get(sku.getProductId());
            if (product == null) continue;

            Long catId = product.getCategoryId();
            if (catId == null || !categoryMap.containsKey(catId)) continue;

            String skuImageUrl = sku.getImageId() != null ? imageUrlMap.get(sku.getImageId()) : null;

            MenuSkuDto skuDto = new MenuSkuDto(
                    sku.getId(), sku.getSkuCode(), sku.getSkuName(),
                    sku.getBasePriceCents(), skuImageUrl, List.of(),
                    item.getInclusionType(), item.getSurchargeCents(), item.getMaxQtyPerPerson()
            );

            categoryProductSkus
                    .computeIfAbsent(catId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(product.getId(), k -> new ArrayList<>())
                    .add(skuDto);
            productLookup.putIfAbsent(product.getId(), product);
        }

        // 8. Assemble final category list
        List<MenuCategoryDto> result = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, List<MenuSkuDto>>> catEntry : categoryProductSkus.entrySet()) {
            ProductCategoryEntity cat = categoryMap.get(catEntry.getKey());
            if (cat == null) continue;

            List<MenuProductDto> products = new ArrayList<>();
            for (Map.Entry<Long, List<MenuSkuDto>> prodEntry : catEntry.getValue().entrySet()) {
                ProductEntity product = productLookup.get(prodEntry.getKey());
                if (product == null) continue;

                String productImageUrl = product.getImageId() != null ? imageUrlMap.get(product.getImageId()) : null;
                products.add(new MenuProductDto(
                        product.getId(), product.getProductName(), productImageUrl, prodEntry.getValue()));
            }

            if (!products.isEmpty()) {
                result.add(new MenuCategoryDto(cat.getId(), cat.getCategoryName(), products));
            }
        }

        return new MenuQueryResultDto(result);
    }
}
