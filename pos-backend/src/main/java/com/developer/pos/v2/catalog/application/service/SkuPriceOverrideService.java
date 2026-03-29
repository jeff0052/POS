package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.SkuPriceOverrideDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuPriceOverrideEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuPriceOverrideRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class SkuPriceOverrideService implements UseCase {

    private final JpaSkuPriceOverrideRepository overrideRepository;
    private final JpaSkuRepository skuRepository;
    private final JpaProductRepository productRepository;
    private final JpaStoreLookupRepository storeLookupRepository;

    public SkuPriceOverrideService(
            JpaSkuPriceOverrideRepository overrideRepository,
            JpaSkuRepository skuRepository,
            JpaProductRepository productRepository,
            JpaStoreLookupRepository storeLookupRepository
    ) {
        this.overrideRepository = overrideRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.storeLookupRepository = storeLookupRepository;
    }

    @Transactional(readOnly = true)
    public List<SkuPriceOverrideDto> listOverrides(Long skuId) {
        enforceMenuView();
        enforceSkuOwnership(skuId);
        return overrideRepository.findBySkuIdAndIsActive(skuId, true).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SkuPriceOverrideDto createOverride(Long skuId, Long storeId, String priceContext,
                                              String priceContextRef, long overridePriceCents, boolean active) {
        enforceMenuManage();
        enforceSkuOwnership(skuId);
        if (storeId != null) {
            enforceStoreOwnership(storeId);
        }

        SkuPriceOverrideEntity entity = new SkuPriceOverrideEntity(
                skuId, storeId, priceContext != null ? priceContext : "DEFAULT",
                priceContextRef, overridePriceCents, active);
        entity = overrideRepository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public void deleteOverride(Long overrideId) {
        enforceMenuManage();
        SkuPriceOverrideEntity entity = overrideRepository.findById(overrideId)
                .orElseThrow(() -> new IllegalArgumentException("Price override not found: " + overrideId));
        enforceSkuOwnership(entity.getSkuId());
        overrideRepository.delete(entity);
    }

    private void enforceMenuView() {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasPermission("MENU_VIEW") && !actor.hasPermission("MENU_MANAGE")) {
            throw new SecurityException("MENU_VIEW permission required");
        }
    }

    private void enforceMenuManage() {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasPermission("MENU_MANAGE")) {
            throw new SecurityException("MENU_MANAGE permission required");
        }
    }

    private void enforceSkuOwnership(Long skuId) {
        Long merchantId = enforceCallerMerchant();
        AuthenticatedActor actor = AuthContext.current();
        SkuEntity sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuId));
        ProductEntity product = productRepository.findById(sku.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found for SKU: " + skuId));
        Long storeId = product.getStoreId();
        storeLookupRepository.findById(storeId)
                .filter(store -> store.getMerchantId().equals(merchantId))
                .orElseThrow(() -> new SecurityException("SKU does not belong to your merchant"));
        if (actor.accessibleStoreIds() != null && !actor.accessibleStoreIds().isEmpty()
                && !actor.hasStoreAccess(storeId)) {
            throw new SecurityException("You do not have access to this SKU's store: " + storeId);
        }
    }

    private void enforceStoreOwnership(Long storeId) {
        Long merchantId = enforceCallerMerchant();
        AuthenticatedActor actor = AuthContext.current();
        storeLookupRepository.findById(storeId)
                .filter(store -> Objects.equals(store.getMerchantId(), merchantId))
                .orElseThrow(() -> new SecurityException("Store does not belong to your merchant"));
        if (actor.accessibleStoreIds() != null && !actor.accessibleStoreIds().isEmpty()
                && !actor.hasStoreAccess(storeId)) {
            throw new SecurityException("You do not have access to store: " + storeId);
        }
    }

    private Long enforceCallerMerchant() {
        AuthenticatedActor actor = AuthContext.current();
        if (actor.merchantId() == null || actor.merchantId() == 0L) {
            throw new IllegalArgumentException("Merchant context required");
        }
        return actor.merchantId();
    }

    private SkuPriceOverrideDto toDto(SkuPriceOverrideEntity e) {
        return new SkuPriceOverrideDto(
                e.getId(), e.getSkuId(), e.getStoreId(),
                e.getPriceContext(), e.getPriceContextRef(),
                e.getOverridePriceCents(), e.isActive());
    }
}
