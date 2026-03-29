package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.settlement.application.dto.PaymentRetryResultDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.PaymentAttemptEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaPaymentAttemptRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentRetryService {

    private final JpaPaymentAttemptRepository attemptRepo;
    private final JpaSettlementRecordRepository settlementRepo;
    private final VibeCashPaymentApplicationService vibecashService;

    public PaymentRetryService(JpaPaymentAttemptRepository attemptRepo,
                               JpaSettlementRecordRepository settlementRepo,
                               VibeCashPaymentApplicationService vibecashService) {
        this.attemptRepo = attemptRepo;
        this.settlementRepo = settlementRepo;
        this.vibecashService = vibecashService;
    }

    @Transactional
    public PaymentRetryResultDto switchMethod(Long storeId, Long tableId, String paymentAttemptId, String newPaymentScheme) {
        PaymentAttemptEntity old = attemptRepo.findByPaymentAttemptId(paymentAttemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + paymentAttemptId));

        if (old.getSettlementRecordId() == null) {
            throw new IllegalArgumentException("Attempt " + paymentAttemptId + " is not part of a stacking settlement");
        }

        var settlement = settlementRepo.findByIdForUpdate(old.getSettlementRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found"));

        // Scope validation — prevent cross-store/cross-table attacks
        if (!storeId.equals(old.getStoreId()) || !tableId.equals(old.getTableId())) {
            throw new IllegalArgumentException(
                    "Attempt " + paymentAttemptId + " does not belong to store " + storeId + " / table " + tableId);
        }

        if (!"PENDING".equals(settlement.getFinalStatus())) {
            throw new IllegalStateException("Settlement is not PENDING: " + settlement.getFinalStatus());
        }
        if (!"FAILED".equals(old.getAttemptStatus())) {
            throw new IllegalStateException("Only FAILED attempts can be switched, got: " + old.getAttemptStatus());
        }
        if (old.getRetryCount() >= old.getMaxRetries()) {
            throw new IllegalStateException("Exceeded max retries (" + old.getMaxRetries() + ") for attempt: " + paymentAttemptId);
        }

        // CAS mark old attempt as REPLACED
        int affected = attemptRepo.markReplacedCas(paymentAttemptId, "FAILED");
        if (affected == 0) throw new IllegalStateException("Concurrent modification on attempt: " + paymentAttemptId);

        // Create new attempt
        PaymentAttemptEntity newAttempt = new PaymentAttemptEntity();
        newAttempt.setPaymentAttemptId("PAT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase());
        newAttempt.setProvider(old.getProvider());
        newAttempt.setPaymentMethod(old.getPaymentMethod());
        newAttempt.setPaymentScheme(newPaymentScheme);
        newAttempt.setStoreId(old.getStoreId());
        newAttempt.setTableId(old.getTableId());
        newAttempt.setTableSessionId(old.getTableSessionId());
        newAttempt.setSessionRef(old.getSessionRef());
        newAttempt.setSettlementAmountCents(old.getSettlementAmountCents());
        newAttempt.setCurrencyCode(old.getCurrencyCode());
        newAttempt.setSettlementRecordId(old.getSettlementRecordId());
        newAttempt.setParentAttemptId(old.getId());
        newAttempt.setRetryCount(old.getRetryCount() + 1);
        newAttempt.setMaxRetries(old.getMaxRetries());
        newAttempt.setAttemptStatus("PENDING_CUSTOMER");
        newAttempt.setCreatedAt(OffsetDateTime.now());
        newAttempt.setUpdatedAt(OffsetDateTime.now());
        newAttempt = attemptRepo.save(newAttempt);

        // Back-fill replacedByAttemptId on old attempt
        old.setReplacedByAttemptId(newAttempt.getId());
        attemptRepo.save(old);

        // Create real VibeCash checkout link for the new attempt
        var attemptDto = vibecashService.createPaymentLinkForSavedAttempt(newAttempt.getId());
        return new PaymentRetryResultDto(newAttempt.getPaymentAttemptId(), attemptDto.checkoutUrl());
    }
}
