package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaInventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long> {
    /** @deprecated Use {@link #findByStoreIdAndInventoryItemIdOrderByCreatedAtDesc} for store-scoped queries. */
    @Deprecated
    List<InventoryMovementEntity> findByInventoryItemIdOrderByCreatedAtDesc(Long inventoryItemId);

    List<InventoryMovementEntity> findByStoreIdAndInventoryItemIdOrderByCreatedAtDesc(Long storeId, Long inventoryItemId);

    // NOTE: Returns unbounded results. Add Pageable parameter for production use with high-volume stores.
    List<InventoryMovementEntity> findByStoreIdAndMovementType(Long storeId, String movementType);
}
