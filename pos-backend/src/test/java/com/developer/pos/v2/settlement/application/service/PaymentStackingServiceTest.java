package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCouponRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.settlement.application.dto.StackingPreviewDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementPaymentHoldRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaPaymentAttemptRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentStackingServiceTest {

    @Mock JpaTableSessionRepository sessionRepo;
    @Mock JpaSubmittedOrderRepository submittedOrderRepo;
    @Mock JpaStoreTableRepository storeTableRepo;
    @Mock JpaSettlementRecordRepository settlementRepo;
    @Mock JpaSettlementPaymentHoldRepository holdRepo;
    @Mock JpaPaymentAttemptRepository attemptRepo;
    @Mock JpaMemberAccountRepository memberAccountRepo;
    @Mock JpaMemberCouponRepository couponRepo;
    @Mock CouponLockingService couponLockingService;
    @Mock TableSettlementFinalizer finalizer;
    @Mock VibeCashPaymentApplicationService vibecashService;
    @InjectMocks PaymentStackingService service;

    private TableSessionEntity buildSession(Long id, Long tableId, Long storeId) {
        TableSessionEntity s = new TableSessionEntity();
        setId(s, id);
        s.setTableId(tableId);
        s.setStoreId(storeId);
        s.setSessionStatus("ACTIVE");
        return s;
    }

    private SubmittedOrderEntity buildOrder(Long sessionId, long amountCents) {
        SubmittedOrderEntity o = new SubmittedOrderEntity();
        o.setTableSessionId(sessionId);
        o.setPayableAmountCents(amountCents);
        return o;
    }

    private void setId(Object entity, Long id) {
        try {
            var f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void previewStacking_noMember_returnsExternalOnlyPreview() {
        TableSessionEntity session = buildSession(1L, 10L, 5L);
        StoreTableEntity table = new StoreTableEntity();

        when(storeTableRepo.findByStoreIdAndId(5L, 10L)).thenReturn(Optional.of(table));
        when(sessionRepo.findActiveByTableId(10L)).thenReturn(Optional.of(session));
        when(sessionRepo.findAllByMergedIntoSessionId(1L)).thenReturn(Collections.emptyList());
        when(submittedOrderRepo.findAllByTableSessionIdIn(List.of(1L)))
                .thenReturn(List.of(buildOrder(1L, 10000L)));

        StackingPreviewDto dto = service.previewStacking(5L, 10L);

        assertThat(dto.totalPayableCents()).isEqualTo(10000L);
        assertThat(dto.externalPaymentCents()).isEqualTo(10000L);
        assertThat(dto.pointsDeductCents()).isNull();
    }

    @Test
    void previewStacking_mergedChildTable_throws() {
        StoreTableEntity table = new StoreTableEntity();
        table.setMergedIntoTableId(99L);
        when(storeTableRepo.findByStoreIdAndId(5L, 10L)).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> service.previewStacking(5L, 10L))
                .isInstanceOf(IllegalStateException.class);
    }
}
