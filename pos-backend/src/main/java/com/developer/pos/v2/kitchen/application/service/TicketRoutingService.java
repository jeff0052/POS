package com.developer.pos.v2.kitchen.application.service;

import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenStationEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenTicketEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenTicketItemEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenStationRepository;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenTicketRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketRoutingService implements UseCase {

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 90;

    private final JpaKitchenStationRepository stationRepository;
    private final JpaKitchenTicketRepository ticketRepository;
    private final JpaSkuRepository skuRepository;
    private final JpaStoreTableRepository storeTableRepository;

    public TicketRoutingService(JpaKitchenStationRepository stationRepository,
                                 JpaKitchenTicketRepository ticketRepository,
                                 JpaSkuRepository skuRepository,
                                 JpaStoreTableRepository storeTableRepository) {
        this.stationRepository = stationRepository;
        this.ticketRepository = ticketRepository;
        this.skuRepository = skuRepository;
        this.storeTableRepository = storeTableRepository;
    }

    /**
     * Routes a submitted order to kitchen stations, creating one ticket per station.
     * Called synchronously within the same transaction as submitToKitchen().
     */
    @Transactional
    public List<KitchenTicketEntity> routeOrder(SubmittedOrderEntity order) {
        StoreTableEntity table = storeTableRepository.findById(order.getTableId())
            .orElseThrow(() -> new IllegalStateException("Table not found: " + order.getTableId()));

        // Step 1: batch-load SKU station assignments
        List<Long> skuIds = order.getItems().stream()
            .map(SubmittedOrderItemEntity::getSkuId)
            .distinct().toList();
        Map<Long, Long> skuToStation = skuRepository.findAllById(skuIds).stream()
            .filter(sku -> sku.getStationId() != null)
            .collect(Collectors.toMap(SkuEntity::getId, SkuEntity::getStationId));

        // Step 2: group order items by stationId
        Map<Long, List<SubmittedOrderItemEntity>> groups = new LinkedHashMap<>();
        for (SubmittedOrderItemEntity item : order.getItems()) {
            Long stationId = skuToStation.get(item.getSkuId());
            if (stationId == null) {
                stationId = resolveDefaultStation(order.getStoreId());
            }
            groups.computeIfAbsent(stationId, k -> new ArrayList<>()).add(item);
        }

        // Step 3: determine round number
        int roundNumber = (int) ticketRepository
            .countDistinctSubmittedOrdersWithTicketsByTableId(order.getTableId()) + 1;

        // Step 4: create one ticket per station group
        List<KitchenTicketEntity> created = new ArrayList<>();
        int seq = 1;
        for (Map.Entry<Long, List<SubmittedOrderItemEntity>> entry : groups.entrySet()) {
            Long stationId = entry.getKey();
            KitchenStationEntity station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalStateException("Station not found: " + stationId));

            // Guard: station must belong to the same store as the order (prevents cross-tenant routing)
            if (!station.getStoreId().equals(order.getStoreId())) {
                throw new IllegalStateException(
                    "Station " + stationId + " belongs to store " + station.getStoreId()
                    + ", not order store " + order.getStoreId() + ". Possible misbound SKU.");
            }

            // Passive heartbeat check
            if (station.isHeartbeatExpired(HEARTBEAT_TIMEOUT_SECONDS)) {
                station.markOffline();
                stationRepository.save(station); // persist OFFLINE state
            }

            // submittedOrderId is a DB PK — globally unique per order; seq unique within an order
            String ticketNo = "KT-" + order.getStoreId() + "-" + order.getId() + "-" + seq++;

            KitchenTicketEntity ticket = new KitchenTicketEntity(
                ticketNo, order.getStoreId(), order.getTableId(),
                table.getTableCode(), stationId, order.getId(), roundNumber);

            for (SubmittedOrderItemEntity item : entry.getValue()) {
                KitchenTicketItemEntity ticketItem = new KitchenTicketItemEntity(
                    ticket, item.getSkuId(), item.getSkuNameSnapshot(),
                    item.getQuantity(), item.getItemRemark());
                ticketItem.setOptionSnapshotJson(item.getOptionSnapshotJson());
                ticket.addItem(ticketItem);
            }

            created.add(ticketRepository.save(ticket));
        }
        return created;
    }

    private Long resolveDefaultStation(Long storeId) {
        return stationRepository
            .findFirstByStoreIdAndStationStatusOrderBySortOrderAscIdAsc(storeId, "ACTIVE")
            .map(KitchenStationEntity::getId)
            .orElseThrow(() -> new IllegalStateException(
                "No active kitchen station for store: " + storeId));
    }
}
