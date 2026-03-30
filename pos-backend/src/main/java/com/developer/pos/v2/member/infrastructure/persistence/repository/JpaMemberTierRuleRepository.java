package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberTierRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaMemberTierRuleRepository extends JpaRepository<MemberTierRuleEntity, Long> {
    List<MemberTierRuleEntity> findByMerchantIdOrderByTierLevelAsc(Long merchantId);
}
