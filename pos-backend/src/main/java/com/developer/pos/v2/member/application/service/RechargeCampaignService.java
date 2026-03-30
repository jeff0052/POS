package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.RechargeCampaignEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaRechargeCampaignRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class RechargeCampaignService {

    private final JpaRechargeCampaignRepository rechargeCampaignRepository;

    public RechargeCampaignService(JpaRechargeCampaignRepository rechargeCampaignRepository) {
        this.rechargeCampaignRepository = rechargeCampaignRepository;
    }

    public record CampaignBonus(long bonusCashCents, long bonusPoints, Long campaignId) {
        public static CampaignBonus none() {
            return new CampaignBonus(0, 0, null);
        }

        public boolean hasBonus() {
            return bonusCashCents > 0 || bonusPoints > 0;
        }
    }

    public CampaignBonus findBestBonus(Long merchantId, long rechargeAmountCents, int memberTierLevel) {
        List<RechargeCampaignEntity> activeCampaigns =
                rechargeCampaignRepository.findByMerchantIdAndCampaignStatus(merchantId, "ACTIVE");

        LocalDateTime now = LocalDateTime.now();

        return activeCampaigns.stream()
                .filter(c -> rechargeAmountCents >= c.getRechargeAmountCents())
                .filter(c -> memberTierLevel >= c.getMinTierLevel())
                .filter(c -> c.getStartsAt() == null || now.isAfter(c.getStartsAt()))
                .filter(c -> c.getEndsAt() == null || now.isBefore(c.getEndsAt()))
                .filter(c -> c.getTotalQuota() == 0 || c.getUsedQuota() < c.getTotalQuota())
                .max(Comparator.comparingLong(RechargeCampaignEntity::getRechargeAmountCents))
                .map(c -> new CampaignBonus(c.getBonusAmountCents(), c.getBonusPoints(), c.getId()))
                .orElse(CampaignBonus.none());
    }
}
