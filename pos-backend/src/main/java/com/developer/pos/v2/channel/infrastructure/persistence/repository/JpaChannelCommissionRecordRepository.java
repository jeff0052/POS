package com.developer.pos.v2.channel.infrastructure.persistence.repository;

import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelCommissionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaChannelCommissionRecordRepository extends JpaRepository<ChannelCommissionRecordEntity, Long> {

    List<ChannelCommissionRecordEntity> findByChannelIdAndCommissionStatusOrderByCreatedAtDesc(Long channelId, String commissionStatus);

    List<ChannelCommissionRecordEntity> findBySettlementBatchId(Long settlementBatchId);
}
