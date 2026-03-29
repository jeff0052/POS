package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity(name = "V2MenuTimeSlotEntity")
@Table(name = "menu_time_slots")
public class MenuTimeSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "slot_code", nullable = false, length = 64)
    private String slotCode;

    @Column(name = "slot_name", nullable = false, length = 128)
    private String slotName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "applicable_days", columnDefinition = "JSON")
    private String applicableDays;

    @Column(name = "dining_modes", columnDefinition = "JSON")
    private String diningModes;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "priority")
    private int priority;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected MenuTimeSlotEntity() {
    }

    public MenuTimeSlotEntity(Long storeId, String slotCode, String slotName,
                              LocalTime startTime, LocalTime endTime,
                              String applicableDays, String diningModes,
                              boolean active, int priority) {
        this.storeId = storeId;
        this.slotCode = slotCode;
        this.slotName = slotName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.applicableDays = applicableDays;
        this.diningModes = diningModes;
        this.active = active;
        this.priority = priority;
    }

    public void update(String slotCode, String slotName, LocalTime startTime, LocalTime endTime,
                       String applicableDays, String diningModes, boolean active, int priority) {
        this.slotCode = slotCode;
        this.slotName = slotName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.applicableDays = applicableDays;
        this.diningModes = diningModes;
        this.active = active;
        this.priority = priority;
    }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public String getSlotCode() { return slotCode; }
    public String getSlotName() { return slotName; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getApplicableDays() { return applicableDays; }
    public String getDiningModes() { return diningModes; }
    public boolean isActive() { return active; }
    public int getPriority() { return priority; }
}
