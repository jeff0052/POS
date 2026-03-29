package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaMemberAccountRepository extends JpaRepository<MemberAccountEntity, Long> {
    Optional<MemberAccountEntity> findByMemberId(Long memberId);

    @Modifying
    @Query(value = """
        UPDATE member_accounts
        SET frozen_points = frozen_points + :points
        WHERE member_id = :memberId
          AND (points_balance - frozen_points) >= :points
        """, nativeQuery = true)
    int freezePoints(@Param("memberId") Long memberId, @Param("points") long points);

    @Modifying
    @Query(value = """
        UPDATE member_accounts
        SET frozen_points = frozen_points - :points
        WHERE member_id = :memberId AND frozen_points >= :points
        """, nativeQuery = true)
    int unfreezePoints(@Param("memberId") Long memberId, @Param("points") long points);

    @Modifying
    @Query(value = """
        UPDATE member_accounts
        SET frozen_cash_cents = frozen_cash_cents + :cents
        WHERE member_id = :memberId
          AND (cash_balance_cents - frozen_cash_cents) >= :cents
        """, nativeQuery = true)
    int freezeCash(@Param("memberId") Long memberId, @Param("cents") long cents);

    @Modifying
    @Query(value = """
        UPDATE member_accounts
        SET frozen_cash_cents = frozen_cash_cents - :cents
        WHERE member_id = :memberId AND frozen_cash_cents >= :cents
        """, nativeQuery = true)
    int unfreezeCash(@Param("memberId") Long memberId, @Param("cents") long cents);

    @Modifying
    @Query(value = """
        UPDATE member_accounts
        SET points_balance = points_balance - :points,
            frozen_points = frozen_points - :points
        WHERE member_id = :memberId
          AND points_balance >= :points AND frozen_points >= :points
        """, nativeQuery = true)
    int deductPoints(@Param("memberId") Long memberId, @Param("points") long points);

    @Modifying
    @Query(value = """
        UPDATE member_accounts
        SET cash_balance_cents = cash_balance_cents - :cents,
            frozen_cash_cents = frozen_cash_cents - :cents
        WHERE member_id = :memberId
          AND cash_balance_cents >= :cents AND frozen_cash_cents >= :cents
        """, nativeQuery = true)
    int deductCash(@Param("memberId") Long memberId, @Param("cents") long cents);
}
