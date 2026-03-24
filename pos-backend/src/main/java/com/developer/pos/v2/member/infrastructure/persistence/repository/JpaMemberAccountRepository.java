package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaMemberAccountRepository extends JpaRepository<MemberAccountEntity, Long> {
    Optional<MemberAccountEntity> findByMemberId(Long memberId);
}
