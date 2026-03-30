package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.application.service.RechargeCampaignService.CampaignBonus;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.RechargeCampaignEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCashLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRechargeOrderRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberTierRuleRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaRechargeCampaignRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberApplicationServiceTest {

    @Mock JpaMemberRepository memberRepository;
    @Mock JpaMemberAccountRepository memberAccountRepository;
    @Mock JpaMemberRechargeOrderRepository memberRechargeOrderRepository;
    @Mock JpaMemberPointsLedgerRepository memberPointsLedgerRepository;
    @Mock JpaActiveTableOrderRepository activeTableOrderRepository;
    @Mock RechargeCampaignService rechargeCampaignService;
    @Mock JpaRechargeCampaignRepository rechargeCampaignRepository;
    @Mock JpaMemberCashLedgerRepository memberCashLedgerRepository;
    @Mock JpaMemberTierRuleRepository memberTierRuleRepository;

    @InjectMocks MemberApplicationService service;

    private MemberEntity buildMember(Long id, Long merchantId) {
        MemberEntity m = new MemberEntity();
        m.setMerchantId(merchantId);
        m.setMemberNo("MEM000001");
        m.setName("Test Member");
        m.setPhone("13800000001");
        m.setTierCode("STANDARD");
        m.setMemberStatus("ACTIVE");
        return m;
    }

    private MemberAccountEntity buildAccount(long pointsBalance, long availablePoints) {
        MemberAccountEntity a = new MemberAccountEntity();
        a.setPointsBalance(pointsBalance);
        a.setAvailablePoints(availablePoints);
        a.setCashBalanceCents(0);
        a.setLifetimeSpendCents(0);
        a.setLifetimeRechargeCents(0);
        return a;
    }

    @Test
    void getMember_wrongMerchant_throws() {
        Long memberId = 1L;
        MemberEntity member = buildMember(memberId, 1L);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.getMember(memberId, 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechargeMember_wrongMerchant_throws() {
        Long memberId = 1L;
        MemberEntity member = buildMember(memberId, 1L);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.rechargeMember(memberId, 2L, 10000L, 0L, "staff"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechargeMember_campaignBonusPoints_awarded() {
        Long memberId = 1L;
        Long merchantId = 1L;
        MemberEntity member = buildMember(memberId, merchantId);
        MemberAccountEntity account = buildAccount(100L, 100L);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberAccountRepository.findByMemberId(memberId)).thenReturn(Optional.of(account));
        when(memberTierRuleRepository.findByMerchantIdOrderByTierLevelAsc(merchantId))
                .thenReturn(Collections.emptyList());
        when(rechargeCampaignService.findBestBonus(merchantId, memberId, 10000L, 0))
                .thenReturn(new CampaignBonus(500, 50L, 1L));
        when(memberRechargeOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberPointsLedgerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberCashLedgerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargeCampaignEntity campaign = new RechargeCampaignEntity();
        campaign.setUsedQuota(0);
        when(rechargeCampaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        service.rechargeMember(memberId, merchantId, 10000L, 0L, "staff");

        assertThat(account.getPointsBalance()).isEqualTo(150L);
        assertThat(account.getAvailablePoints()).isEqualTo(150L);
    }
}
