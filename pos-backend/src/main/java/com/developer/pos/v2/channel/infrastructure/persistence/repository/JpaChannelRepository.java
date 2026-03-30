package com.developer.pos.v2.channel.infrastructure.persistence.repository;

import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaChannelRepository extends JpaRepository<ChannelEntity, Long> {

    List<ChannelEntity> findByMerchantIdAndChannelStatusOrderByIdDesc(Long merchantId, String channelStatus);

    Optional<ChannelEntity> findByMerchantIdAndChannelCode(Long merchantId, String channelCode);

    Optional<ChannelEntity> findByIdAndMerchantId(Long id, Long merchantId);
}
