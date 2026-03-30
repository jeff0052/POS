package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaMemberRepository extends JpaRepository<MemberEntity, Long> {

    @Query("SELECT m FROM V2MemberEntity m WHERE m.merchantId = :merchantId AND m.memberStatus = 'ACTIVE' AND (:keyword = '' OR m.name LIKE %:keyword% OR m.phone LIKE %:keyword%)")
    List<MemberEntity> searchActiveMembers(@Param("merchantId") Long merchantId, @Param("keyword") String keyword);

    Optional<MemberEntity> findByPhone(String phone);
    Optional<MemberEntity> findByMerchantIdAndPhone(Long merchantId, String phone);
}
