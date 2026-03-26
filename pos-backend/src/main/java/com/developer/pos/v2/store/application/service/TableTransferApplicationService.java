package com.developer.pos.v2.store.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.application.dto.TableTransferResultDto;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TableTransferApplicationService implements UseCase {

    private final JpaStoreTableRepository storeTableRepository;
    private final JpaTableSessionRepository tableSessionRepository;
    private final JpaSubmittedOrderRepository submittedOrderRepository;
    private final JpaActiveTableOrderRepository activeTableOrderRepository;

    public TableTransferApplicationService(
            JpaStoreTableRepository storeTableRepository,
            JpaTableSessionRepository tableSessionRepository,
            JpaSubmittedOrderRepository submittedOrderRepository,
            JpaActiveTableOrderRepository activeTableOrderRepository
    ) {
        this.storeTableRepository = storeTableRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.submittedOrderRepository = submittedOrderRepository;
        this.activeTableOrderRepository = activeTableOrderRepository;
    }

    @Transactional
    public TableTransferResultDto transfer(Long storeId, Long sourceTableId, Long destinationTableId) {
        if (sourceTableId.equals(destinationTableId)) {
            throw new IllegalArgumentException("Source and destination tables cannot be the same.");
        }

        StoreTableEntity sourceTable = storeTableRepository.findByIdAndStoreId(sourceTableId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Source table not found."));
        StoreTableEntity destinationTable = storeTableRepository.findByIdAndStoreId(destinationTableId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Destination table not found."));

        if (!"AVAILABLE".equalsIgnoreCase(destinationTable.getTableStatus())) {
            throw new IllegalStateException("Destination table must be available.");
        }

        TableSessionEntity session = tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, sourceTableId, "OPEN")
                .orElseThrow(() -> new IllegalStateException("Open table session not found for transfer."));
        session.setTableId(destinationTableId);
        tableSessionRepository.save(session);

        activeTableOrderRepository.findByStoreIdAndTableId(storeId, sourceTableId).ifPresent(activeOrder -> {
            activeOrder.setTableId(destinationTableId);
            activeTableOrderRepository.save(activeOrder);
        });

        List<SubmittedOrderEntity> submittedOrders = submittedOrderRepository.findAllByTableSessionIdOrderByIdAsc(session.getId());
        submittedOrders.forEach(order -> order.setTableId(destinationTableId));
        submittedOrderRepository.saveAll(submittedOrders);

        destinationTable.setTableStatus(sourceTable.getTableStatus());
        sourceTable.setTableStatus("AVAILABLE");
        storeTableRepository.save(destinationTable);
        storeTableRepository.save(sourceTable);

        return new TableTransferResultDto(sourceTableId, destinationTableId, session.getSessionId(), destinationTable.getTableStatus());
    }
}
