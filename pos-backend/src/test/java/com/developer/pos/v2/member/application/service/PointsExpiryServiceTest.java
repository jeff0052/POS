package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberPointsLedgerEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsBatchEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PointsExpiryServiceTest {

    @Mock
    private JpaPointsBatchRepository pointsBatchRepo;

    @Mock
    private JpaMemberAccountRepository memberAccountRepo;

    @Mock
    private JpaMemberPointsLedgerRepository memberPointsLedgerRepo;

    @Mock
    private JpaMemberRepository memberRepo;

    @InjectMocks
    private PointsExpiryService pointsExpiryService;

    @Test
    void expireBatches_expiredBatches_deductsFromBalance() {
        PointsBatchEntity batch = new PointsBatchEntity();
        batch.setMemberId(10L);
        batch.setRemainingPoints(200L);
        batch.setBatchStatus("ACTIVE");
        batch.setExpiredPoints(0L);
        batch.setExpiresAt(LocalDateTime.now().minusDays(1));

        MemberAccountEntity account = new MemberAccountEntity();
        account.setMemberId(10L);
        account.setPointsBalance(500L);
        account.setAvailablePoints(500L);

        MemberEntity member = new MemberEntity();
        member.setMerchantId(1L);

        when(pointsBatchRepo.findByBatchStatusAndExpiresAtBefore(anyString(), any()))
                .thenReturn(List.of(batch));
        when(memberAccountRepo.findByMemberId(10L)).thenReturn(Optional.of(account));
        when(memberRepo.findById(10L)).thenReturn(Optional.of(member));
        when(memberPointsLedgerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberAccountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pointsBatchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pointsExpiryService.expirePointsBatches();

        ArgumentCaptor<MemberAccountEntity> accountCaptor = ArgumentCaptor.forClass(MemberAccountEntity.class);
        verify(memberAccountRepo).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getPointsBalance()).isEqualTo(300L);
        assertThat(accountCaptor.getValue().getAvailablePoints()).isEqualTo(300L);

        ArgumentCaptor<MemberPointsLedgerEntity> ledgerCaptor = ArgumentCaptor.forClass(MemberPointsLedgerEntity.class);
        verify(memberPointsLedgerRepo).save(ledgerCaptor.capture());
        assertThat(ledgerCaptor.getValue().getChangeType()).isEqualTo("EXPIRE");
        assertThat(ledgerCaptor.getValue().getPointsDelta()).isEqualTo(-200L);
        assertThat(ledgerCaptor.getValue().getBalanceAfter()).isEqualTo(300L);
    }

    @Test
    void expireBatches_noExpiredBatches_noOp() {
        when(pointsBatchRepo.findByBatchStatusAndExpiresAtBefore(anyString(), any()))
                .thenReturn(List.of());

        pointsExpiryService.expirePointsBatches();

        verify(memberAccountRepo, never()).save(any());
        verify(memberPointsLedgerRepo, never()).save(any());
    }

    @Test
    void expireBatches_multipleBatchesSameMember_aggregatesDeduction() {
        PointsBatchEntity batch1 = new PointsBatchEntity();
        batch1.setMemberId(20L);
        batch1.setRemainingPoints(100L);
        batch1.setBatchStatus("ACTIVE");
        batch1.setExpiredPoints(0L);
        batch1.setExpiresAt(LocalDateTime.now().minusDays(2));

        PointsBatchEntity batch2 = new PointsBatchEntity();
        batch2.setMemberId(20L);
        batch2.setRemainingPoints(150L);
        batch2.setBatchStatus("ACTIVE");
        batch2.setExpiredPoints(0L);
        batch2.setExpiresAt(LocalDateTime.now().minusDays(1));

        MemberAccountEntity account = new MemberAccountEntity();
        account.setMemberId(20L);
        account.setPointsBalance(400L);
        account.setAvailablePoints(400L);

        MemberEntity member = new MemberEntity();
        member.setMerchantId(1L);

        when(pointsBatchRepo.findByBatchStatusAndExpiresAtBefore(anyString(), any()))
                .thenReturn(List.of(batch1, batch2));
        when(memberAccountRepo.findByMemberId(20L)).thenReturn(Optional.of(account));
        when(memberRepo.findById(20L)).thenReturn(Optional.of(member));
        when(memberPointsLedgerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberAccountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pointsBatchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pointsExpiryService.expirePointsBatches();

        // Only one account update for the same member
        ArgumentCaptor<MemberAccountEntity> accountCaptor = ArgumentCaptor.forClass(MemberAccountEntity.class);
        verify(memberAccountRepo).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getPointsBalance()).isEqualTo(150L); // 400 - 250

        // Only one ledger entry for the same member
        ArgumentCaptor<MemberPointsLedgerEntity> ledgerCaptor = ArgumentCaptor.forClass(MemberPointsLedgerEntity.class);
        verify(memberPointsLedgerRepo).save(ledgerCaptor.capture());
        assertThat(ledgerCaptor.getValue().getPointsDelta()).isEqualTo(-250L);
    }
}
