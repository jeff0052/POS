package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface JpaInventoryItemRepository extends JpaRepository<InventoryItemEntity, Long> {
    List<InventoryItemEntity> findByStoreIdAndItemStatusOrderByItemNameAsc(Long storeId, String itemStatus);
    Optional<InventoryItemEntity> findByStoreIdAndItemCode(Long storeId, String itemCode);
    boolean existsByStoreIdAndItemCode(Long storeId, String itemCode);

    @Query("SELECT i FROM V2InventoryItemEntity i " +
           "WHERE i.storeId = :storeId AND i.itemStatus = 'ACTIVE' " +
           "AND i.currentStock < i.safetyStock " +
           "ORDER BY i.itemName ASC")
    List<InventoryItemEntity> findLowStockByStoreId(@Param("storeId") Long storeId);
}
