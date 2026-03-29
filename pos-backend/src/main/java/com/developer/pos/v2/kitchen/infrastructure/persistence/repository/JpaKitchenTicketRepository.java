package com.developer.pos.v2.kitchen.infrastructure.persistence.repository;

import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaKitchenTicketRepository extends JpaRepository<KitchenTicketEntity, Long> {

    List<KitchenTicketEntity> findByStoreIdAndStationIdAndTicketStatusIn(
            Long storeId, Long stationId, List<String> ticketStatuses);

    List<KitchenTicketEntity> findByStoreIdAndTicketStatusIn(
            Long storeId, List<String> ticketStatuses);

    List<KitchenTicketEntity> findBySubmittedOrderId(Long submittedOrderId);

    /** Used by TicketRoutingService to determine round_number for a table */
    @Query("SELECT COUNT(DISTINCT kt.submittedOrderId) FROM V2KitchenTicketEntity kt WHERE kt.tableId = :tableId")
    long countDistinctSubmittedOrdersWithTicketsByTableId(@Param("tableId") Long tableId);
}
