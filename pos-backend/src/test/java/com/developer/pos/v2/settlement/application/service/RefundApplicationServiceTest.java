package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberCouponEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCouponRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderItemRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.settlement.application.command.ApproveRefundCommand;
import com.developer.pos.v2.settlement.application.command.CreateRefundCommand;
import com.developer.pos.v2.settlement.application.dto.RefundRecordDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementPaymentHoldEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundLineItemRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundRecordRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementPaymentHoldRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundApplicationServiceTest {

    @Mock private JpaRefundRecordRepository refundRecordRepository;
    @Mock private JpaSettlementRecordRepository settlementRecordRepository;
    @Mock private JpaRefundLineItemRepository refundLineItemRepository;
    @Mock private JpaSettlementPaymentHoldRepository paymentHoldRepository;
    @Mock private JpaMemberAccountRepository memberAccountRepository;
    @Mock private JpaMemberCouponRepository memberCouponRepository;
    @Mock private JpaSubmittedOrderItemRepository orderItemRepository;
    @Mock private JpaTableSessionRepository tableSessionRepository;
    @Mock private StoreAccessEnforcer storeAccessEnforcer;

    @InjectMocks private RefundApplicationService service;

    private SettlementRecordEntity settlement;

    @BeforeEach
    void setUp() {
        setActor(0L); // default: unlimited refund

        settlement = new SettlementRecordEntity();
        settlement.setFinalStatus("SETTLED");
        settlement.setCollectedAmountCents(10000);
        settlement.setPointsDeductCents(2000);
        settlement.setCashBalanceDeductCents(3000);
        settlement.setCouponDiscountCents(1000);
        settlement.setExternalPaymentCents(4000);
        settlement.setRefundedAmountCents(0);
        settlement.setSettlementNo("SET-001");
        settlement.setPaymentMethod("MIXED");
        settlement.setMerchantId(1L);
        settlement.setStoreId(1L);
        settlement.setCouponId(99L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setActor(long maxRefundCents) {
        AuthenticatedActor actor = new AuthenticatedActor(
                42L, "user", "AU-42", "CASHIER", 1L, 1L, Set.of(1L), Set.of("REFUND_SMALL"), maxRefundCents);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, List.of()));
    }

    @Test
    void fullRefund_withExternalPayment_statusIsAwaitingExternalRefund() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refundRecordRepository.findBySettlementId(any())).thenReturn(List.of());

        SettlementPaymentHoldEntity pointsHold = new SettlementPaymentHoldEntity();
        pointsHold.setHoldType("POINTS");
        pointsHold.setMemberId(77L);
        pointsHold.setPointsHeld(200L);
        pointsHold.setHoldAmountCents(2000);

        when(paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(any(), eq("CONFIRMED")))
                .thenReturn(List.of(pointsHold));

        MemberAccountEntity account = new MemberAccountEntity();
        account.setAvailablePoints(100);
        account.setPointsBalance(100);
        account.setAvailableCashCents(500);
        account.setCashBalanceCents(500);
        when(memberAccountRepository.findByMemberId(77L)).thenReturn(Optional.of(account));

        MemberCouponEntity coupon = new MemberCouponEntity();
        coupon.setCouponStatus("USED");
        when(memberCouponRepository.findById(99L)).thenReturn(Optional.of(coupon));

        CreateRefundCommand cmd = new CreateRefundCommand(1L, 0, "FULL", "customer request", null);
        RefundRecordDto result = service.createRefund(cmd);

        // External payment present → not COMPLETED
        assertThat(result.refundStatus()).isEqualTo("AWAITING_EXTERNAL_REFUND");
        assertThat(result.externalRefundStatus()).isEqualTo("PENDING");
        // Internal assets still reversed
        assertThat(account.getAvailablePoints()).isEqualTo(300); // 100 + 200
        assertThat(account.getAvailableCashCents()).isEqualTo(3500); // 500 + 3000
        assertThat(coupon.getCouponStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void fullRefund_noExternalPayment_statusIsCompleted() {
        settlement.setExternalPaymentCents(0);
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refundRecordRepository.findBySettlementId(any())).thenReturn(List.of());
        when(paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(any(), eq("CONFIRMED")))
                .thenReturn(List.of());

        CreateRefundCommand cmd = new CreateRefundCommand(1L, 0, "FULL", "reason", null);
        RefundRecordDto result = service.createRefund(cmd);

        assertThat(result.refundStatus()).isEqualTo("COMPLETED");
        assertThat(result.externalRefundStatus()).isNull();
    }

    @Test
    void secondPartialRefund_capsReversalAtRemaining() {
        // First refund already reversed 1000 points-cents and 1500 cash-cents
        RefundRecordEntity prior = new RefundRecordEntity();
        prior.setRefundStatus("COMPLETED");
        prior.setPointsReversedCents(1000);
        prior.setCashReversedCents(1500);
        prior.setCouponReversed(false);
        when(refundRecordRepository.findBySettlementId(any())).thenReturn(List.of(prior));

        settlement.setRefundedAmountCents(5000); // 5000 already refunded
        settlement.setExternalPaymentCents(0);
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(any(), eq("CONFIRMED")))
                .thenReturn(List.of());

        // Second partial refund of 5000 (50% of collected)
        CreateRefundCommand cmd = new CreateRefundCommand(1L, 5000, "PARTIAL", "second refund", null);
        RefundRecordDto result = service.createRefund(cmd);

        // Proportional would be 50% of 2000 = 1000, but only 1000 remains (2000 - 1000)
        assertThat(result.pointsReversedCents()).isEqualTo(1000);
        // Proportional would be 50% of 3000 = 1500, and 1500 remains (3000 - 1500)
        assertThat(result.cashReversedCents()).isEqualTo(1500);
    }

    @Test
    void rbacThreshold_overLimit_requiresApproval() {
        setActor(5000L); // maxRefundCents = 5000
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRefundCommand cmd = new CreateRefundCommand(1L, 6000, "PARTIAL", "reason", null);
        RefundRecordDto result = service.createRefund(cmd);

        assertThat(result.approvalStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(result.refundStatus()).isEqualTo("PENDING");
    }

    @Test
    void lineItems_sumValidation_rejectsIfMismatch() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));

        var items = List.of(
                new CreateRefundCommand.RefundItemCommand(101L, 1, 1000),
                new CreateRefundCommand.RefundItemCommand(102L, 1, 500) // sum=1500 != 3000
        );
        CreateRefundCommand cmd = new CreateRefundCommand(1L, 3000, "PARTIAL", "reason", items);

        assertThatThrownBy(() -> service.createRefund(cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void approveRefund_withExternalPayment_statusIsAwaitingExternalRefund() {
        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundNo("REF001");
        refund.setApprovalStatus("PENDING_APPROVAL");
        refund.setRefundAmountCents(6000);
        refund.setSettlementId(1L);
        refund.setStoreId(1L);
        refund.setPointsReversedCents(0);
        refund.setCashReversedCents(0);
        refund.setCouponReversed(false);

        when(refundRecordRepository.findByRefundNoForUpdate("REF001")).thenReturn(Optional.of(refund));
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refundLineItemRepository.findByRefundId(any())).thenReturn(List.of());
        when(paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(any(), eq("CONFIRMED")))
                .thenReturn(List.of());

        ApproveRefundCommand cmd = new ApproveRefundCommand("REF001", true);
        RefundRecordDto result = service.approveRefund(cmd);

        assertThat(result.approvalStatus()).isEqualTo("APPROVED");
        assertThat(result.refundStatus()).isEqualTo("AWAITING_EXTERNAL_REFUND");
        assertThat(result.approvedBy()).isEqualTo(42L);
    }

    @Test
    void approveRefund_rejected_doesNotUpdateSettlement() {
        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundNo("REF002");
        refund.setApprovalStatus("PENDING_APPROVAL");
        refund.setRefundAmountCents(6000);
        refund.setSettlementId(1L);
        refund.setStoreId(1L);

        when(refundRecordRepository.findByRefundNoForUpdate("REF002")).thenReturn(Optional.of(refund));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refundLineItemRepository.findByRefundId(any())).thenReturn(List.of());

        ApproveRefundCommand cmd = new ApproveRefundCommand("REF002", false);
        RefundRecordDto result = service.approveRefund(cmd);

        assertThat(result.approvalStatus()).isEqualTo("REJECTED");
        verify(settlementRecordRepository, never()).save(any());
    }

    @Test
    void refund_nonSettledOrder_throws() {
        settlement.setFinalStatus("PENDING");
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));

        CreateRefundCommand cmd = new CreateRefundCommand(1L, 5000, "PARTIAL", "reason", null);

        assertThatThrownBy(() -> service.createRefund(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only refund SETTLED orders");
    }

    @Test
    void refund_exceedsRefundable_throws() {
        settlement.setRefundedAmountCents(9000);
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));

        CreateRefundCommand cmd = new CreateRefundCommand(1L, 5000, "PARTIAL", "reason", null);

        assertThatThrownBy(() -> service.createRefund(cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds refundable");
    }
}
