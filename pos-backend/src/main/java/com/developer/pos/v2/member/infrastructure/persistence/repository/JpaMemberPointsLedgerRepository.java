package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberPointsLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface JpaMemberPointsLedgerRepository extends JpaRepository<MemberPointsLedgerEntity, Long> {
    List<MemberPointsLedgerEntity> findAllByMerchantIdOrderByIdDesc(Long merchantId);

    @Query("SELECT COALESCE(SUM(e.pointsDelta), 0) FROM V2MemberPointsLedgerEntity e " +
           "WHERE e.memberId = :memberId AND e.changeType = 'EARN' AND e.createdAt >= :startOfDay")
    long sumEarnedPointsToday(@Param("memberId") Long memberId, @Param("startOfDay") OffsetDateTime startOfDay);
}
