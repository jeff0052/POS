package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberCouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaMemberCouponRepository extends JpaRepository<MemberCouponEntity, Long> {

    // CAS lock: coupon_status='AVAILABLE' AND lock_version=:expectedVersion
    @Modifying
    @Query(value = """
        UPDATE member_coupons
        SET coupon_status = 'LOCKED', lock_version = lock_version + 1,
            locked_by_session = :sessionId, locked_at = NOW()
        WHERE id = :couponId AND coupon_status = 'AVAILABLE' AND lock_version = :expectedVersion
        """, nativeQuery = true)
    int lockCouponCas(@Param("couponId") Long couponId,
                      @Param("expectedVersion") int expectedVersion,
                      @Param("sessionId") Long sessionId);

    // CAS release: only if still locked by this session
    @Modifying
    @Query(value = """
        UPDATE member_coupons
        SET coupon_status = 'AVAILABLE', locked_by_session = NULL, locked_at = NULL,
            lock_version = lock_version + 1
        WHERE id = :couponId AND coupon_status = 'LOCKED' AND locked_by_session = :sessionId
        """, nativeQuery = true)
    int releaseCouponCas(@Param("couponId") Long couponId, @Param("sessionId") Long sessionId);

    // CAS confirm: LOCKED + same session -> USED
    @Modifying
    @Query(value = """
        UPDATE member_coupons
        SET coupon_status = 'USED', used_at = NOW(),
            used_order_id = :usedOrderId, used_store_id = :usedStoreId
        WHERE id = :couponId AND coupon_status = 'LOCKED' AND locked_by_session = :sessionId
        """, nativeQuery = true)
    int confirmCouponCas(@Param("couponId") Long couponId,
                         @Param("sessionId") Long sessionId,
                         @Param("usedOrderId") String usedOrderId,
                         @Param("usedStoreId") Long usedStoreId);

    Optional<MemberCouponEntity> findByIdAndCouponStatusAndLockedBySession(Long id, String couponStatus, Long lockedBySession);

    List<MemberCouponEntity> findAllByCouponStatusAndLockedAtBefore(String couponStatus, OffsetDateTime before);

    List<MemberCouponEntity> findAllByMemberIdAndCouponStatus(Long memberId, String couponStatus);

    List<MemberCouponEntity> findAllByMemberId(Long memberId);

    long countByMemberIdAndTemplateId(Long memberId, Long templateId);

    List<MemberCouponEntity> findAllByCouponStatusAndValidUntilBefore(String couponStatus, OffsetDateTime before);
}
