package com.developer.pos.v2.promotion.infrastructure.persistence.repository;

import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionHitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPromotionHitRepository extends JpaRepository<PromotionHitEntity, Long> {
    void deleteByActiveOrderDbId(Long activeOrderDbId);

    List<PromotionHitProjection> findByActiveOrderDbId(Long activeOrderDbId);
}
