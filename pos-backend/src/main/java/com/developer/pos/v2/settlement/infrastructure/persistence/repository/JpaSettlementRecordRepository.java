package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface JpaSettlementRecordRepository extends JpaRepository<SettlementRecordEntity, Long> {
    List<SettlementRecordEntity> findAllByStoreIdAndCashierIdAndCreatedAtBetweenOrderByIdAsc(
            Long storeId,
            Long cashierId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
