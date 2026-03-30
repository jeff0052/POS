package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberCashLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaMemberCashLedgerRepository extends JpaRepository<MemberCashLedgerEntity, Long> {
    List<MemberCashLedgerEntity> findByMemberIdOrderByIdDesc(Long memberId);
    long countByMemberIdAndSourceTypeAndSourceRef(Long memberId, String sourceType, String sourceRef);
}
