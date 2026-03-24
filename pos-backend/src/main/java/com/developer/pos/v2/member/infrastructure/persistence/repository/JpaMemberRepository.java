package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JpaMemberRepository extends JpaRepository<MemberEntity, Long> {

    @Query("""
            select m from V2MemberEntity m
            where m.memberStatus = 'ACTIVE'
              and (
                    lower(m.name) like lower(concat('%', :keyword, '%'))
                    or lower(m.phone) like lower(concat('%', :keyword, '%'))
                    or lower(m.memberNo) like lower(concat('%', :keyword, '%'))
                  )
            order by m.id asc
            """)
    List<MemberEntity> searchActiveMembers(String keyword);

    Optional<MemberEntity> findByPhone(String phone);
}
