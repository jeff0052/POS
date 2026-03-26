package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaSettlementRecordRepository extends JpaRepository<SettlementRecordEntity, Long> {
    boolean existsByActiveOrderId(String activeOrderId);

    Optional<SettlementRecordEntity> findByActiveOrderId(String activeOrderId);

    List<SettlementRecordEntity> findAllByStoreIdAndCashierIdAndCreatedAtBetweenOrderByIdAsc(
            Long storeId,
            Long cashierId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
