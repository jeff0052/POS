package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.inventory.application.dto.InventoryDrivenPromotionDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryDrivenPromotionEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.RecipeEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryBatchRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryDrivenPromotionRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryItemRepository;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaRecipeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryPromotionScanService {

    private static final Logger log = LoggerFactory.getLogger(InventoryPromotionScanService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BigDecimal OVERSTOCK_MULTIPLIER = new BigDecimal("3");
    private static final int MAX_EXPIRY_SCAN_DAYS = 14;

    private final JpaInventoryBatchRepository batchRepository;
    private final JpaInventoryItemRepository itemRepository;
    private final JpaRecipeRepository recipeRepository;
    private final JpaInventoryDrivenPromotionRepository promotionRepository;
    private final StoreAccessEnforcer enforcer;

    public InventoryPromotionScanService(
            JpaInventoryBatchRepository batchRepository,
            JpaInventoryItemRepository itemRepository,
            JpaRecipeRepository recipeRepository,
            JpaInventoryDrivenPromotionRepository promotionRepository,
            StoreAccessEnforcer enforcer) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.recipeRepository = recipeRepository;
        this.promotionRepository = promotionRepository;
        this.enforcer = enforcer;
    }

    @Transactional
    public List<InventoryDrivenPromotionDto> scanAll(Long storeId) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("INVENTORY_VIEW");
        List<InventoryDrivenPromotionDto> all = new ArrayList<>();
        all.addAll(scanNearExpiry(storeId));
        all.addAll(scanOverstock(storeId));
        return all;
    }

    @Transactional
    public List<InventoryDrivenPromotionDto> scanNearExpiry(Long storeId) {
        LocalDate warningDate = LocalDate.now().plusDays(MAX_EXPIRY_SCAN_DAYS);
        List<InventoryBatchEntity> expiring = batchRepository.findExpiringSoon(storeId, warningDate);
        List<InventoryDrivenPromotionDto> drafts = new ArrayList<>();

        for (InventoryBatchEntity batch : expiring) {
            if (isDuplicateDraft(storeId, batch.getInventoryItemId())) continue;

            List<Long> affectedSkuIds = findAffectedSkuIds(batch.getInventoryItemId());
            if (affectedSkuIds.isEmpty()) continue;

            long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
            BigDecimal discount = computeExpiryDiscount(daysToExpiry);

            InventoryDrivenPromotionEntity entity = new InventoryDrivenPromotionEntity(
                storeId, batch.getInventoryItemId(), batch.getId(),
                "NEAR_EXPIRY", discount, serializeSkuIds(affectedSkuIds),
                batch.getExpiryDate().atStartOfDay()
            );
            entity = promotionRepository.save(entity);
            drafts.add(toDto(entity));
        }
        return drafts;
    }

    @Transactional
    public List<InventoryDrivenPromotionDto> scanOverstock(Long storeId) {
        List<InventoryItemEntity> overstocked = itemRepository.findOverstock(storeId, OVERSTOCK_MULTIPLIER);
        List<InventoryDrivenPromotionDto> drafts = new ArrayList<>();

        for (InventoryItemEntity item : overstocked) {
            if (isDuplicateDraft(storeId, item.getId())) continue;

            List<Long> affectedSkuIds = findAffectedSkuIds(item.getId());
            if (affectedSkuIds.isEmpty()) continue;

            InventoryDrivenPromotionEntity entity = new InventoryDrivenPromotionEntity(
                storeId, item.getId(), null,
                "OVERSTOCK", new BigDecimal("15.00"), serializeSkuIds(affectedSkuIds),
                LocalDateTime.now().plusDays(30)
            );
            entity = promotionRepository.save(entity);
            drafts.add(toDto(entity));
        }
        return drafts;
    }

    private BigDecimal computeExpiryDiscount(long daysToExpiry) {
        if (daysToExpiry <= 3) return new BigDecimal("30.00");
        if (daysToExpiry <= 7) return new BigDecimal("20.00");
        return new BigDecimal("10.00");
    }

    private List<Long> findAffectedSkuIds(Long inventoryItemId) {
        return recipeRepository.findByInventoryItemId(inventoryItemId).stream()
            .map(RecipeEntity::getSkuId)
            .distinct()
            .collect(Collectors.toList());
    }

    private boolean isDuplicateDraft(Long storeId, Long inventoryItemId) {
        return promotionRepository.existsByStoreIdAndInventoryItemIdAndDraftStatus(
            storeId, inventoryItemId, "DRAFT");
    }

    private String serializeSkuIds(List<Long> skuIds) {
        try { return MAPPER.writeValueAsString(skuIds); }
        catch (Exception e) { return "[]"; }
    }

    InventoryDrivenPromotionDto toDto(InventoryDrivenPromotionEntity e) {
        return new InventoryDrivenPromotionDto(e.getId(), e.getStoreId(),
            e.getInventoryItemId(), e.getInventoryBatchId(), e.getTriggerType(),
            e.getSuggestedDiscountPercent(), e.getSuggestedSkuIds(),
            e.getDraftStatus(), e.getPromotionRuleId(), e.getExpiresAt(), e.getCreatedAt());
    }
}
