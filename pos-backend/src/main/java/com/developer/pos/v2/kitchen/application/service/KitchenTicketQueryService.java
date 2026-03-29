package com.developer.pos.v2.kitchen.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.kitchen.application.dto.KitchenTicketDto;
import com.developer.pos.v2.kitchen.application.dto.KitchenTicketItemDto;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenTicketEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KitchenTicketQueryService implements UseCase {

    private static final List<String> DEFAULT_ACTIVE_STATUSES = List.of("SUBMITTED", "PREPARING");

    private final JpaKitchenTicketRepository ticketRepository;
    private final StoreAccessEnforcer enforcer;

    public KitchenTicketQueryService(JpaKitchenTicketRepository ticketRepository,
                                      StoreAccessEnforcer enforcer) {
        this.ticketRepository = ticketRepository;
        this.enforcer = enforcer;
    }

    @Transactional(readOnly = true)
    public List<KitchenTicketDto> listTickets(Long storeId, Long stationId, String status) {
        enforcer.enforce(storeId);
        enforcer.enforcePermission("KDS_OPERATE");

        List<String> statuses = status != null ? List.of(status) : DEFAULT_ACTIVE_STATUSES;

        List<KitchenTicketEntity> tickets = stationId != null
            ? ticketRepository.findByStoreIdAndStationIdAndTicketStatusIn(storeId, stationId, statuses)
            : ticketRepository.findByStoreIdAndTicketStatusIn(storeId, statuses);

        return tickets.stream().map(this::toDto).toList();
    }

    KitchenTicketDto toDto(KitchenTicketEntity e) {
        List<KitchenTicketItemDto> items = e.getItems().stream()
            .map(i -> new KitchenTicketItemDto(i.getSkuId(), i.getSkuNameSnapshot(),
                i.getQuantity(), i.getItemRemark(), i.getOptionSnapshotJson()))
            .toList();
        return new KitchenTicketDto(e.getId(), e.getTicketNo(), e.getStoreId(), e.getTableId(),
            e.getTableCode(), e.getStationId(), e.getRoundNumber(), e.getTicketStatus(), items,
            e.getSubmittedAt(), e.getStartedAt(), e.getReadyAt(), e.getServedAt());
    }
}
