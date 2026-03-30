package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberTierRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberTierRuleRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsBatchRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsEarningServiceTest {

    @Mock JpaPointsRuleRepository pointsRuleRepo;
    @Mock JpaPointsBatchRepository pointsBatchRepo;
    @Mock JpaMemberAccountRepository memberAccountRepo;
    @Mock JpaMemberPointsLedgerRepository memberPointsLedgerRepo;
    @Mock JpaMemberRepository memberRepo;
    @Mock JpaMemberTierRuleRepository tierRuleRepo;

    @InjectMocks PointsEarningService service;

    private PointsRuleEntity buildSpendRule(int pointsPerDollar, long minSpendCents, Long maxPointsPerOrder) {
        PointsRuleEntity rule = new PointsRuleEntity();
        rule.setRuleType("SPEND");
        rule.setPointsPerDollar(pointsPerDollar);
        rule.setMinSpendCents(minSpendCents);
        rule.setMaxPointsPerOrder(maxPointsPerOrder);
        return rule;
    }

    private MemberEntity buildMember(Long id, String tierCode) {
        MemberEntity m = new MemberEntity();
        m.setTierCode(tierCode);
        return m;
    }

    private MemberAccountEntity buildAccount(long pointsBalance, long availablePoints) {
        MemberAccountEntity a = new MemberAccountEntity();
        a.setPointsBalance(pointsBalance);
        a.setAvailablePoints(availablePoints);
        return a;
    }

    private MemberTierRuleEntity buildTierRule(String tierCode, BigDecimal multiplier) {
        MemberTierRuleEntity t = new MemberTierRuleEntity();
        t.setTierCode(tierCode);
        t.setPointsMultiplier(multiplier);
        t.setUpgradeType("LIFETIME_SPEND");
        t.setUpgradeThresholdCents(0L);
        return t;
    }

    @Test
    void awardPoints_basicSpend_createsLedgerAndBatch() {
        // 1000 cents spend, rule 1pt/dollar → 10pts, STANDARD tier 1.0x → 10pts awarded
        Long memberId = 1L;
        Long merchantId = 10L;
        Long settlementId = 100L;

        when(pointsRuleRepo.findByMerchantIdAndRuleStatus(merchantId, "ACTIVE"))
                .thenReturn(List.of(buildSpendRule(1, 0L, null)));
        when(memberRepo.findById(memberId))
                .thenReturn(Optional.of(buildMember(memberId, "STANDARD")));
        when(tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId))
                .thenReturn(List.of(buildTierRule("STANDARD", BigDecimal.ONE)));

        MemberAccountEntity account = buildAccount(0L, 0L);
        when(memberAccountRepo.findByMemberId(memberId)).thenReturn(Optional.of(account));

        service.awardPostSettlementPoints(memberId, merchantId, settlementId, 1000L);

        verify(pointsBatchRepo).save(any());
        verify(memberAccountRepo).save(account);
        assertThat(account.getPointsBalance()).isEqualTo(10L);
        assertThat(account.getAvailablePoints()).isEqualTo(10L);
        verify(memberPointsLedgerRepo).save(any());
    }

    @Test
    void awardPoints_tierMultiplier_appliesCorrectly() {
        // 1000 cents spend, rule 1pt/dollar, tier GOLD 1.5x → 15pts
        Long memberId = 2L;
        Long merchantId = 10L;
        Long settlementId = 101L;

        when(pointsRuleRepo.findByMerchantIdAndRuleStatus(merchantId, "ACTIVE"))
                .thenReturn(List.of(buildSpendRule(1, 0L, null)));
        when(memberRepo.findById(memberId))
                .thenReturn(Optional.of(buildMember(memberId, "GOLD")));
        when(tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId))
                .thenReturn(List.of(
                        buildTierRule("STANDARD", BigDecimal.ONE),
                        buildTierRule("GOLD", new BigDecimal("1.5"))
                ));

        MemberAccountEntity account = buildAccount(0L, 0L);
        when(memberAccountRepo.findByMemberId(memberId)).thenReturn(Optional.of(account));

        service.awardPostSettlementPoints(memberId, merchantId, settlementId, 1000L);

        assertThat(account.getPointsBalance()).isEqualTo(15L);
        assertThat(account.getAvailablePoints()).isEqualTo(15L);
        verify(pointsBatchRepo).save(any());
        verify(memberPointsLedgerRepo).save(any());
    }

    @Test
    void awardPoints_noActiveRule_skipsAward() {
        Long memberId = 3L;
        Long merchantId = 10L;

        when(pointsRuleRepo.findByMerchantIdAndRuleStatus(merchantId, "ACTIVE"))
                .thenReturn(List.of());

        service.awardPostSettlementPoints(memberId, merchantId, null, 1000L);

        verify(memberAccountRepo, never()).save(any());
        verify(pointsBatchRepo, never()).save(any());
        verify(memberPointsLedgerRepo, never()).save(any());
    }

    @Test
    void awardPoints_belowMinSpend_skipsAward() {
        // minSpendCents=5000, spendCents=3000 → skip
        Long memberId = 4L;
        Long merchantId = 10L;

        when(pointsRuleRepo.findByMerchantIdAndRuleStatus(merchantId, "ACTIVE"))
                .thenReturn(List.of(buildSpendRule(1, 5000L, null)));

        service.awardPostSettlementPoints(memberId, merchantId, null, 3000L);

        verify(memberAccountRepo, never()).save(any());
        verify(pointsBatchRepo, never()).save(any());
    }

    @Test
    void awardPoints_alreadyAwarded_isIdempotent() {
        Long memberId = 6L;
        Long merchantId = 10L;
        Long settlementId = 99L;

        when(pointsRuleRepo.findByMerchantIdAndRuleStatus(merchantId, "ACTIVE"))
                .thenReturn(List.of(buildSpendRule(1, 0L, 0L)));
        when(memberRepo.findById(memberId))
                .thenReturn(Optional.of(buildMember(memberId, "STANDARD")));
        when(tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId))
                .thenReturn(List.of(buildTierRule("STANDARD", BigDecimal.ONE)));
        when(pointsBatchRepo.existsByMemberIdAndSourceTypeAndSourceRef(memberId, "SPEND", "99"))
                .thenReturn(true);

        service.awardPostSettlementPoints(memberId, merchantId, settlementId, 1000L);

        verify(memberAccountRepo, never()).save(any());
        verify(pointsBatchRepo, never()).save(any());
    }

    @Test
    void awardPoints_capsAtMaxPointsPerOrder() {
        // 10000 cents, 2pt/dollar=200pts, but maxPointsPerOrder=50 → 50pts
        Long memberId = 5L;
        Long merchantId = 10L;
        Long settlementId = 102L;

        when(pointsRuleRepo.findByMerchantIdAndRuleStatus(merchantId, "ACTIVE"))
                .thenReturn(List.of(buildSpendRule(2, 0L, 50L)));
        when(memberRepo.findById(memberId))
                .thenReturn(Optional.of(buildMember(memberId, "STANDARD")));
        when(tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId))
                .thenReturn(List.of(buildTierRule("STANDARD", BigDecimal.ONE)));

        MemberAccountEntity account = buildAccount(0L, 0L);
        when(memberAccountRepo.findByMemberId(memberId)).thenReturn(Optional.of(account));

        service.awardPostSettlementPoints(memberId, merchantId, settlementId, 10000L);

        assertThat(account.getPointsBalance()).isEqualTo(50L);
        assertThat(account.getAvailablePoints()).isEqualTo(50L);
        verify(pointsBatchRepo).save(any());
        verify(memberPointsLedgerRepo).save(any());
    }
}
