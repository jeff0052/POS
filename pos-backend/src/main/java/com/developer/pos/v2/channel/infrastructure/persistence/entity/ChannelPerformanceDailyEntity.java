package com.developer.pos.v2.channel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity(name = "V2ChannelPerformanceDailyEntity")
@Table(name = "channel_performance_daily")
public class ChannelPerformanceDailyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "impressions", nullable = false)
    private int impressions;

    @Column(name = "clicks", nullable = false)
    private int clicks;

    @Column(name = "unique_visitors", nullable = false)
    private int uniqueVisitors;

    @Column(name = "orders", nullable = false)
    private int orders;

    @Column(name = "new_customers", nullable = false)
    private int newCustomers;

    @Column(name = "returning_customers", nullable = false)
    private int returningCustomers;

    @Column(name = "gross_sales_cents", nullable = false)
    private long grossSalesCents;

    @Column(name = "net_sales_cents", nullable = false)
    private long netSalesCents;

    @Column(name = "commission_cents", nullable = false)
    private long commissionCents;

    @Column(name = "profit_after_commission_cents", nullable = false)
    private long profitAfterCommissionCents;

    @Column(name = "cost_per_order_cents", nullable = false)
    private long costPerOrderCents;

    @Column(name = "customer_acquisition_cost_cents", nullable = false)
    private long customerAcquisitionCostCents;

    @Column(name = "roi_percent", precision = 10, scale = 4)
    private BigDecimal roiPercent;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    public ChannelPerformanceDailyEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public int getImpressions() { return impressions; }
    public void setImpressions(int impressions) { this.impressions = impressions; }
    public int getClicks() { return clicks; }
    public void setClicks(int clicks) { this.clicks = clicks; }
    public int getUniqueVisitors() { return uniqueVisitors; }
    public void setUniqueVisitors(int uniqueVisitors) { this.uniqueVisitors = uniqueVisitors; }
    public int getOrders() { return orders; }
    public void setOrders(int orders) { this.orders = orders; }
    public int getNewCustomers() { return newCustomers; }
    public void setNewCustomers(int newCustomers) { this.newCustomers = newCustomers; }
    public int getReturningCustomers() { return returningCustomers; }
    public void setReturningCustomers(int returningCustomers) { this.returningCustomers = returningCustomers; }
    public long getGrossSalesCents() { return grossSalesCents; }
    public void setGrossSalesCents(long grossSalesCents) { this.grossSalesCents = grossSalesCents; }
    public long getNetSalesCents() { return netSalesCents; }
    public void setNetSalesCents(long netSalesCents) { this.netSalesCents = netSalesCents; }
    public long getCommissionCents() { return commissionCents; }
    public void setCommissionCents(long commissionCents) { this.commissionCents = commissionCents; }
    public long getProfitAfterCommissionCents() { return profitAfterCommissionCents; }
    public void setProfitAfterCommissionCents(long profitAfterCommissionCents) { this.profitAfterCommissionCents = profitAfterCommissionCents; }
    public long getCostPerOrderCents() { return costPerOrderCents; }
    public void setCostPerOrderCents(long costPerOrderCents) { this.costPerOrderCents = costPerOrderCents; }
    public long getCustomerAcquisitionCostCents() { return customerAcquisitionCostCents; }
    public void setCustomerAcquisitionCostCents(long customerAcquisitionCostCents) { this.customerAcquisitionCostCents = customerAcquisitionCostCents; }
    public BigDecimal getRoiPercent() { return roiPercent; }
    public void setRoiPercent(BigDecimal roiPercent) { this.roiPercent = roiPercent; }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(OffsetDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
