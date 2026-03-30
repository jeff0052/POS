package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.CouponTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCouponTemplateRepository extends JpaRepository<CouponTemplateEntity, Long> {

    List<CouponTemplateEntity> findAllByMerchantId(Long merchantId);
}
