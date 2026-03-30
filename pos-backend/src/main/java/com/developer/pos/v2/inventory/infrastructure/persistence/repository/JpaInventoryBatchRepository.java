package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface JpaInventoryBatchRepository extends JpaRepository<InventoryBatchEntity, Long> {
    /** FEFO order (First Expiry, First Out) — batches closest to expiry are consumed first; null expiry dates sort last */
    @Query("SELECT b FROM V2InventoryBatchEntity b WHERE b.storeId = :storeId AND b.inventoryItemId = :itemId AND b.batchStatus = :status ORDER BY CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END, b.expiryDate ASC, b.id ASC")
    List<InventoryBatchEntity> findActiveByStoreAndItemFifo(@Param("storeId") Long storeId, @Param("itemId") Long itemId, @Param("status") String status);
    List<InventoryBatchEntity> findByStoreIdAndInventoryItemId(Long storeId, Long inventoryItemId);

    @Query("SELECT b FROM V2InventoryBatchEntity b " +
           "WHERE b.storeId = :storeId AND b.batchStatus = 'ACTIVE' " +
           "AND b.expiryDate IS NOT NULL AND b.expiryDate <= :warningDate " +
           "ORDER BY b.expiryDate ASC")
    List<InventoryBatchEntity> findExpiringSoon(@Param("storeId") Long storeId,
                                                 @Param("warningDate") LocalDate warningDate);
}
