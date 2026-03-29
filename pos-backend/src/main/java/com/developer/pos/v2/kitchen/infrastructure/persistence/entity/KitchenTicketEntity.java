package com.developer.pos.v2.kitchen.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "V2KitchenTicketEntity")
@Table(name = "kitchen_tickets")
public class KitchenTicketEntity {

    private static final List<String> TERMINAL_STATUSES = List.of("SERVED", "CANCELLED");
    private static final java.util.Map<String, List<String>> VALID_TRANSITIONS = java.util.Map.of(
        "SUBMITTED",  List.of("PREPARING", "CANCELLED"),
        "PREPARING",  List.of("READY", "CANCELLED"),
        "READY",      List.of("SERVED")
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_no", nullable = false, unique = true, length = 64)
    private String ticketNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "table_code", nullable = false, length = 64)
    private String tableCode;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "submitted_order_id", nullable = false)
    private Long submittedOrderId;

    @Column(name = "round_number")
    private int roundNumber = 1;

    @Column(name = "ticket_status", nullable = false, length = 32)
    private String ticketStatus = "SUBMITTED";

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "served_at")
    private LocalDateTime servedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KitchenTicketItemEntity> items = new ArrayList<>();

    protected KitchenTicketEntity() {}

    public KitchenTicketEntity(String ticketNo, Long storeId, Long tableId, String tableCode,
                                Long stationId, Long submittedOrderId, int roundNumber) {
        this.ticketNo = ticketNo;
        this.storeId = storeId;
        this.tableId = tableId;
        this.tableCode = tableCode;
        this.stationId = stationId;
        this.submittedOrderId = submittedOrderId;
        this.roundNumber = roundNumber;
    }

    /**
     * Transitions ticket to newStatus. Throws IllegalStateException for invalid transitions.
     * Terminal states (SERVED, CANCELLED) cannot be transitioned from.
     */
    public void transitionTo(String newStatus) {
        if (TERMINAL_STATUSES.contains(ticketStatus)) {
            throw new IllegalStateException(
                "Ticket " + ticketNo + " is in terminal state " + ticketStatus + " and cannot be transitioned");
        }
        List<String> allowed = VALID_TRANSITIONS.getOrDefault(ticketStatus, List.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                "Cannot transition ticket from " + ticketStatus + " to " + newStatus);
        }
        this.ticketStatus = newStatus;
    }

    public void addItem(KitchenTicketItemEntity item) {
        items.add(item);
    }

    // Getters
    public Long getId() { return id; }
    public String getTicketNo() { return ticketNo; }
    public Long getStoreId() { return storeId; }
    public Long getTableId() { return tableId; }
    public String getTableCode() { return tableCode; }
    public Long getStationId() { return stationId; }
    public Long getSubmittedOrderId() { return submittedOrderId; }
    public int getRoundNumber() { return roundNumber; }
    public String getTicketStatus() { return ticketStatus; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getReadyAt() { return readyAt; }
    public void setReadyAt(LocalDateTime readyAt) { this.readyAt = readyAt; }
    public LocalDateTime getServedAt() { return servedAt; }
    public void setServedAt(LocalDateTime servedAt) { this.servedAt = servedAt; }
    public List<KitchenTicketItemEntity> getItems() { return items; }
}
