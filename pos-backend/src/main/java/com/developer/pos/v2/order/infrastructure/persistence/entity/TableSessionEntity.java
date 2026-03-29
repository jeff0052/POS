package com.developer.pos.v2.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "table_sessions")
public class TableSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "session_status", nullable = false)
    private String sessionStatus;

    @Column(name = "merged_into_session_id")
    private Long mergedIntoSessionId;

    @Column(name = "opened_at", insertable = false, updatable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "dining_mode")
    private String diningMode = "A_LA_CARTE";

    @Column(name = "guest_count")
    private int guestCount = 1;

    @Column(name = "child_count")
    private int childCount;

    @Column(name = "buffet_package_id")
    private Long buffetPackageId;

    @Column(name = "buffet_started_at")
    private OffsetDateTime buffetStartedAt;

    @Column(name = "buffet_ends_at")
    private OffsetDateTime buffetEndsAt;

    @Column(name = "buffet_status", length = 32)
    private String buffetStatus;

    @Column(name = "buffet_overtime_minutes")
    private int buffetOvertimeMinutes;

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public Long getMergedIntoSessionId() {
        return mergedIntoSessionId;
    }

    public void setMergedIntoSessionId(Long mergedIntoSessionId) {
        this.mergedIntoSessionId = mergedIntoSessionId;
    }

    public String getDiningMode() {
        return diningMode;
    }

    public void setDiningMode(String diningMode) {
        this.diningMode = diningMode;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public Long getBuffetPackageId() {
        return buffetPackageId;
    }

    public void setBuffetPackageId(Long buffetPackageId) {
        this.buffetPackageId = buffetPackageId;
    }

    public OffsetDateTime getBuffetStartedAt() {
        return buffetStartedAt;
    }

    public void setBuffetStartedAt(OffsetDateTime buffetStartedAt) {
        this.buffetStartedAt = buffetStartedAt;
    }

    public OffsetDateTime getBuffetEndsAt() {
        return buffetEndsAt;
    }

    public void setBuffetEndsAt(OffsetDateTime buffetEndsAt) {
        this.buffetEndsAt = buffetEndsAt;
    }

    public String getBuffetStatus() {
        return buffetStatus;
    }

    public void setBuffetStatus(String buffetStatus) {
        this.buffetStatus = buffetStatus;
    }

    public int getBuffetOvertimeMinutes() {
        return buffetOvertimeMinutes;
    }

    public void setBuffetOvertimeMinutes(int buffetOvertimeMinutes) {
        this.buffetOvertimeMinutes = buffetOvertimeMinutes;
    }
}
