package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.PaymentAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaPaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, Long> {

    Optional<PaymentAttemptEntity> findByPaymentAttemptId(String paymentAttemptId);

    Optional<PaymentAttemptEntity> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

    Optional<PaymentAttemptEntity> findFirstByStoreIdAndTableIdAndAttemptStatusInOrderByIdDesc(
            Long storeId,
            Long tableId,
            Collection<String> attemptStatuses
    );

    List<PaymentAttemptEntity> findBySettlementRecordIdOrderByCreatedAtDesc(Long settlementRecordId);

    // CAS markReplaced
    @Modifying
    @Query(value = """
        UPDATE payment_attempts
        SET attempt_status = 'REPLACED'
        WHERE payment_attempt_id = :attemptId AND attempt_status = :currentStatus
          AND replaced_by_attempt_id IS NULL
        """, nativeQuery = true)
    int markReplacedCas(@Param("attemptId") String attemptId,
                        @Param("currentStatus") String currentStatus);
}
