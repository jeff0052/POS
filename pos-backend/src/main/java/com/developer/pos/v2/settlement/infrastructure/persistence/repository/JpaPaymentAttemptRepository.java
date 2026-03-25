package com.developer.pos.v2.settlement.infrastructure.persistence.repository;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.PaymentAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface JpaPaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, Long> {

    Optional<PaymentAttemptEntity> findByPaymentAttemptId(String paymentAttemptId);

    Optional<PaymentAttemptEntity> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

    Optional<PaymentAttemptEntity> findFirstByStoreIdAndTableIdAndAttemptStatusInOrderByIdDesc(
            Long storeId,
            Long tableId,
            Collection<String> attemptStatuses
    );
}
