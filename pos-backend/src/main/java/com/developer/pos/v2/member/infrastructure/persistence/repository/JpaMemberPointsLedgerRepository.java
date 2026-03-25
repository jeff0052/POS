package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberPointsLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaMemberPointsLedgerRepository extends JpaRepository<MemberPointsLedgerEntity, Long> {
    List<MemberPointsLedgerEntity> findAllByMerchantIdOrderByIdDesc(Long merchantId);
}
