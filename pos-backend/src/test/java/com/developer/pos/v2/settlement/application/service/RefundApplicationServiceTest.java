package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.settlement.application.command.ApproveRefundCommand;
import com.developer.pos.v2.settlement.application.command.CreateRefundCommand;
import com.developer.pos.v2.settlement.application.dto.RefundRecordDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundLineItemEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundLineItemRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundRecordRepository;
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
    @Mock private StoreAccessEnforcer storeAccessEnforcer;

    @InjectMocks private RefundApplicationService service;

    private SettlementRecordEntity settlement;

    @BeforeEach
    void setUp() {
        // Set up SecurityContext with a test actor
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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fullRefund_setsReversalAmounts() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRefundCommand command = new CreateRefundCommand(1L, 0, "FULL", "customer request", 0L, null);
        RefundRecordDto result = service.createRefund(command);

        assertThat(result.refundAmountCents()).isEqualTo(10000);
        assertThat(result.pointsReversedCents()).isEqualTo(2000);
        assertThat(result.cashReversedCents()).isEqualTo(3000);
        assertThat(result.couponReversed()).isTrue();
        assertThat(result.externalRefundStatus()).isEqualTo("PENDING");
        assertThat(result.operatedBy()).isEqualTo(42L); // from AuthContext, not request
        verify(storeAccessEnforcer).enforce(1L);
    }

    @Test
    void partialRefund_withItems_tracksLineItems() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
        CreateRefundCommand command = new CreateRefundCommand(1L, 3000, "PARTIAL", "partial return", 0L, items);
        RefundRecordDto result = service.createRefund(command);

        verify(refundLineItemRepository).saveAll(argThat(list -> ((List<?>) list).size() == 2));
        assertThat(result.lineItems()).hasSize(2);
    }

    @Test
    void refund_overThreshold_requiresApproval() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRefundCommand command = new CreateRefundCommand(1L, 6000, "PARTIAL", "reason", 5000L, null);
        RefundRecordDto result = service.createRefund(command);

        assertThat(result.approvalStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(result.refundStatus()).isEqualTo("PENDING");
        verify(settlementRecordRepository, never()).save(any());
    }

    @Test
    void refund_underThreshold_autoApproved() {
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRefundCommand command = new CreateRefundCommand(1L, 3000, "PARTIAL", "reason", 5000L, null);
        RefundRecordDto result = service.createRefund(command);

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

        // Must use findByRefundNoForUpdate (pessimistic lock), not findByRefundNo
        when(refundRecordRepository.findByRefundNoForUpdate("REF001")).thenReturn(Optional.of(refund));
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));
        when(refundRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refundLineItemRepository.findByRefundId(any())).thenReturn(List.of());

        ApproveRefundCommand command = new ApproveRefundCommand("REF001", true);
        RefundRecordDto result = service.approveRefund(command);

        assertThat(result.approvalStatus()).isEqualTo("APPROVED");
        assertThat(result.refundStatus()).isEqualTo("COMPLETED");
        assertThat(result.approvedBy()).isEqualTo(42L); // from AuthContext, not request
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

        ApproveRefundCommand command = new ApproveRefundCommand("REF002", false);
        RefundRecordDto result = service.approveRefund(command);

        assertThat(result.approvalStatus()).isEqualTo("REJECTED");
        assertThat(result.refundStatus()).isEqualTo("REJECTED");
        verify(settlementRecordRepository, never()).save(any());
    }

    @Test
    void refund_nonSettledOrder_throws() {
        settlement.setFinalStatus("PENDING");
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));

        CreateRefundCommand command = new CreateRefundCommand(1L, 5000, "PARTIAL", "reason", 0L, null);

        assertThatThrownBy(() -> service.createRefund(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only refund SETTLED orders");
    }

    @Test
    void refund_exceedsRefundable_throws() {
        settlement.setRefundedAmountCents(9000);
        when(settlementRecordRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(settlement));

        CreateRefundCommand command = new CreateRefundCommand(1L, 5000, "PARTIAL", "reason", 0L, null);

        assertThatThrownBy(() -> service.createRefund(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds refundable");
    }
}
