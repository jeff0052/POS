package com.developer.pos.v2.channel.infrastructure.persistence.repository;

import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelSettlementBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaChannelSettlementBatchRepository extends JpaRepository<ChannelSettlementBatchEntity, Long> {

    List<ChannelSettlementBatchEntity> findByChannelIdOrderByPeriodStartDesc(Long channelId);

    Optional<ChannelSettlementBatchEntity> findByIdAndChannelId(Long id, Long channelId);
}
