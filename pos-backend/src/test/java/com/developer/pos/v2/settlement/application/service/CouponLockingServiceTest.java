package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberCouponEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponLockingServiceTest {

    @Mock JpaMemberCouponRepository couponRepo;
    @InjectMocks CouponLockingService service;

    @Test
    void lockCoupon_success() {
        when(couponRepo.lockCouponCas(1L, 0, 100L)).thenReturn(1);
        assertThatNoException().isThrownBy(() -> service.lockCoupon(1L, 0, 100L));
    }

    @Test
    void lockCoupon_alreadyLocked_throws() {
        when(couponRepo.lockCouponCas(1L, 0, 100L)).thenReturn(0);
        assertThatThrownBy(() -> service.lockCoupon(1L, 0, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already locked");
    }

    @Test
    void releaseCoupon_idempotent_whenAlreadyAvailable() {
        when(couponRepo.releaseCouponCas(1L, 100L)).thenReturn(0);
        // 已 AVAILABLE → 无操作，不抛异常
        assertThatNoException().isThrownBy(() -> service.releaseCoupon(1L, 100L));
    }

    @Test
    void confirmCoupon_success() {
        when(couponRepo.confirmCouponCas(1L, 100L, "order-1", 10L)).thenReturn(1);
        assertThatNoException().isThrownBy(() -> service.confirmCoupon(1L, 100L, "order-1", 10L));
    }

    @Test
    void confirmCoupon_alreadyUsedSameOrder_idempotent() {
        when(couponRepo.confirmCouponCas(1L, 100L, "order-1", 10L)).thenReturn(0);
        MemberCouponEntity used = new MemberCouponEntity();
        used.setCouponStatus("USED");
        used.setUsedOrderId("order-1");
        when(couponRepo.findById(1L)).thenReturn(Optional.of(used));
        assertThatNoException().isThrownBy(() -> service.confirmCoupon(1L, 100L, "order-1", 10L));
    }

    @Test
    void confirmCoupon_alreadyUsedDifferentOrder_throws() {
        when(couponRepo.confirmCouponCas(1L, 100L, "order-2", 10L)).thenReturn(0);
        MemberCouponEntity used = new MemberCouponEntity();
        used.setCouponStatus("USED");
        used.setUsedOrderId("order-1");
        when(couponRepo.findById(1L)).thenReturn(Optional.of(used));
        assertThatThrownBy(() -> service.confirmCoupon(1L, 100L, "order-2", 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conflict");
    }
}
