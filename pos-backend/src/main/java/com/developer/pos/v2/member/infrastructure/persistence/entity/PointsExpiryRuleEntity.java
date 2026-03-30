package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2PointsExpiryRuleEntity")
@Table(name = "points_expiry_rules")
public class PointsExpiryRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "expiry_type", nullable = false)
    private String expiryType; // ROLLING, YEAR_END, NEVER

    @Column(name = "expiry_months")
    private int expiryMonths;

    @Column(name = "year_end_clear_month")
    private int yearEndClearMonth;

    @Column(name = "year_end_clear_day")
    private int yearEndClearDay;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public PointsExpiryRuleEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getExpiryType() {
        return expiryType;
    }

    public void setExpiryType(String expiryType) {
        this.expiryType = expiryType;
    }

    public int getExpiryMonths() {
        return expiryMonths;
    }

    public void setExpiryMonths(int expiryMonths) {
        this.expiryMonths = expiryMonths;
    }

    public int getYearEndClearMonth() {
        return yearEndClearMonth;
    }

    public void setYearEndClearMonth(int yearEndClearMonth) {
        this.yearEndClearMonth = yearEndClearMonth;
    }

    public int getYearEndClearDay() {
        return yearEndClearDay;
    }

    public void setYearEndClearDay(int yearEndClearDay) {
        this.yearEndClearDay = yearEndClearDay;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
