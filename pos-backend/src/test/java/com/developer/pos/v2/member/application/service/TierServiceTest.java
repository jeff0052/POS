package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberTierRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberTierRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class TierServiceTest {

    @Mock JpaMemberRepository memberRepo;
    @Mock JpaMemberAccountRepository memberAccountRepo;
    @Mock JpaMemberTierRuleRepository tierRuleRepo;

    @InjectMocks TierService service;

    private MemberEntity buildMember(String tierCode) {
        MemberEntity m = new MemberEntity();
        m.setTierCode(tierCode);
        return m;
    }

    private MemberAccountEntity buildAccount(long lifetimeSpendCents) {
        MemberAccountEntity a = new MemberAccountEntity();
        a.setLifetimeSpendCents(lifetimeSpendCents);
        return a;
    }

    private MemberTierRuleEntity buildTierRule(String tierCode, long thresholdCents) {
        MemberTierRuleEntity t = new MemberTierRuleEntity();
        t.setTierCode(tierCode);
        t.setUpgradeType("LIFETIME_SPEND");
        t.setUpgradeThresholdCents(thresholdCents);
        t.setPointsMultiplier(BigDecimal.ONE);
        return t;
    }

    @Test
    void checkAndUpgrade_thresholdMet_upgradesTier() {
        // lifetimeSpend=10000 cents, SILVER threshold=5000 → upgrades to SILVER
        Long memberId = 1L;
        Long merchantId = 10L;

        MemberEntity member = buildMember("STANDARD");
        when(memberRepo.findById(memberId)).thenReturn(Optional.of(member));
        when(memberAccountRepo.findByMemberId(memberId)).thenReturn(Optional.of(buildAccount(10000L)));
        when(tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId))
                .thenReturn(List.of(
                        buildTierRule("STANDARD", 0L),
                        buildTierRule("SILVER", 5000L)
                ));

        service.checkAndUpgrade(memberId, merchantId);

        assertThat(member.getTierCode()).isEqualTo("SILVER");
        verify(memberRepo).save(member);
    }

    @Test
    void checkAndUpgrade_alreadyAtTier_noChange() {
        // already SILVER, GOLD threshold=50000, spend=10000 → no change
        Long memberId = 2L;
        Long merchantId = 10L;

        MemberEntity member = buildMember("SILVER");
        when(memberRepo.findById(memberId)).thenReturn(Optional.of(member));
        when(memberAccountRepo.findByMemberId(memberId)).thenReturn(Optional.of(buildAccount(10000L)));
        when(tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId))
                .thenReturn(List.of(
                        buildTierRule("STANDARD", 0L),
                        buildTierRule("SILVER", 5000L),
                        buildTierRule("GOLD", 50000L)
                ));

        service.checkAndUpgrade(memberId, merchantId);

        // bestTierCode = SILVER (same as current), no save
        verify(memberRepo, never()).save(any());
    }

    @Test
    void checkAndUpgrade_noTierRules_noChange() {
        // empty rules list → member.save never called
        Long memberId = 3L;
        Long merchantId = 10L;

        MemberEntity member = buildMember("STANDARD");
        when(memberRepo.findById(memberId)).thenReturn(Optional.of(member));
        when(memberAccountRepo.findByMemberId(memberId)).thenReturn(Optional.of(buildAccount(999999L)));
        when(tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId)).thenReturn(List.of());

        service.checkAndUpgrade(memberId, merchantId);

        verify(memberRepo, never()).save(any());
    }
}
