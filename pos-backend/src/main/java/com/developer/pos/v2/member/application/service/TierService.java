package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberTierRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberTierRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TierService {

    private final JpaMemberRepository memberRepo;
    private final JpaMemberAccountRepository memberAccountRepo;
    private final JpaMemberTierRuleRepository tierRuleRepo;

    public TierService(
            JpaMemberRepository memberRepo,
            JpaMemberAccountRepository memberAccountRepo,
            JpaMemberTierRuleRepository tierRuleRepo) {
        this.memberRepo = memberRepo;
        this.memberAccountRepo = memberAccountRepo;
        this.tierRuleRepo = tierRuleRepo;
    }

    /**
     * Check if member has crossed a tier upgrade threshold and upgrade if needed.
     * Called after lifetime spend is updated.
     */
    @Transactional
    public void checkAndUpgrade(Long memberId, Long merchantId) {
        MemberEntity member = memberRepo.findById(memberId).orElse(null);
        if (member == null) return;

        MemberAccountEntity account = memberAccountRepo.findByMemberId(memberId).orElse(null);
        if (account == null) return;

        List<MemberTierRuleEntity> tiers = tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId);
        if (tiers.isEmpty()) return;

        // Find highest tier whose threshold is met
        String bestTierCode = member.getTierCode(); // default: stay at current
        for (MemberTierRuleEntity tier : tiers) {
            if ("LIFETIME_SPEND".equals(tier.getUpgradeType())
                    && account.getLifetimeSpendCents() >= tier.getUpgradeThresholdCents()) {
                bestTierCode = tier.getTierCode();
            }
        }

        if (!bestTierCode.equalsIgnoreCase(member.getTierCode())) {
            member.setTierCode(bestTierCode);
            memberRepo.save(member);
        }
    }
}
