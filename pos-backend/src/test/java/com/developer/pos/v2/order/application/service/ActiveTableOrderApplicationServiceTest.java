package com.developer.pos.v2.order.application.service;

import com.developer.pos.v2.order.application.command.ReplaceActiveTableOrderItemsCommand;
import com.developer.pos.v2.order.application.dto.ActiveTableOrderDto;
import com.developer.pos.v2.order.application.query.GetActiveTableOrderQuery;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderItemEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ActiveTableOrderApplicationServiceTest {

    @Mock private JpaActiveTableOrderRepository activeTableOrderRepository;
    @Mock private JpaStoreRepository storeRepository;
    @Mock private JpaStoreLookupRepository storeLookupRepository;
    @Mock private JpaStoreTableRepository storeTableRepository;
    @Mock private JpaMemberRepository memberRepository;
    @Mock private JpaTableSessionRepository tableSessionRepository;
    @Mock private JpaSubmittedOrderRepository submittedOrderRepository;

    @InjectMocks
    private ActiveTableOrderApplicationService service;

    private static final Long STORE_ID = 1001L;
    private static final Long TABLE_ID = 1L;

    @Nested
    @DisplayName("getActiveTableOrder")
    class GetActiveTableOrder {

        @Test
        @DisplayName("returns DTO when active order exists and not settled")
        void returnsDto_whenActiveOrderExists() {
            ActiveTableOrderEntity entity = buildActiveOrder(ActiveOrderStatus.DRAFT);
            when(activeTableOrderRepository.findByStoreIdAndTableId(STORE_ID, TABLE_ID))
                    .thenReturn(Optional.of(entity));

            ActiveTableOrderDto result = service.getActiveTableOrder(
                    new GetActiveTableOrderQuery(STORE_ID, TABLE_ID));

            assertNotNull(result);
            assertEquals(entity.getActiveOrderId(), result.activeOrderId());
        }

        @Test
        @DisplayName("returns null when no active order")
        void returnsNull_whenNoActiveOrder() {
            when(activeTableOrderRepository.findByStoreIdAndTableId(STORE_ID, TABLE_ID))
                    .thenReturn(Optional.empty());

            ActiveTableOrderDto result = service.getActiveTableOrder(
                    new GetActiveTableOrderQuery(STORE_ID, TABLE_ID));

            assertNull(result);
        }

        @Test
        @DisplayName("returns null when order is settled")
        void returnsNull_whenOrderSettled() {
            ActiveTableOrderEntity entity = buildActiveOrder(ActiveOrderStatus.SETTLED);
            when(activeTableOrderRepository.findByStoreIdAndTableId(STORE_ID, TABLE_ID))
                    .thenReturn(Optional.of(entity));

            ActiveTableOrderDto result = service.getActiveTableOrder(
                    new GetActiveTableOrderQuery(STORE_ID, TABLE_ID));

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("replaceItems")
    class ReplaceItems {

        @Test
        @DisplayName("creates new order when none exists")
        void createsNewOrder_whenNoneExists() {
            when(activeTableOrderRepository.findByStoreIdAndTableId(STORE_ID, TABLE_ID))
                    .thenReturn(Optional.empty());

            StoreEntity store = new StoreEntity();
            store.setId(STORE_ID);
            when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));

            StoreTableEntity table = new StoreTableEntity();
            table.setId(TABLE_ID);
            table.setTableCode("T01");
            table.setTableName("Table 1");
            when(storeTableRepository.findById(TABLE_ID)).thenReturn(Optional.of(table));

            when(tableSessionRepository.save(any())).thenAnswer(inv -> {
                TableSessionEntity s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> {
                ActiveTableOrderEntity e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });

            ReplaceActiveTableOrderItemsCommand command = new ReplaceActiveTableOrderItemsCommand(
                    STORE_ID, TABLE_ID, 1L, null,
                    List.of(new ReplaceActiveTableOrderItemsCommand.OrderItemInput(
                            100L, "SKU001", "Test SKU", 1000L, 2, "POS"
                    ))
            );

            ActiveTableOrderDto result = service.replaceItems(command);

            assertNotNull(result);
            verify(activeTableOrderRepository).save(any());
            verify(tableSessionRepository).save(any());
        }

        @Test
        @DisplayName("amount calculation: 2 items at 1000 cents = 2000 cents")
        void calculatesAmountCorrectly() {
            when(activeTableOrderRepository.findByStoreIdAndTableId(STORE_ID, TABLE_ID))
                    .thenReturn(Optional.empty());

            StoreEntity store = new StoreEntity();
            store.setId(STORE_ID);
            when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));

            StoreTableEntity table = new StoreTableEntity();
            table.setId(TABLE_ID);
            table.setTableCode("T01");
            table.setTableName("Table 1");
            when(storeTableRepository.findById(TABLE_ID)).thenReturn(Optional.of(table));

            when(tableSessionRepository.save(any())).thenAnswer(inv -> {
                TableSessionEntity s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });
            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> {
                ActiveTableOrderEntity e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });

            ReplaceActiveTableOrderItemsCommand command = new ReplaceActiveTableOrderItemsCommand(
                    STORE_ID, TABLE_ID, 1L, null,
                    List.of(
                            new ReplaceActiveTableOrderItemsCommand.OrderItemInput(100L, "SKU001", "A", 1000L, 1, "POS"),
                            new ReplaceActiveTableOrderItemsCommand.OrderItemInput(101L, "SKU002", "B", 500L, 2, "POS")
                    )
            );

            ActiveTableOrderDto result = service.replaceItems(command);

            assertNotNull(result);
            // 1*1000 + 2*500 = 2000
            assertEquals(2000L, result.originalAmountCents());
        }
    }

    private ActiveTableOrderEntity buildActiveOrder(ActiveOrderStatus status) {
        ActiveTableOrderEntity entity = new ActiveTableOrderEntity();
        entity.setId(1L);
        entity.setActiveOrderId("ATO-TEST-001");
        entity.setStoreId(STORE_ID);
        entity.setTableId(TABLE_ID);
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
