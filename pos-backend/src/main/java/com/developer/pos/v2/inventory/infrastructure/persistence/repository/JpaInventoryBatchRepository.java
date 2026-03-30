package com.developer.pos.v2.inventory.infrastructure.persistence.repository;

import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface JpaInventoryBatchRepository extends JpaRepository<InventoryBatchEntity, Long> {
    /** FIFO order: oldest expiry first, null expiry last */
    List<InventoryBatchEntity> findByInventoryItemIdAndBatchStatusOrderByExpiryDateAscIdAsc(
        Long inventoryItemId, String batchStatus);
    /** FIFO order with store isolation */
    List<InventoryBatchEntity> findByStoreIdAndInventoryItemIdAndBatchStatusOrderByExpiryDateAscIdAsc(
        Long storeId, Long inventoryItemId, String batchStatus);
    List<InventoryBatchEntity> findByStoreIdAndInventoryItemId(Long storeId, Long inventoryItemId);

    @Query("SELECT b FROM V2InventoryBatchEntity b " +
           "WHERE b.storeId = :storeId AND b.batchStatus = 'ACTIVE' " +
           "AND b.expiryDate IS NOT NULL AND b.expiryDate <= :warningDate " +
           "ORDER BY b.expiryDate ASC")
    List<InventoryBatchEntity> findExpiringSoon(@Param("storeId") Long storeId,
                                                 @Param("warningDate") LocalDate warningDate);
}
