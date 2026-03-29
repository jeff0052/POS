package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaSettlementRecordRepository extends JpaRepository<SettlementRecordEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SettlementRecordEntity s WHERE s.id = :id")
    Optional<SettlementRecordEntity> findByIdForUpdate(Long id);
    boolean existsByActiveOrderId(String activeOrderId);

    Optional<SettlementRecordEntity> findByActiveOrderId(String activeOrderId);

    List<SettlementRecordEntity> findAllByStoreIdAndCashierIdAndCreatedAtBetweenOrderByIdAsc(
            Long storeId,
            Long cashierId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    Optional<SettlementRecordEntity> findByStackingSessionIdAndFinalStatus(Long stackingSessionId, String finalStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SettlementRecordEntity s WHERE s.stackingSessionId = :stackingSessionId AND s.finalStatus = :status")
    Optional<SettlementRecordEntity> findByStackingSessionIdAndFinalStatusForUpdate(@Param("stackingSessionId") Long stackingSessionId, @Param("status") String status);

    @Query("SELECT s FROM SettlementRecordEntity s WHERE s.finalStatus = 'PENDING' AND s.createdAt < :before")
    List<SettlementRecordEntity> findPendingOlderThan(@Param("before") OffsetDateTime before);
}
