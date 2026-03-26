package com.developer.pos.v2.staff.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cashier_shifts")
public class CashierShiftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shift_id", nullable = false)
    private String shiftId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "cashier_name", nullable = false)
    private String cashierName;

    @Column(name = "shift_status", nullable = false)
    private String shiftStatus;

    @Column(name = "opening_float_cents", nullable = false)
    private long openingFloatCents;

    @Column(name = "closing_cash_cents")
    private Long closingCashCents;

    @Column(name = "closing_note")
    private String closingNote;

    @Column(name = "opened_at", insertable = false, updatable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    public Long getId() {
        return id;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public void setCashierId(Long cashierId) {
        this.cashierId = cashierId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getShiftStatus() {
        return shiftStatus;
    }

    public void setShiftStatus(String shiftStatus) {
        this.shiftStatus = shiftStatus;
    }

    public long getOpeningFloatCents() {
        return openingFloatCents;
    }

    public void setOpeningFloatCents(long openingFloatCents) {
        this.openingFloatCents = openingFloatCents;
    }

    public Long getClosingCashCents() {
        return closingCashCents;
    }

    public void setClosingCashCents(Long closingCashCents) {
        this.closingCashCents = closingCashCents;
    }

    public String getClosingNote() {
        return closingNote;
    }

    public void setClosingNote(String closingNote) {
        this.closingNote = closingNote;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
