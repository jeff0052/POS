package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaInventoryItemRepository extends JpaRepository<InventoryItemEntity, Long> {
    List<InventoryItemEntity> findByStoreIdAndItemStatusOrderByItemNameAsc(Long storeId, String itemStatus);
    Optional<InventoryItemEntity> findByStoreIdAndItemCode(Long storeId, String itemCode);
    boolean existsByStoreIdAndItemCode(Long storeId, String itemCode);
}
