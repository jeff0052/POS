package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.inventory.application.service.StockDeductionService;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionHitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashierSettlementStockDeductionTest {

    @Mock JpaActiveTableOrderRepository activeTableOrderRepository;
    @Mock JpaSettlementRecordRepository settlementRecordRepository;
    @Mock JpaStoreTableRepository storeTableRepository;
    @Mock JpaMemberRepository memberRepository;
    @Mock JpaPromotionHitRepository promotionHitRepository;
    @Mock ObjectMapper objectMapper;
    @Mock JpaTableSessionRepository tableSessionRepository;
    @Mock JpaSubmittedOrderRepository submittedOrderRepository;
    @Mock StockDeductionService stockDeductionService;

    @InjectMocks CashierSettlementApplicationService service;

    @Test
    void collectForTable_callsStockDeductionAfterSettlement() {
        Long storeId = 10L;
        Long tableId = 1L;

        TableSessionEntity session = mock(TableSessionEntity.class);
        when(session.getId()).thenReturn(100L);
        when(session.getSessionId()).thenReturn("SESSION-001");
        when(session.getMergedIntoSessionId()).thenReturn(null);

        when(tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN"))
            .thenReturn(Optional.of(session));
        when(settlementRecordRepository.findByActiveOrderId("SESSION-001")).thenReturn(Optional.empty());
        when(tableSessionRepository.findAllByMergedIntoSessionIdAndSessionStatus(100L, "OPEN"))
            .thenReturn(List.of());

        SubmittedOrderEntity order = mock(SubmittedOrderEntity.class);
        when(order.getMerchantId()).thenReturn(1L);
        when(order.getPayableAmountCents()).thenReturn(1000L);
        when(submittedOrderRepository.findByTableSessionIdInAndSettlementStatusOrderByIdAsc(anyList(), eq("UNPAID")))
            .thenReturn(List.of(order));

        when(settlementRecordRepository.saveAndFlush(any())).thenReturn(mock(SettlementRecordEntity.class));

        StoreTableEntity table = mock(StoreTableEntity.class);
        when(storeTableRepository.findByIdAndStoreId(tableId, storeId)).thenReturn(Optional.of(table));

        CollectCashierSettlementCommand command = new CollectCashierSettlementCommand(
            null, null, "CASH", 1000L
        );

        service.collectForTable(storeId, tableId, command);

        verify(stockDeductionService).deductForOrders(eq(storeId), eq(List.of(order)));
    }
}
