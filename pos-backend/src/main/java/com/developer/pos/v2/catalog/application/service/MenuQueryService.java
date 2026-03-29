package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuCategoryDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuModifierGroupDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuModifierOptionDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuProductDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuSkuDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.MenuTimeSlotEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.MenuTimeSlotProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ModifierGroupEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ModifierOptionEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductCategoryEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuModifierGroupBindingEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuPriceOverrideEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.StoreSkuAvailabilityEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaMenuTimeSlotProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaMenuTimeSlotRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaModifierGroupRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaModifierOptionRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductCategoryRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuModifierGroupBindingRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuPriceOverrideRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaStoreSkuAvailabilityRepository;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuQueryService implements UseCase {

    private final JpaStoreLookupRepository storeLookupRepository;
    private final JpaProductCategoryRepository categoryRepository;
    private final JpaProductRepository productRepository;
    private final JpaSkuRepository skuRepository;
    private final JpaStoreSkuAvailabilityRepository availabilityRepository;
    private final JpaSkuPriceOverrideRepository priceOverrideRepository;
    private final JpaSkuModifierGroupBindingRepository bindingRepository;
    private final JpaModifierGroupRepository modifierGroupRepository;
    private final JpaModifierOptionRepository modifierOptionRepository;
    private final JpaMenuTimeSlotRepository timeSlotRepository;
    private final JpaMenuTimeSlotProductRepository timeSlotProductRepository;

    public MenuQueryService(
            JpaStoreLookupRepository storeLookupRepository,
            JpaProductCategoryRepository categoryRepository,
            JpaProductRepository productRepository,
            JpaSkuRepository skuRepository,
            JpaStoreSkuAvailabilityRepository availabilityRepository,
            JpaSkuPriceOverrideRepository priceOverrideRepository,
            JpaSkuModifierGroupBindingRepository bindingRepository,
            JpaModifierGroupRepository modifierGroupRepository,
            JpaModifierOptionRepository modifierOptionRepository,
            JpaMenuTimeSlotRepository timeSlotRepository,
            JpaMenuTimeSlotProductRepository timeSlotProductRepository
    ) {
        this.storeLookupRepository = storeLookupRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.availabilityRepository = availabilityRepository;
        this.priceOverrideRepository = priceOverrideRepository;
        this.bindingRepository = bindingRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierOptionRepository = modifierOptionRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.timeSlotProductRepository = timeSlotProductRepository;
    }

    @Transactional(readOnly = true)
    public MenuQueryResultDto queryMenu(Long storeId, String diningMode, Long timeSlotId) {
        // 0. Validate store exists (tenant isolation: store data is scoped by storeId FK)
        storeLookupRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        // 1. Load categories
        List<ProductCategoryEntity> categories = categoryRepository
                .findByStoreIdOrderBySortOrderAscCategoryNameAsc(storeId);
        Map<Long, ProductCategoryEntity> categoryMap = new LinkedHashMap<>();
        for (ProductCategoryEntity c : categories) {
            if (c.isActive()) {
                categoryMap.put(c.getId(), c);
            }
        }

        // 2. Load products, filter by diningMode and timeSlot
        List<ProductEntity> allProducts = productRepository.findByStoreIdOrderByProductNameAsc(storeId);
        List<ProductEntity> filteredProducts = filterProducts(allProducts, diningMode, timeSlotId, storeId);

        if (filteredProducts.isEmpty()) {
            return new MenuQueryResultDto(List.of());
        }

        // 3. Load SKUs + availability + price overrides
        List<Long> productIds = filteredProducts.stream().map(ProductEntity::getId).toList();
        List<SkuEntity> allSkus = skuRepository.findByProductIdInOrderByProductIdAscIdAsc(productIds);
        List<Long> skuIds = allSkus.stream().map(SkuEntity::getId).toList();

        Map<Long, Boolean> availabilityMap = buildAvailabilityMap(storeId, skuIds);
        // Resolve slot_code for TIME_SLOT price overrides
        String timeSlotCode = null;
        if (timeSlotId != null) {
            MenuTimeSlotEntity resolvedSlot = timeSlotRepository.findById(timeSlotId).orElse(null);
            if (resolvedSlot != null) {
                timeSlotCode = resolvedSlot.getSlotCode();
            }
        }
        Map<Long, Long> effectivePriceMap = buildEffectivePriceMap(skuIds, storeId, diningMode, timeSlotCode, allSkus);

        // 4. Load modifier bindings + groups + options
        Map<Long, List<MenuModifierGroupDto>> skuModifierMap = buildSkuModifierMap(skuIds);

        // 5. Assemble result grouped by category
        Map<Long, List<MenuProductDto>> categoryProducts = new LinkedHashMap<>();
        Map<Long, ProductEntity> productMap = new HashMap<>();
        for (ProductEntity p : filteredProducts) {
            productMap.put(p.getId(), p);
        }

        for (SkuEntity sku : allSkus) {
            if (!availabilityMap.getOrDefault(sku.getId(), true)) {
                continue;
            }
            if (!"ACTIVE".equalsIgnoreCase(sku.getSkuStatus())) {
                continue;
            }

            ProductEntity product = productMap.get(sku.getProductId());
            if (product == null || !"ACTIVE".equalsIgnoreCase(product.getProductStatus())) {
                continue;
            }

            Long catId = product.getCategoryId();
            if (!categoryMap.containsKey(catId)) {
                continue;
            }

            long effectivePrice = effectivePriceMap.getOrDefault(sku.getId(), sku.getBasePriceCents());
            String skuImageUrl = sku.getImageId() != null ? "/api/v2/images/" + sku.getImageId() : null;
            List<MenuModifierGroupDto> modifiers = skuModifierMap.getOrDefault(sku.getId(), List.of());

            MenuSkuDto skuDto = new MenuSkuDto(
                    sku.getId(), sku.getSkuCode(), sku.getSkuName(),
                    effectivePrice, skuImageUrl, modifiers);

            categoryProducts.computeIfAbsent(catId, k -> new ArrayList<>());

            // Find or create product entry in this category
            List<MenuProductDto> productsInCategory = categoryProducts.get(catId);
            MenuProductDto existingProduct = null;
            int existingIndex = -1;
            for (int i = 0; i < productsInCategory.size(); i++) {
                if (productsInCategory.get(i).productId().equals(product.getId())) {
                    existingProduct = productsInCategory.get(i);
                    existingIndex = i;
                    break;
                }
            }

            if (existingProduct == null) {
                String productImageUrl = product.getImageId() != null ? "/api/v2/images/" + product.getImageId() : null;
                productsInCategory.add(new MenuProductDto(
                        product.getId(), product.getProductName(), productImageUrl,
                        new ArrayList<>(List.of(skuDto))));
            } else {
                List<MenuSkuDto> skus = new ArrayList<>(existingProduct.skus());
                skus.add(skuDto);
                productsInCategory.set(existingIndex, new MenuProductDto(
                        existingProduct.productId(), existingProduct.productName(),
                        existingProduct.imageUrl(), skus));
            }
        }

        // 6. Build final category list (preserve order, skip empty)
        List<MenuCategoryDto> result = new ArrayList<>();
        for (Map.Entry<Long, ProductCategoryEntity> entry : categoryMap.entrySet()) {
            List<MenuProductDto> products = categoryProducts.getOrDefault(entry.getKey(), List.of());
            if (!products.isEmpty()) {
                ProductCategoryEntity cat = entry.getValue();
                result.add(new MenuCategoryDto(cat.getId(), cat.getCategoryName(), products));
            }
        }

        return new MenuQueryResultDto(result);
    }

    private List<ProductEntity> filterProducts(List<ProductEntity> allProducts, String diningMode,
                                               Long timeSlotId, Long storeId) {
        List<ProductEntity> filtered = allProducts;

        // Filter by diningMode via product.menu_modes JSON
        if (diningMode != null && !diningMode.isBlank()) {
            filtered = filtered.stream()
                    .filter(p -> productMatchesDiningMode(p, diningMode))
                    .toList();
        }

        // Filter by timeSlot — only show products bound to this slot
        if (timeSlotId != null) {
            MenuTimeSlotEntity slot = timeSlotRepository.findById(timeSlotId).orElse(null);
            if (slot != null && !slot.getStoreId().equals(storeId)) {
                throw new IllegalArgumentException("Time slot " + timeSlotId + " does not belong to store " + storeId);
            }
            if (slot != null && slot.isActive()) {
                List<MenuTimeSlotProductEntity> slotProducts = timeSlotProductRepository
                        .findByTimeSlotIdAndIsVisible(timeSlotId, true);
                Set<Long> visibleProductIds = slotProducts.stream()
                        .map(MenuTimeSlotProductEntity::getProductId)
                        .collect(Collectors.toSet());
                // Empty slot = no products visible (not "show all")
                filtered = filtered.stream()
                        .filter(p -> visibleProductIds.contains(p.getId()))
                        .toList();
            }
        }

        return filtered;
    }

    private boolean productMatchesDiningMode(ProductEntity product, String diningMode) {
        List<String> modes = product.getMenuModes();
        if (modes == null || modes.isEmpty()) {
            return true; // no restriction = available for all modes
        }
        return modes.contains(diningMode);
    }

    private Map<Long, Boolean> buildAvailabilityMap(Long storeId, List<Long> skuIds) {
        if (skuIds.isEmpty()) return Map.of();
        Map<Long, Boolean> map = new HashMap<>();
        for (StoreSkuAvailabilityEntity a : availabilityRepository.findByStoreIdAndSkuIdIn(storeId, skuIds)) {
            map.put(a.getSkuId(), a.isAvailable());
        }
        return map;
    }

    /**
     * Price override chain. price_context is a scene type (BASE, TIME_SLOT, DELIVERY, etc.),
     * price_context_ref is the specific ref (slot ID, platform name).
     *
     * Resolution priority (per SKU, highest wins):
     *   4. store + scene-specific (e.g. store=1, context=TIME_SLOT, ref=slotId) → highest
     *   3. global scene-specific (store=NULL, context=TIME_SLOT, ref=slotId)
     *   2. store + BASE (e.g. store=1, context=BASE or DEFAULT)
     *   1. global BASE (store=NULL, context=BASE or DEFAULT)
     *   0. skus.base_price_cents → fallback
     */
    private Map<Long, Long> buildEffectivePriceMap(List<Long> skuIds, Long storeId,
                                                    String diningMode, String timeSlotCode,
                                                    List<SkuEntity> skus) {
        if (skuIds.isEmpty()) return Map.of();

        // Priority scores per SKU — higher score wins
        Map<Long, Long> priceMap = new HashMap<>();
        Map<Long, Integer> priorityMap = new HashMap<>();

        // Base prices (priority 0)
        for (SkuEntity sku : skus) {
            priceMap.put(sku.getId(), sku.getBasePriceCents());
            priorityMap.put(sku.getId(), 0);
        }

        List<SkuPriceOverrideEntity> overrides = priceOverrideRepository.findBySkuIdInAndIsActive(skuIds, true);

        for (SkuPriceOverrideEntity override : overrides) {
            int priority = scorePriceOverride(override, storeId, diningMode, timeSlotCode);
            if (priority <= 0) continue; // not applicable

            int existing = priorityMap.getOrDefault(override.getSkuId(), 0);
            if (priority > existing) {
                priceMap.put(override.getSkuId(), override.getOverridePriceCents());
                priorityMap.put(override.getSkuId(), priority);
            }
        }
        return priceMap;
    }

    /**
     * Score a price override's applicability. Returns 0 if not applicable.
     * Higher score = higher priority.
     */
    private int scorePriceOverride(SkuPriceOverrideEntity override, Long storeId,
                                   String diningMode, String timeSlotCode) {
        String context = override.getPriceContext();
        String contextRef = override.getPriceContextRef();
        boolean isBaseContext = context == null || context.isBlank()
                || "DEFAULT".equalsIgnoreCase(context) || "BASE".equalsIgnoreCase(context);
        boolean isStoreSpecific = override.getStoreId() != null;
        boolean storeMatches = isStoreSpecific && override.getStoreId().equals(storeId);

        // If store-specific but wrong store, skip entirely
        if (isStoreSpecific && !storeMatches) {
            return 0;
        }

        if (isBaseContext) {
            // BASE/DEFAULT context: always applicable
            return isStoreSpecific ? 2 : 1;
        }

        // Scene-specific context: must match the current query parameters
        if (!matchesSceneContext(context, contextRef, diningMode, timeSlotCode)) {
            return 0;
        }

        return isStoreSpecific ? 4 : 3;
    }

    private boolean matchesSceneContext(String context, String contextRef,
                                       String diningMode, String timeSlotCode) {
        if ("TIME_SLOT".equalsIgnoreCase(context)) {
            // price_context_ref stores slot_code (e.g. "LUNCH", "DINNER"), not numeric id
            return timeSlotCode != null && contextRef != null
                    && contextRef.equalsIgnoreCase(timeSlotCode);
        }
        if ("DELIVERY".equalsIgnoreCase(context) || "A_LA_CARTE".equalsIgnoreCase(context)
                || "BUFFET".equalsIgnoreCase(context)) {
            return context.equalsIgnoreCase(diningMode);
        }
        // Unknown context type — don't apply
        return false;
    }

    private Map<Long, List<MenuModifierGroupDto>> buildSkuModifierMap(List<Long> skuIds) {
        if (skuIds.isEmpty()) return Map.of();

        List<SkuModifierGroupBindingEntity> bindings = bindingRepository.findBySkuIdInOrderBySkuIdAscSortOrderAsc(skuIds);
        if (bindings.isEmpty()) return Map.of();

        List<Long> groupIds = bindings.stream().map(SkuModifierGroupBindingEntity::getModifierGroupId).distinct().toList();
        List<ModifierGroupEntity> groups = modifierGroupRepository.findByIdInOrderBySortOrderAsc(groupIds);
        Map<Long, ModifierGroupEntity> groupMap = new HashMap<>();
        for (ModifierGroupEntity g : groups) { groupMap.put(g.getId(), g); }

        List<ModifierOptionEntity> allOptions = modifierOptionRepository.findByGroupIdInOrderByGroupIdAscSortOrderAsc(groupIds);
        Map<Long, List<MenuModifierOptionDto>> optionsByGroup = new HashMap<>();
        for (ModifierOptionEntity o : allOptions) {
            optionsByGroup.computeIfAbsent(o.getGroupId(), k -> new ArrayList<>())
                    .add(new MenuModifierOptionDto(o.getId(), o.getOptionName(), o.getPriceAdjustmentCents()));
        }

        Map<Long, List<MenuModifierGroupDto>> result = new HashMap<>();
        for (SkuModifierGroupBindingEntity binding : bindings) {
            ModifierGroupEntity group = groupMap.get(binding.getModifierGroupId());
            if (group == null) continue;
            result.computeIfAbsent(binding.getSkuId(), k -> new ArrayList<>())
                    .add(new MenuModifierGroupDto(
                            group.getId(), group.getGroupName(), group.getSelectionType(),
                            group.isRequired(), group.getMinSelect(), group.getMaxSelect(),
                            optionsByGroup.getOrDefault(group.getId(), List.of())));
        }
        return result;
    }
}
