package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.RechargeCampaignEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaRechargeCampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RechargeCampaignServiceTest {

    @Mock
    private JpaRechargeCampaignRepository rechargeCampaignRepository;

    @InjectMocks
    private RechargeCampaignService rechargeCampaignService;

    @Test
    void findBestBonus_matchingCampaign_returnsBonusCash() throws Exception {
        RechargeCampaignEntity campaign = buildCampaign(1L, 5000L, 1000L, 0L, 0, 0, 0, null, null);

        when(rechargeCampaignRepository.findByMerchantIdAndCampaignStatus(1L, "ACTIVE"))
                .thenReturn(List.of(campaign));

        RechargeCampaignService.CampaignBonus result =
                rechargeCampaignService.findBestBonus(1L, 10000L, 0);

        assertThat(result.bonusCashCents()).isEqualTo(1000L);
        assertThat(result.bonusPoints()).isEqualTo(0L);
        assertThat(result.campaignId()).isEqualTo(1L);
    }

    @Test
    void findBestBonus_amountBelowThreshold_returnsNone() throws Exception {
        RechargeCampaignEntity campaign = buildCampaign(1L, 5000L, 1000L, 0L, 0, 0, 0, null, null);

        when(rechargeCampaignRepository.findByMerchantIdAndCampaignStatus(1L, "ACTIVE"))
                .thenReturn(List.of(campaign));

        RechargeCampaignService.CampaignBonus result =
                rechargeCampaignService.findBestBonus(1L, 3000L, 0);

        assertThat(result).isEqualTo(RechargeCampaignService.CampaignBonus.none());
    }

    @Test
    void findBestBonus_expiredCampaign_skipped() throws Exception {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        RechargeCampaignEntity campaign = buildCampaign(1L, 5000L, 1000L, 0L, 0, 0, 0, null, yesterday);

        when(rechargeCampaignRepository.findByMerchantIdAndCampaignStatus(1L, "ACTIVE"))
                .thenReturn(List.of(campaign));

        RechargeCampaignService.CampaignBonus result =
                rechargeCampaignService.findBestBonus(1L, 10000L, 0);

        assertThat(result).isEqualTo(RechargeCampaignService.CampaignBonus.none());
    }

    private RechargeCampaignEntity buildCampaign(Long id, long rechargeAmountCents,
                                                  long bonusAmountCents, long bonusPoints,
                                                  int minTierLevel, int maxPerMember,
                                                  int usedQuota,
                                                  LocalDateTime startsAt, LocalDateTime endsAt)
            throws Exception {
        RechargeCampaignEntity c = new RechargeCampaignEntity();
        Field idField = RechargeCampaignEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(c, id);

        c.setMerchantId(1L);
        c.setRechargeAmountCents(rechargeAmountCents);
        c.setBonusAmountCents(bonusAmountCents);
        c.setBonusPoints(bonusPoints);
        c.setMinTierLevel(minTierLevel);
        c.setMaxPerMember(maxPerMember);
        c.setTotalQuota(0);
        c.setUsedQuota(usedQuota);
        c.setStartsAt(startsAt);
        c.setEndsAt(endsAt);
        c.setCampaignCode("TEST");
        c.setCampaignName("Test Campaign");
        c.setCampaignStatus("ACTIVE");
        c.setSortOrder(0);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }
}
