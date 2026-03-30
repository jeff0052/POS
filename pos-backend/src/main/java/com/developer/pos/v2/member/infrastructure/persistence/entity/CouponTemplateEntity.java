package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity(name = "V2CouponTemplateEntity")
@Table(name = "coupon_templates")
public class CouponTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "coupon_type", nullable = false)
    private String couponType;

    @Column(name = "discount_amount_cents")
    private Long discountAmountCents;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @Column(name = "min_spend_cents")
    private Long minSpendCents;

    @Column(name = "max_discount_cents")
    private Long maxDiscountCents;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount;

    @Column(name = "per_member_limit", nullable = false)
    private int perMemberLimit;

    @Column(name = "validity_type", nullable = false)
    private String validityType;

    @Column(name = "valid_from")
    private OffsetDateTime validFrom;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "template_status", nullable = false)
    private String templateStatus;

    // --- getters & setters ---

    public Long getId() { return id; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getCouponType() { return couponType; }
    public void setCouponType(String couponType) { this.couponType = couponType; }

    public Long getDiscountAmountCents() { return discountAmountCents; }
    public void setDiscountAmountCents(Long discountAmountCents) { this.discountAmountCents = discountAmountCents; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public Long getMinSpendCents() { return minSpendCents; }
    public void setMinSpendCents(Long minSpendCents) { this.minSpendCents = minSpendCents; }

    public Long getMaxDiscountCents() { return maxDiscountCents; }
    public void setMaxDiscountCents(Long maxDiscountCents) { this.maxDiscountCents = maxDiscountCents; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public int getIssuedCount() { return issuedCount; }
    public void setIssuedCount(int issuedCount) { this.issuedCount = issuedCount; }

    public int getPerMemberLimit() { return perMemberLimit; }
    public void setPerMemberLimit(int perMemberLimit) { this.perMemberLimit = perMemberLimit; }

    public String getValidityType() { return validityType; }
    public void setValidityType(String validityType) { this.validityType = validityType; }

    public OffsetDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(OffsetDateTime validFrom) { this.validFrom = validFrom; }

    public OffsetDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(OffsetDateTime validUntil) { this.validUntil = validUntil; }

    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }

    public String getTemplateStatus() { return templateStatus; }
    public void setTemplateStatus(String templateStatus) { this.templateStatus = templateStatus; }
}
