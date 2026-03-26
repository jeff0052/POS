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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
            when(storeTableRepository.findByIdAndStoreId(TABLE_ID, STORE_ID))
                    .thenReturn(Optional.of(buildStoreTable()));

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

            StoreEntity store = buildStore();
            when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));

            StoreTableEntity table = buildStoreTable();
            when(storeTableRepository.findByIdAndStoreId(TABLE_ID, STORE_ID)).thenReturn(Optional.of(table));

            when(tableSessionRepository.save(any())).thenAnswer(inv -> {
                TableSessionEntity s = inv.getArgument(0);
                ReflectionTestUtils.setField(s, "id", 1L);
                return s;
            });
            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> {
                ActiveTableOrderEntity e = inv.getArgument(0);
                ReflectionTestUtils.setField(e, "id", 1L);
                return e;
            });
            when(storeTableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReplaceActiveTableOrderItemsCommand command = new ReplaceActiveTableOrderItemsCommand(
                    STORE_ID, TABLE_ID, "T01", null, null,
                    List.of(new ReplaceActiveTableOrderItemsCommand.ReplaceActiveTableOrderItemInput(
                            100L, "SKU001", "Test SKU", 2, 1000L, "POS"
                    ))
            );

            ActiveTableOrderDto result = service.replaceItems(command);

            assertNotNull(result);
            verify(activeTableOrderRepository).save(any());
        }

        @Test
        @DisplayName("amount calculation: 1*1000 + 2*500 = 2000 cents")
        void calculatesAmountCorrectly() {
            when(activeTableOrderRepository.findByStoreIdAndTableId(STORE_ID, TABLE_ID))
                    .thenReturn(Optional.empty());

            StoreEntity store = buildStore();
            when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));

            StoreTableEntity table = buildStoreTable();
            when(storeTableRepository.findByIdAndStoreId(TABLE_ID, STORE_ID)).thenReturn(Optional.of(table));

            when(tableSessionRepository.save(any())).thenAnswer(inv -> {
                TableSessionEntity s = inv.getArgument(0);
                ReflectionTestUtils.setField(s, "id", 1L);
                return s;
            });
            when(activeTableOrderRepository.save(any())).thenAnswer(inv -> {
                ActiveTableOrderEntity e = inv.getArgument(0);
                ReflectionTestUtils.setField(e, "id", 1L);
                return e;
            });
            when(storeTableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReplaceActiveTableOrderItemsCommand command = new ReplaceActiveTableOrderItemsCommand(
                    STORE_ID, TABLE_ID, "T01", null, null,
                    List.of(
                            new ReplaceActiveTableOrderItemsCommand.ReplaceActiveTableOrderItemInput(100L, "SKU001", "A", 1, 1000L, "POS"),
                            new ReplaceActiveTableOrderItemsCommand.ReplaceActiveTableOrderItemInput(101L, "SKU002", "B", 2, 500L, "POS")
                    )
            );

            ActiveTableOrderDto result = service.replaceItems(command);

            assertNotNull(result);
            // 1*1000 + 2*500 = 2000
            assertEquals(2000L, result.originalAmountCents());
        }
    }

    private StoreEntity buildStore() {
        StoreEntity store = org.springframework.beans.BeanUtils.instantiateClass(StoreEntity.class);
        ReflectionTestUtils.setField(store, "id", STORE_ID);
        ReflectionTestUtils.setField(store, "merchantId", 1L);
        ReflectionTestUtils.setField(store, "storeCode", "S001");
        ReflectionTestUtils.setField(store, "storeName", "Test Store");
        return store;
    }

    private StoreTableEntity buildStoreTable() {
        StoreTableEntity table = org.springframework.beans.BeanUtils.instantiateClass(StoreTableEntity.class);
        ReflectionTestUtils.setField(table, "id", TABLE_ID);
        ReflectionTestUtils.setField(table, "storeId", STORE_ID);
        ReflectionTestUtils.setField(table, "tableCode", "T01");
        ReflectionTestUtils.setField(table, "tableName", "Table 1");
        ReflectionTestUtils.setField(table, "tableStatus", "AVAILABLE");
        return table;
    }

    private ActiveTableOrderEntity buildActiveOrder(ActiveOrderStatus status) {
        ActiveTableOrderEntity entity = new ActiveTableOrderEntity();
        ReflectionTestUtils.setField(entity, "id", 1L);
        entity.setActiveOrderId("ATO-TEST-001");
        entity.setOrderNo("ATO1234567890");
        entity.setMerchantId(1L);
        entity.setStoreId(STORE_ID);
        entity.setTableId(TABLE_ID);
        entity.setDiningType("DINE_IN");
        entity.setStatus(status);
        entity.setOriginalAmountCents(5000L);
        entity.setMemberDiscountCents(0L);
        entity.setPromotionDiscountCents(0L);
        entity.setPayableAmountCents(5000L);
        return entity;
    }
}
