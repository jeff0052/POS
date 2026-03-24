package com.developer.pos.v2.promotion.infrastructure.persistence.repository;

public interface PromotionHitProjection {
    String getRuleCode();

    long getDiscountAmountCents();

    String getGiftSnapshotJson();
}
