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
     * in a specific table session. Scoped to session (not just table) to prevent
     * items from older sessions on the same physical table from passing validation.
     */
    @Query("SELECT i FROM SubmittedOrderItemEntity i " +
           "WHERE i.id IN :itemIds " +
           "AND i.submittedOrder.tableSessionId = :sessionId")
    List<SubmittedOrderItemEntity> findByIdsAndSessionId(
            @Param("itemIds") Collection<Long> itemIds,
            @Param("sessionId") Long sessionId);
}
