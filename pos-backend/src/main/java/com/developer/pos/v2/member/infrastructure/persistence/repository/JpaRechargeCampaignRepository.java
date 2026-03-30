package com.developer.pos.v2.member.infrastructure.persistence.repository;

import com.developer.pos.v2.member.infrastructure.persistence.entity.RechargeCampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRechargeCampaignRepository extends JpaRepository<RechargeCampaignEntity, Long> {
    List<RechargeCampaignEntity> findByMerchantIdAndCampaignStatus(Long merchantId, String campaignStatus);
}
