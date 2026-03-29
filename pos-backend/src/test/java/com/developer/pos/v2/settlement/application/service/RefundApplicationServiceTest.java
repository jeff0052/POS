package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberCouponEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCouponRepository;
import com.developer.pos.v2.settlement.application.command.ApproveRefundCommand;
import com.developer.pos.v2.settlement.application.command.CreateRefundCommand;
import com.developer.pos.v2.settlement.application.dto.RefundRecordDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundLineItemEntity;
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
    @Mock private StoreAccessEnforcer storeAccessEnforcer;

    @InjectMocks private RefundApplicationService service;

    private SettlementRecordEntity settlement;

    @BeforeEach
    void setUp() {
        // CASHIER role → threshold 5000 cents
        AuthenticatedActor actor = new AuthenticatedActor(
                42L, "cashier1", "AU-42", "CASHIER", 1L, 1L, Set.of(1L), Set.of("REFUND_SMALL"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, List.of()));

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

    @Test
    void fullRefund_reversesPointsAndCashAndCoupon() {
        // MERCHANT_OWNER has no threshold → full refund auto-approved → reversal executes
        AuthenticatedActor owner = new AuthenticatedActor(
                42L, "owner", "AU-42", "MERCHANT_OWNER", 1L, 1L, Set.of(1L), Set.of("REFUND_LARGE"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner, null, List.of()));

        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Set up payment holds with member for asset reversal
        SettlementPaymentHoldEntity pointsHold = new SettlementPaymentHoldEntity();
        pointsHold.setHoldType("POINTS");
        pointsHold.setMemberId(77L);
        pointsHold.setPointsHeld(200L); // 200 points used
        pointsHold.setHoldAmountCents(2000);

        SettlementPaymentHoldEntity cashHold = new SettlementPaymentHoldEntity();
        cashHold.setHoldType("CASH_BALANCE");
        cashHold.setMemberId(77L);
        cashHold.setHoldAmountCents(3000);

        SettlementPaymentHoldEntity couponHold = new SettlementPaymentHoldEntity();
        couponHold.setHoldType("COUPON");
        couponHold.setMemberId(77L);
        couponHold.setTableSessionId(10L);

        when(paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(any(), eq("CONFIRMED")))
                .thenReturn(List.of(pointsHold, cashHold, couponHold));

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

        // Verify refund record
        assertThat(result.refundAmountCents()).isEqualTo(10000);
        assertThat(result.operatedBy()).isEqualTo(42L);

        // Verify actual asset reversal happened
        assertThat(account.getAvailablePoints()).isEqualTo(300); // 100 + 200
        assertThat(account.getPointsBalance()).isEqualTo(300);
        assertThat(account.getAvailableCashCents()).isEqualTo(3500); // 500 + 3000
        assertThat(account.getCashBalanceCents()).isEqualTo(3500);
        assertThat(coupon.getCouponStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void partialRefund_withItems_tracksLineItems() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(any(), eq("CONFIRMED")))
                .thenReturn(List.of());

        RefundLineItemEntity li1 = new RefundLineItemEntity();
        li1.setOrderItemId(101L);
        li1.setQuantity(1);
        li1.setRefundAmountCents(0);

        RefundLineItemEntity li2 = new RefundLineItemEntity();
        li2.setOrderItemId(102L);
        li2.setQuantity(2);
        li2.setRefundAmountCents(0);

        when(refundLineItemRepository.saveAll(any())).thenReturn(List.of(li1, li2));

        List<CreateRefundCommand.RefundItemCommand> items = List.of(
                new CreateRefundCommand.RefundItemCommand(101L, 1),
                new CreateRefundCommand.RefundItemCommand(102L, 2)
        );
        CreateRefundCommand cmd = new CreateRefundCommand(1L, 3000, "PARTIAL", "partial return", items);
        RefundRecordDto result = service.createRefund(cmd);

        verify(refundLineItemRepository).saveAll(argThat(list -> ((List<?>) list).size() == 2));
        assertThat(result.lineItems()).hasSize(2);
    }

    @Test
    void cashier_overThreshold_requiresApproval() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // CASHIER role has threshold 5000, amount 6000 → needs approval
        CreateRefundCommand cmd = new CreateRefundCommand(1L, 6000, "PARTIAL", "reason", null);
        RefundRecordDto result = service.createRefund(cmd);

        assertThat(result.approvalStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(result.refundStatus()).isEqualTo("PENDING");
        verify(settlementRecordRepository, never()).save(any());
        verify(paymentHoldRepository, never()).findAllBySettlementRecordIdAndHoldStatus(any(), any());
    }

    @Test
    void merchantOwner_anyAmount_autoApproved() {
        // MERCHANT_OWNER has no threshold → always auto-approved
        AuthenticatedActor owner = new AuthenticatedActor(
                99L, "owner", "AU-99", "MERCHANT_OWNER", 1L, 1L, Set.of(1L), Set.of("REFUND_LARGE"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner, null, List.of()));

        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(any(), eq("CONFIRMED")))
                .thenReturn(List.of());

        CreateRefundCommand cmd = new CreateRefundCommand(1L, 9000, "PARTIAL", "big refund", null);
        RefundRecordDto result = service.createRefund(cmd);

        assertThat(result.approvalStatus()).isEqualTo("AUTO_APPROVED");
        assertThat(result.refundStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void cashier_underThreshold_autoApproved() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(any(), eq("CONFIRMED")))
                .thenReturn(List.of());

        CreateRefundCommand cmd = new CreateRefundCommand(1L, 3000, "PARTIAL", "reason", null);
        RefundRecordDto result = service.createRefund(cmd);

        assertThat(result.approvalStatus()).isEqualTo("AUTO_APPROVED");
        assertThat(result.refundStatus()).isEqualTo("COMPLETED");
        verify(settlementRecordRepository).save(any());
    }

    @Test
    void approveRefund_completesRefund_usingPessimisticLock() {
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
        assertThat(result.refundStatus()).isEqualTo("COMPLETED");
        assertThat(result.approvedBy()).isEqualTo(42L);
        verify(settlementRecordRepository).save(any());
        verify(storeAccessEnforcer).enforce(1L);
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
        verify(paymentHoldRepository, never()).findAllBySettlementRecordIdAndHoldStatus(any(), any());
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
