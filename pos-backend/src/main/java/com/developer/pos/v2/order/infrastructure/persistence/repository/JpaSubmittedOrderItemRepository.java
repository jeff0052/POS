package com.developer.pos.v2.order.infrastructure.persistence.repository;

import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface JpaSubmittedOrderItemRepository extends JpaRepository<SubmittedOrderItemEntity, Long> {

    /**
     * Find submitted order items by IDs, restricted to items belonging to orders
     * in a given table session. Used to validate refund item IDs belong to the
     * settlement's order chain.
     */
    @Query("SELECT i FROM SubmittedOrderItemEntity i " +
           "WHERE i.id IN :itemIds " +
           "AND i.submittedOrder.tableSessionId IN " +
           "  (SELECT s.id FROM TableSessionEntity s WHERE s.tableId = :tableId AND s.storeId = :storeId)")
    List<SubmittedOrderItemEntity> findByIdsAndTableContext(
            @Param("itemIds") Collection<Long> itemIds,
            @Param("tableId") Long tableId,
            @Param("storeId") Long storeId);
}
