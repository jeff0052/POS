package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.settlement.infrastructure.persistence.entity.PaymentAttemptEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaPaymentAttemptRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRetryServiceTest {

    @Mock JpaPaymentAttemptRepository attemptRepo;
    @Mock JpaSettlementRecordRepository settlementRepo;
    @Mock VibeCashPaymentApplicationService vibecashService;
    @InjectMocks PaymentRetryService service;

    @Test
    void switchMethod_maxRetriesExceeded_throws() {
        PaymentAttemptEntity attempt = buildAttempt("pa_001", "FAILED", 3, 3, 1L);
        when(attemptRepo.findByPaymentAttemptId("pa_001")).thenReturn(Optional.of(attempt));

        SettlementRecordEntity settlement = new SettlementRecordEntity();
        settlement.setFinalStatus("PENDING");
        when(settlementRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.switchMethod(5L, 10L, "pa_001", "WECHAT_QR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max retries");
    }

    @Test
    void switchMethod_attemptNotFailed_throws() {
        PaymentAttemptEntity attempt = buildAttempt("pa_001", "EXPIRED", 0, 3, 1L);
        when(attemptRepo.findByPaymentAttemptId("pa_001")).thenReturn(Optional.of(attempt));

        SettlementRecordEntity settlement = new SettlementRecordEntity();
        settlement.setFinalStatus("PENDING");
        when(settlementRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.switchMethod(5L, 10L, "pa_001", "WECHAT_QR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void switchMethod_settlementNotPending_throws() {
        PaymentAttemptEntity attempt = buildAttempt("pa_001", "FAILED", 0, 3, 1L);
        when(attemptRepo.findByPaymentAttemptId("pa_001")).thenReturn(Optional.of(attempt));

        SettlementRecordEntity settlement = new SettlementRecordEntity();
        settlement.setFinalStatus("CANCELLED");
        when(settlementRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.switchMethod(5L, 10L, "pa_001", "WECHAT_QR"))
                .isInstanceOf(IllegalStateException.class);
    }

    private PaymentAttemptEntity buildAttempt(String id, String status, int retryCount, int maxRetries, Long settlementId) {
        PaymentAttemptEntity a = new PaymentAttemptEntity();
        a.setPaymentAttemptId(id);
        a.setAttemptStatus(status);
        a.setRetryCount(retryCount);
        a.setMaxRetries(maxRetries);
        a.setSettlementRecordId(settlementId);
        a.setStoreId(5L);
        a.setTableId(10L);
        a.setCreatedAt(OffsetDateTime.now());
        a.setUpdatedAt(OffsetDateTime.now());
        return a;
    }
}
