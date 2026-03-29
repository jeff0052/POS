package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity(name = "V2CouponTemplateEntity")
@Table(name = "coupon_templates")
public class CouponTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Long getId() { return id; }
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
}
