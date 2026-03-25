package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberRechargeOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaMemberRechargeOrderRepository extends JpaRepository<MemberRechargeOrderEntity, Long> {
    List<MemberRechargeOrderEntity> findAllByMerchantIdOrderByIdDesc(Long merchantId);
}
