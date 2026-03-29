package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.CouponTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCouponTemplateRepository extends JpaRepository<CouponTemplateEntity, Long> {
}
