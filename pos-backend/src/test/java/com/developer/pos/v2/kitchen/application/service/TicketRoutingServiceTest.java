package com.developer.pos.v2.kitchen.application.service;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenStationEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenTicketEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenStationRepository;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenTicketRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketRoutingServiceTest {

    @Mock JpaKitchenStationRepository stationRepository;
    @Mock JpaKitchenTicketRepository ticketRepository;
    @Mock JpaSkuRepository skuRepository;
    @Mock JpaStoreTableRepository storeTableRepository;

    private TicketRoutingService buildService() {
        return new TicketRoutingService(stationRepository, ticketRepository,
            skuRepository, storeTableRepository);
    }

    private SubmittedOrderEntity buildOrder(Long storeId, Long tableId, List<Long> skuIds) {
        SubmittedOrderEntity order = new SubmittedOrderEntity();
        try {
            setField(order, "storeId", storeId);
            setField(order, "tableId", tableId);
            setField(order, "id", 100L);
            List<SubmittedOrderItemEntity> items = skuIds.stream().map(skuId -> {
                SubmittedOrderItemEntity item = new SubmittedOrderItemEntity();
                item.setSkuId(skuId);
                item.setSkuNameSnapshot("SKU-" + skuId);
                item.setQuantity(1);
                item.setSkuCodeSnapshot("SC" + skuId);
                item.setUnitPriceSnapshotCents(1000L);
                item.setLineTotalCents(1000L);
                return item;
            }).toList();
            setField(order, "items", items);
        } catch (Exception e) { throw new RuntimeException(e); }
        return order;
    }

    private SkuEntity buildSku(Long skuId, Long stationId) {
        SkuEntity sku = mock(SkuEntity.class);
        when(sku.getId()).thenReturn(skuId);
        when(sku.getStationId()).thenReturn(stationId);
        when(sku.getSkuName()).thenReturn("SKU-" + skuId);
        return sku;
    }

    private KitchenStationEntity buildStation(Long id, Long storeId) {
        KitchenStationEntity s = new KitchenStationEntity(storeId, "S" + id, "Station " + id, 0);
        try { setField(s, "id", id); } catch (Exception e) { throw new RuntimeException(e); }
        return s;
    }

    private StoreTableEntity buildTable(Long id, String tableCode) {
        StoreTableEntity table = mock(StoreTableEntity.class);
        when(table.getId()).thenReturn(id);
        when(table.getTableCode()).thenReturn(tableCode);
        return table;
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        var f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try { return clazz.getDeclaredField(name); }
        catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) return findField(clazz.getSuperclass(), name);
            throw e;
        }
    }

    @Test
    void routeOrder_singleStation_createsOneTicket() {
        SubmittedOrderEntity order = buildOrder(10L, 5L, List.of(1L, 2L));
        SkuEntity sku1 = buildSku(1L, 20L);
        SkuEntity sku2 = buildSku(2L, 20L); // same station
        KitchenStationEntity station = buildStation(20L, 10L);
        StoreTableEntity table = buildTable(5L, "T01");

        when(storeTableRepository.findById(5L)).thenReturn(Optional.of(table));
        when(skuRepository.findAllById(any())).thenReturn(List.of(sku1, sku2));
        when(stationRepository.findById(20L)).thenReturn(Optional.of(station));
        when(ticketRepository.countDistinctSubmittedOrdersWithTicketsByTableId(5L)).thenReturn(0L);
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<KitchenTicketEntity> tickets = buildService().routeOrder(order);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getStationId()).isEqualTo(20L);
        assertThat(tickets.get(0).getItems()).hasSize(2);
        assertThat(tickets.get(0).getRoundNumber()).isEqualTo(1);
    }

    @Test
    void routeOrder_multipleStations_createsMultipleTickets() {
        SubmittedOrderEntity order = buildOrder(10L, 5L, List.of(1L, 2L));
        SkuEntity sku1 = buildSku(1L, 20L);
        SkuEntity sku2 = buildSku(2L, 21L); // different station
        KitchenStationEntity station1 = buildStation(20L, 10L);
        KitchenStationEntity station2 = buildStation(21L, 10L);
        StoreTableEntity table = buildTable(5L, "T01");

        when(storeTableRepository.findById(5L)).thenReturn(Optional.of(table));
        when(skuRepository.findAllById(any())).thenReturn(List.of(sku1, sku2));
        when(stationRepository.findById(20L)).thenReturn(Optional.of(station1));
        when(stationRepository.findById(21L)).thenReturn(Optional.of(station2));
        when(ticketRepository.countDistinctSubmittedOrdersWithTicketsByTableId(5L)).thenReturn(0L);
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<KitchenTicketEntity> tickets = buildService().routeOrder(order);

        assertThat(tickets).hasSize(2);
    }

    @Test
    void routeOrder_nullStationId_routesToDefaultStation() {
        SubmittedOrderEntity order = buildOrder(10L, 5L, List.of(1L));
        SkuEntity sku = buildSku(1L, null); // no station assigned
        KitchenStationEntity defaultStation = buildStation(99L, 10L);
        StoreTableEntity table = buildTable(5L, "T01");

        when(storeTableRepository.findById(5L)).thenReturn(Optional.of(table));
        when(skuRepository.findAllById(any())).thenReturn(List.of(sku));
        when(stationRepository.findFirstByStoreIdAndStationStatusOrderBySortOrderAscIdAsc(10L, "ACTIVE"))
            .thenReturn(Optional.of(defaultStation));
        when(ticketRepository.countDistinctSubmittedOrdersWithTicketsByTableId(5L)).thenReturn(0L);
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<KitchenTicketEntity> tickets = buildService().routeOrder(order);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getStationId()).isEqualTo(99L);
    }

    @Test
    void routeOrder_noActiveStation_throwsIllegalState() {
        SubmittedOrderEntity order = buildOrder(10L, 5L, List.of(1L));
        SkuEntity sku = buildSku(1L, null);
        StoreTableEntity table = buildTable(5L, "T01");

        when(storeTableRepository.findById(5L)).thenReturn(Optional.of(table));
        when(skuRepository.findAllById(any())).thenReturn(List.of(sku));
        when(stationRepository.findFirstByStoreIdAndStationStatusOrderBySortOrderAscIdAsc(10L, "ACTIVE"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildService().routeOrder(order))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No active kitchen station");
    }

    @Test
    void routeOrder_expiredHeartbeat_marksStationOffline() {
        SubmittedOrderEntity order = buildOrder(10L, 5L, List.of(1L));
        SkuEntity sku = buildSku(1L, 20L);
        KitchenStationEntity station = buildStation(20L, 10L);
        // Simulate expired heartbeat
        station.setLastHeartbeatAt(java.time.LocalDateTime.now().minusSeconds(200));
        StoreTableEntity table = buildTable(5L, "T01");

        when(storeTableRepository.findById(5L)).thenReturn(Optional.of(table));
        when(skuRepository.findAllById(any())).thenReturn(List.of(sku));
        when(stationRepository.findById(20L)).thenReturn(Optional.of(station));
        when(stationRepository.save(station)).thenReturn(station);
        when(ticketRepository.countDistinctSubmittedOrdersWithTicketsByTableId(5L)).thenReturn(0L);
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        buildService().routeOrder(order);

        assertThat(station.isOnline()).isFalse();
        verify(stationRepository).save(station); // OFFLINE state persisted
    }

    @Test
    void routeOrder_roundNumberIncrements() {
        SubmittedOrderEntity order = buildOrder(10L, 5L, List.of(1L));
        SkuEntity sku = buildSku(1L, 20L);
        KitchenStationEntity station = buildStation(20L, 10L);
        StoreTableEntity table = buildTable(5L, "T01");

        when(storeTableRepository.findById(5L)).thenReturn(Optional.of(table));
        when(skuRepository.findAllById(any())).thenReturn(List.of(sku));
        when(stationRepository.findById(20L)).thenReturn(Optional.of(station));
        when(ticketRepository.countDistinctSubmittedOrdersWithTicketsByTableId(5L)).thenReturn(2L); // 2 prior rounds
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<KitchenTicketEntity> tickets = buildService().routeOrder(order);

        assertThat(tickets.get(0).getRoundNumber()).isEqualTo(3);
    }
}
