package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.BuffetPackageDto;
import com.developer.pos.v2.catalog.application.dto.BuffetPackageItemDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageItemEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageItemRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.catalog.interfaces.rest.request.BindSkuRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.CreateBuffetPackageRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpdateBindingRequest;
import com.developer.pos.v2.catalog.interfaces.rest.request.UpdateBuffetPackageRequest;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.image.application.service.ImageUploadService;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class BuffetPackageService implements UseCase {

    private final JpaBuffetPackageRepository packageRepository;
    private final JpaBuffetPackageItemRepository itemRepository;
    private final JpaStoreLookupRepository storeLookupRepository;
    private final JpaSkuRepository skuRepository;
    private final ImageUploadService imageUploadService;
    private final ObjectMapper objectMapper;

    public BuffetPackageService(
            JpaBuffetPackageRepository packageRepository,
            JpaBuffetPackageItemRepository itemRepository,
            JpaStoreLookupRepository storeLookupRepository,
            JpaSkuRepository skuRepository,
            ImageUploadService imageUploadService,
            ObjectMapper objectMapper
    ) {
        this.packageRepository = packageRepository;
        this.itemRepository = itemRepository;
        this.storeLookupRepository = storeLookupRepository;
        this.skuRepository = skuRepository;
        this.imageUploadService = imageUploadService;
        this.objectMapper = objectMapper;
    }

    // ─── Package CRUD ────────────────────────────────────────────────────

    @Transactional
    public BuffetPackageDto createPackage(Long storeId, CreateBuffetPackageRequest req) {
        enforceBuffetManage();
        enforceStoreAccess(storeId);

        AuthenticatedActor actor = AuthContext.current();
        BuffetPackageEntity entity = new BuffetPackageEntity(
                storeId,
                req.packageCode(),
                req.packageName(),
                req.description(),
                req.priceCents(),
                req.childPriceCents(),
                req.childAgeMax(),
                req.durationMinutes(),
                req.warningBeforeMinutes(),
                req.overtimeFeePerMinuteCents(),
                req.overtimeGraceMinutes(),
                req.maxOvertimeMinutes(),
                "ACTIVE",
                writeJson(req.applicableTimeSlots()),
                writeJson(req.applicableDays()),
                req.sortOrder(),
                req.imageId(),
                actor.userId()
        );
        entity = packageRepository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public BuffetPackageDto updatePackage(Long storeId, Long packageId, UpdateBuffetPackageRequest req) {
        enforceBuffetManage();
        enforceStoreAccess(storeId);

        AuthenticatedActor actor = AuthContext.current();
        BuffetPackageEntity entity = findPackageAndVerifyStore(packageId, storeId);
        entity.update(
                req.packageCode(),
                req.packageName(),
                req.description(),
                req.priceCents(),
                req.childPriceCents(),
                req.childAgeMax(),
                req.durationMinutes(),
                req.warningBeforeMinutes(),
                req.overtimeFeePerMinuteCents(),
                req.overtimeGraceMinutes(),
                req.maxOvertimeMinutes(),
                entity.getPackageStatus(),
                writeJson(req.applicableTimeSlots()),
                writeJson(req.applicableDays()),
                req.sortOrder(),
                req.imageId(),
                actor.userId()
        );
        entity = packageRepository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public void deletePackage(Long storeId, Long packageId) {
        enforceBuffetManage();
        enforceStoreAccess(storeId);

        AuthenticatedActor actor = AuthContext.current();
        BuffetPackageEntity entity = findPackageAndVerifyStore(packageId, storeId);
        entity.update(
                entity.getPackageCode(),
                entity.getPackageName(),
                entity.getDescription(),
                entity.getPriceCents(),
                entity.getChildPriceCents(),
                entity.getChildAgeMax(),
                entity.getDurationMinutes(),
                entity.getWarningBeforeMinutes(),
                entity.getOvertimeFeePerMinuteCents(),
                entity.getOvertimeGraceMinutes(),
                entity.getMaxOvertimeMinutes(),
                "INACTIVE",
                entity.getApplicableTimeSlots(),
                entity.getApplicableDays(),
                entity.getSortOrder(),
                entity.getImageId(),
                actor.userId()
        );
        packageRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<BuffetPackageDto> listPackages(Long storeId) {
        enforceBuffetViewOrManage();
        enforceStoreAccess(storeId);
        return packageRepository
                .findByStoreIdAndPackageStatusOrderBySortOrderAsc(storeId, "ACTIVE")
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BuffetPackageDto getPackage(Long storeId, Long packageId) {
        enforceBuffetViewOrManage();
        enforceStoreAccess(storeId);
        return toDto(findPackageAndVerifyStore(packageId, storeId));
    }

    // ─── SKU Binding ─────────────────────────────────────────────────────

    @Transactional
    public BuffetPackageItemDto bindSku(Long storeId, Long packageId, BindSkuRequest req) {
        enforceBuffetManage();
        enforceStoreAccess(storeId);
        findPackageAndVerifyStore(packageId, storeId);

        itemRepository.findByPackageIdAndSkuId(packageId, req.skuId()).ifPresent(existing -> {
            throw new IllegalStateException("SKU " + req.skuId() + " is already bound to package " + packageId);
        });

        BuffetPackageItemEntity item = new BuffetPackageItemEntity(
                packageId, req.skuId(), req.inclusionType(),
                req.surchargeCents(), req.maxQtyPerPerson(), req.sortOrder()
        );
        item = itemRepository.save(item);
        return toItemDto(item);
    }

    @Transactional
    public BuffetPackageItemDto updatePackageItem(Long storeId, Long packageId, Long itemId, UpdateBindingRequest req) {
        enforceBuffetManage();
        enforceStoreAccess(storeId);
        findPackageAndVerifyStore(packageId, storeId);

        BuffetPackageItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Package item not found: " + itemId));
        if (!Objects.equals(item.getPackageId(), packageId)) {
            throw new IllegalArgumentException("Item does not belong to package: " + packageId);
        }
        item.update(req.inclusionType(), req.surchargeCents(), req.maxQtyPerPerson(), req.sortOrder());
        item = itemRepository.save(item);
        return toItemDto(item);
    }

    @Transactional
    public void unbindSku(Long storeId, Long packageId, Long itemId) {
        enforceBuffetManage();
        enforceStoreAccess(storeId);
        findPackageAndVerifyStore(packageId, storeId);

        BuffetPackageItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Package item not found: " + itemId));
        if (!Objects.equals(item.getPackageId(), packageId)) {
            throw new IllegalArgumentException("Item does not belong to package: " + packageId);
        }
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public List<BuffetPackageItemDto> listPackageItems(Long storeId, Long packageId) {
        enforceBuffetViewOrManage();
        enforceStoreAccess(storeId);
        findPackageAndVerifyStore(packageId, storeId);
        return itemRepository.findByPackageIdOrderBySortOrderAsc(packageId)
                .stream()
                .map(this::toItemDto)
                .toList();
    }

    // ─── Permission Helpers ───────────────────────────────────────────────

    private void enforceBuffetManage() {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasPermission("BUFFET_MANAGE")) {
            throw new SecurityException("Missing BUFFET_MANAGE permission");
        }
    }

    private void enforceBuffetViewOrManage() {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasPermission("MENU_VIEW") && !actor.hasPermission("BUFFET_MANAGE")) {
            throw new SecurityException("Missing MENU_VIEW or BUFFET_MANAGE permission");
        }
    }

    private void enforceStoreAccess(Long storeId) {
        AuthenticatedActor actor = AuthContext.current();
        if (actor.merchantId() != null && actor.merchantId() != 0L) {
            StoreEntity store = storeLookupRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
            if (!Objects.equals(store.getMerchantId(), actor.merchantId())) {
                throw new SecurityException("Store does not belong to your merchant");
            }
            if (actor.accessibleStoreIds() != null && !actor.accessibleStoreIds().isEmpty()
                    && !actor.hasStoreAccess(storeId)) {
                throw new SecurityException("You do not have access to store: " + storeId);
            }
        }
    }

    // ─── Private Helpers ──────────────────────────────────────────────────

    private BuffetPackageEntity findPackageAndVerifyStore(Long packageId, Long storeId) {
        BuffetPackageEntity pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Buffet package not found: " + packageId));
        if (!Objects.equals(pkg.getStoreId(), storeId)) {
            throw new IllegalArgumentException("Buffet package not found in this store");
        }
        return pkg;
    }

    private BuffetPackageDto toDto(BuffetPackageEntity entity) {
        String imageUrl = entity.getImageId() != null
                ? imageUploadService.resolvePublicUrl(String.valueOf(entity.getImageId()))
                : null;
        return new BuffetPackageDto(
                entity.getId(),
                entity.getPackageCode(),
                entity.getPackageName(),
                entity.getDescription(),
                entity.getPriceCents(),
                entity.getChildPriceCents(),
                entity.getChildAgeMax(),
                entity.getDurationMinutes(),
                entity.getWarningBeforeMinutes(),
                entity.getOvertimeFeePerMinuteCents(),
                entity.getOvertimeGraceMinutes(),
                entity.getMaxOvertimeMinutes(),
                entity.getPackageStatus(),
                readJson(entity.getApplicableTimeSlots()),
                readJson(entity.getApplicableDays()),
                entity.getSortOrder(),
                imageUrl
        );
    }

    private BuffetPackageItemDto toItemDto(BuffetPackageItemEntity item) {
        SkuEntity sku = skuRepository.findById(item.getSkuId()).orElse(null);
        String skuCode = sku != null ? sku.getSkuCode() : null;
        String skuName = sku != null ? sku.getSkuName() : null;
        return new BuffetPackageItemDto(
                item.getId(),
                item.getSkuId(),
                skuCode,
                skuName,
                item.getInclusionType(),
                item.getSurchargeCents(),
                item.getMaxQtyPerPerson(),
                item.getSortOrder()
        );
    }

    private String writeJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize JSON", e);
        }
    }

    private List<String> readJson(String raw) {
        try {
            return raw == null || raw.isBlank()
                    ? List.of()
                    : objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
