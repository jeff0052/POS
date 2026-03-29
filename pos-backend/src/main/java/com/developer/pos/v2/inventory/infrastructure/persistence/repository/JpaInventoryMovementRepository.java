package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaInventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long> {
    List<InventoryMovementEntity> findByInventoryItemIdOrderByCreatedAtDesc(Long inventoryItemId);
    List<InventoryMovementEntity> findByStoreIdAndMovementType(Long storeId, String movementType);
}
