package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaInventoryBatchRepository extends JpaRepository<InventoryBatchEntity, Long> {
    /** FIFO order: oldest expiry first, null expiry last */
    List<InventoryBatchEntity> findByInventoryItemIdAndBatchStatusOrderByExpiryDateAscIdAsc(
        Long inventoryItemId, String batchStatus);
    List<InventoryBatchEntity> findByStoreIdAndInventoryItemId(Long storeId, Long inventoryItemId);
}
