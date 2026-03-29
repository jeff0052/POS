package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementPaymentHoldEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface JpaSettlementPaymentHoldRepository extends JpaRepository<SettlementPaymentHoldEntity, Long> {

    List<SettlementPaymentHoldEntity> findAllByTableSessionIdAndHoldStatus(Long tableSessionId, String holdStatus);

    List<SettlementPaymentHoldEntity> findAllBySettlementRecordIdAndHoldStatus(Long settlementRecordId, String holdStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM SettlementPaymentHoldEntity h WHERE h.settlementRecordId = :settlementRecordId AND h.holdStatus = 'HELD'")
    List<SettlementPaymentHoldEntity> findHeldBySettlementForUpdate(@Param("settlementRecordId") Long settlementRecordId);

    List<SettlementPaymentHoldEntity> findAllByHoldStatusAndHeldAtBefore(String holdStatus, OffsetDateTime before);
}
