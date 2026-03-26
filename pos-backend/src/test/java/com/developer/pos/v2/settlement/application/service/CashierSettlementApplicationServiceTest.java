package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.settlement.application.command.CollectForTableCommand;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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

    @InjectMocks
    private CashierSettlementApplicationService service;

    @Nested
    @DisplayName("collectForTable")
    class CollectForTable {

        @Test
        @DisplayName("throws when no active order found")
        void throws_whenNoActiveOrder() {
            when(activeTableOrderRepository.findByStoreIdAndTableId(1001L, 1L))
                    .thenReturn(Optional.empty());

            CollectForTableCommand command = new CollectForTableCommand(
                    1001L, 1L, "CASH", null, null, null
            );

            assertThrows(IllegalArgumentException.class,
                    () -> service.collectForTable(command));
        }

        @Test
        @DisplayName("throws when order not in PENDING_SETTLEMENT status")
        void throws_whenNotPendingSettlement() {
            ActiveTableOrderEntity order = buildActiveOrder(ActiveOrderStatus.DRAFT);
            when(activeTableOrderRepository.findByStoreIdAndTableId(1001L, 1L))
                    .thenReturn(Optional.of(order));

            CollectForTableCommand command = new CollectForTableCommand(
                    1001L, 1L, "CASH", null, null, null
            );

            assertThrows(IllegalStateException.class,
                    () -> service.collectForTable(command));
        }

        @Test
        @DisplayName("creates settlement record for valid PENDING_SETTLEMENT order")
        void createsSettlement_whenValid() {
            ActiveTableOrderEntity order = buildActiveOrder(ActiveOrderStatus.PENDING_SETTLEMENT);
            order.setSessionId(10L);
            when(activeTableOrderRepository.findByStoreIdAndTableId(1001L, 1L))
                    .thenReturn(Optional.of(order));

            TableSessionEntity session = new TableSessionEntity();
            session.setId(10L);
            session.setSessionId("SESS-001");
            when(tableSessionRepository.findById(10L)).thenReturn(Optional.of(session));

            when(submittedOrderRepository.findBySessionId(10L)).thenReturn(List.of());
            when(settlementRecordRepository.save(any())).thenAnswer(inv -> {
                SettlementRecordEntity e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });
            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CollectForTableCommand command = new CollectForTableCommand(
                    1001L, 1L, "CASH", null, null, null
            );

            var result = service.collectForTable(command);

            assertNotNull(result);
            verify(settlementRecordRepository).save(any());
            verify(activeTableOrderRepository).save(argThat(e ->
                    e.getStatus() == ActiveOrderStatus.SETTLED));
        }
    }

    private ActiveTableOrderEntity buildActiveOrder(ActiveOrderStatus status) {
        ActiveTableOrderEntity entity = new ActiveTableOrderEntity();
        entity.setId(1L);
        entity.setActiveOrderId("ATO-TEST-001");
        entity.setStoreId(1001L);
        entity.setTableId(1L);
        entity.setStatus(status);
        entity.setOriginalAmountCents(5000L);
        entity.setMemberDiscountCents(0L);
        entity.setPromotionDiscountCents(0L);
        entity.setPayableAmountCents(5000L);
        entity.setItems(new ArrayList<>());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }
}
