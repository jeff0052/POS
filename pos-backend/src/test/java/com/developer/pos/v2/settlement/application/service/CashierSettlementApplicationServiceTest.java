package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionHitRepository;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.dto.CashierSettlementResultDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashierSettlementApplicationServiceTest {

    @Mock private JpaActiveTableOrderRepository activeTableOrderRepository;
    @Mock private JpaSubmittedOrderRepository submittedOrderRepository;
    @Mock private JpaTableSessionRepository tableSessionRepository;
    @Mock private JpaSettlementRecordRepository settlementRecordRepository;
    @Mock private JpaStoreTableRepository storeTableRepository;
    @Mock private JpaMemberRepository memberRepository;
    @Mock private JpaPromotionHitRepository promotionHitRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private CashierSettlementApplicationService service;

    private static final Long STORE_ID = 1001L;
    private static final Long TABLE_ID = 1L;

    @Nested
    @DisplayName("collectForTable")
    class CollectForTable {

        @Test
        @DisplayName("throws when no open table session found")
        void throws_whenNoOpenSession() {
            when(tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(STORE_ID, TABLE_ID, "OPEN"))
                    .thenReturn(Optional.empty());

            CollectCashierSettlementCommand command = new CollectCashierSettlementCommand(
                    null, 1L, "CASH", 5000L
            );

            assertThrows(IllegalArgumentException.class,
                    () -> service.collectForTable(STORE_ID, TABLE_ID, command));
        }

        @Test
        @DisplayName("throws when no unpaid submitted orders")
        void throws_whenNoUnpaidOrders() {
            TableSessionEntity session = new TableSessionEntity();
            ReflectionTestUtils.setField(session, "id", 10L);
            session.setSessionId("SESS-001");
            when(tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(STORE_ID, TABLE_ID, "OPEN"))
                    .thenReturn(Optional.of(session));
            when(submittedOrderRepository.findByTableSessionIdAndSettlementStatusOrderByIdAsc(10L, "UNPAID"))
                    .thenReturn(List.of());

            CollectCashierSettlementCommand command = new CollectCashierSettlementCommand(
                    null, 1L, "CASH", 5000L
            );

            assertThrows(IllegalStateException.class,
                    () -> service.collectForTable(STORE_ID, TABLE_ID, command));
        }

        @Test
        @DisplayName("creates settlement record for valid table collection")
        void createsSettlement_whenValid() {
            TableSessionEntity session = new TableSessionEntity();
            ReflectionTestUtils.setField(session, "id", 10L);
            session.setSessionId("SESS-001");
            when(tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(STORE_ID, TABLE_ID, "OPEN"))
                    .thenReturn(Optional.of(session));

            SubmittedOrderEntity unpaidOrder = new SubmittedOrderEntity();
            ReflectionTestUtils.setField(unpaidOrder, "id", 1L);
            unpaidOrder.setMerchantId(1L);
            unpaidOrder.setPayableAmountCents(5000L);
            when(submittedOrderRepository.findByTableSessionIdAndSettlementStatusOrderByIdAsc(10L, "UNPAID"))
                    .thenReturn(List.of(unpaidOrder));

            when(settlementRecordRepository.saveAndFlush(any())).thenAnswer(inv -> {
                SettlementRecordEntity e = inv.getArgument(0);
                ReflectionTestUtils.setField(e, "id", 1L);
                return e;
            });
            when(submittedOrderRepository.saveAllAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            when(tableSessionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            when(activeTableOrderRepository.findByStoreIdAndTableId(STORE_ID, TABLE_ID))
                    .thenReturn(Optional.empty());

            StoreTableEntity table = org.springframework.beans.BeanUtils.instantiateClass(StoreTableEntity.class);
            ReflectionTestUtils.setField(table, "id", TABLE_ID);
            ReflectionTestUtils.setField(table, "storeId", STORE_ID);
            ReflectionTestUtils.setField(table, "tableCode", "T01");
            ReflectionTestUtils.setField(table, "tableName", "Table 1");
            ReflectionTestUtils.setField(table, "tableStatus", "PENDING_SETTLEMENT");
            when(storeTableRepository.findByIdAndStoreId(TABLE_ID, STORE_ID))
                    .thenReturn(Optional.of(table));
            when(storeTableRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            CollectCashierSettlementCommand command = new CollectCashierSettlementCommand(
                    null, 1L, "CASH", 5000L
            );

            CashierSettlementResultDto result = service.collectForTable(STORE_ID, TABLE_ID, command);

            assertNotNull(result);
            assertEquals("SESS-001", result.activeOrderId());
            verify(settlementRecordRepository).saveAndFlush(any());
        }
    }
}
