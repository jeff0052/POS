package com.developer.pos.payment.persistence.repository;

import com.developer.pos.payment.core.PaymentIntentEntity;
import com.developer.pos.payment.core.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, Long> {

    Optional<PaymentIntentEntity> findByIntentId(String intentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentIntentEntity p WHERE p.intentId = :intentId")
    Optional<PaymentIntentEntity> findByIntentIdForUpdate(String intentId);

    Optional<PaymentIntentEntity> findByProviderTransactionId(String providerTransactionId);

    Optional<PaymentIntentEntity> findFirstByStoreIdAndTableIdAndStatusInOrderByIdDesc(
            Long storeId, Long tableId, java.util.Collection<PaymentStatus> statuses);
}
