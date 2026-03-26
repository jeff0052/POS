package com.developer.pos.v2.order.infrastructure.persistence.repository;

import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface JpaSubmittedOrderRepository extends JpaRepository<SubmittedOrderEntity, Long> {
    List<SubmittedOrderEntity> findByTableSessionIdAndSettlementStatusOrderByIdAsc(Long tableSessionId, String settlementStatus);

    List<SubmittedOrderEntity> findAllByStoreIdOrderByIdDesc(Long storeId);

    List<SubmittedOrderEntity> findByStoreIdAndSubmittedAtBetweenOrderByIdDesc(Long storeId, OffsetDateTime start, OffsetDateTime end);
}
