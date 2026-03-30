package com.developer.pos.v2.channel.infrastructure.persistence.repository;

import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelCommissionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaChannelCommissionRuleRepository extends JpaRepository<ChannelCommissionRuleEntity, Long> {

    List<ChannelCommissionRuleEntity> findByChannelIdAndRuleStatusOrderByIdDesc(Long channelId, String ruleStatus);
}
