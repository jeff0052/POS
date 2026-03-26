package com.developer.pos.v2.shift.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cashier_shifts")
public class CashierShiftEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String shiftId;
    private Long merchantId;
    private Long storeId;
    private String cashierStaffId;
    private String cashierName;
    private String shiftStatus;
    private OffsetDateTime openedAt;
    private OffsetDateTime closedAt;
    private long openingCashCents;
    private Long closingCashCents;
    private Long expectedCashCents;
    private Long cashDifferenceCents;
    private long totalSalesCents;
    private long totalRefundsCents;
    private int totalTransactionCount;
    private String notes;

    public Long getId() { return id; }
    public String getShiftId() { return shiftId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getCashierStaffId() { return cashierStaffId; }
    public void setCashierStaffId(String cashierStaffId) { this.cashierStaffId = cashierStaffId; }
    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }
    public String getShiftStatus() { return shiftStatus; }
    public void setShiftStatus(String shiftStatus) { this.shiftStatus = shiftStatus; }
    public OffsetDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(OffsetDateTime openedAt) { this.openedAt = openedAt; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime closedAt) { this.closedAt = closedAt; }
    public long getOpeningCashCents() { return openingCashCents; }
    public void setOpeningCashCents(long openingCashCents) { this.openingCashCents = openingCashCents; }
    public Long getClosingCashCents() { return closingCashCents; }
    public void setClosingCashCents(Long closingCashCents) { this.closingCashCents = closingCashCents; }
    public Long getExpectedCashCents() { return expectedCashCents; }
    public void setExpectedCashCents(Long expectedCashCents) { this.expectedCashCents = expectedCashCents; }
    public Long getCashDifferenceCents() { return cashDifferenceCents; }
    public void setCashDifferenceCents(Long cashDifferenceCents) { this.cashDifferenceCents = cashDifferenceCents; }
    public long getTotalSalesCents() { return totalSalesCents; }
    public void setTotalSalesCents(long totalSalesCents) { this.totalSalesCents = totalSalesCents; }
    public long getTotalRefundsCents() { return totalRefundsCents; }
    public void setTotalRefundsCents(long totalRefundsCents) { this.totalRefundsCents = totalRefundsCents; }
    public int getTotalTransactionCount() { return totalTransactionCount; }
    public void setTotalTransactionCount(int totalTransactionCount) { this.totalTransactionCount = totalTransactionCount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
