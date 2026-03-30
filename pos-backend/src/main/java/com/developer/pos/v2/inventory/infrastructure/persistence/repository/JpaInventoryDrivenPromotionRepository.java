package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryDrivenPromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaInventoryDrivenPromotionRepository extends JpaRepository<InventoryDrivenPromotionEntity, Long> {
    List<InventoryDrivenPromotionEntity> findByStoreIdAndDraftStatusOrderByCreatedAtDesc(Long storeId, String draftStatus);
    List<InventoryDrivenPromotionEntity> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    boolean existsByStoreIdAndInventoryItemIdAndDraftStatus(Long storeId, Long inventoryItemId, String draftStatus);
    boolean existsByStoreIdAndInventoryItemIdAndTriggerTypeAndDraftStatus(Long storeId, Long inventoryItemId, String triggerType, String draftStatus);
}
