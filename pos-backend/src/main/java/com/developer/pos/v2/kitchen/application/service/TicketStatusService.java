package com.developer.pos.v2.kitchen.application.service;

import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.kitchen.application.dto.KitchenTicketDto;
import com.developer.pos.v2.kitchen.application.dto.KitchenTicketItemDto;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenTicketEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketStatusService implements UseCase {

    private final JpaKitchenTicketRepository ticketRepository;
    private final StoreAccessEnforcer enforcer;

    public TicketStatusService(JpaKitchenTicketRepository ticketRepository,
                                StoreAccessEnforcer enforcer) {
        this.ticketRepository = ticketRepository;
        this.enforcer = enforcer;
    }

    @Transactional
    public KitchenTicketDto updateStatus(Long ticketId, String newStatus) {
        KitchenTicketEntity ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        // storeId derived from ticket row, not from request
        enforcer.enforce(ticket.getStoreId());

        // CANCELLED requires elevated permission
        if ("CANCELLED".equals(newStatus)) {
            enforcer.enforcePermission("TICKET_CANCEL");
        } else {
            enforcer.enforcePermission("KDS_OPERATE");
        }

        ticket.transitionTo(newStatus); // throws IllegalStateException on invalid transition

        // Populate timestamps
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case "PREPARING" -> ticket.setStartedAt(now);
            case "READY"     -> ticket.setReadyAt(now);
            case "SERVED"    -> ticket.setServedAt(now);
            case "CANCELLED" -> {
                // TODO Session 4.2: add submitted_order_item_id FK to kitchen_ticket_items
                // and item_status to submitted_order_items, then implement precise item cancellation.
            }
        }

        ticketRepository.save(ticket);
        return toDto(ticket);
    }

    private KitchenTicketDto toDto(KitchenTicketEntity e) {
        List<KitchenTicketItemDto> items = e.getItems().stream()
            .map(i -> new KitchenTicketItemDto(i.getSkuId(), i.getSkuNameSnapshot(),
                i.getQuantity(), i.getItemRemark(), i.getOptionSnapshotJson()))
            .toList();
        return new KitchenTicketDto(e.getId(), e.getTicketNo(), e.getStoreId(), e.getTableId(),
            e.getTableCode(), e.getStationId(), e.getRoundNumber(), e.getTicketStatus(), items,
            e.getSubmittedAt(), e.getStartedAt(), e.getReadyAt(), e.getServedAt());
    }
}
