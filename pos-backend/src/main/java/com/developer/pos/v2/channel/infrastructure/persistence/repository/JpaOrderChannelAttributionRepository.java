package com.developer.pos.v2.channel.infrastructure.persistence.repository;

import com.developer.pos.v2.channel.infrastructure.persistence.entity.OrderChannelAttributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaOrderChannelAttributionRepository extends JpaRepository<OrderChannelAttributionEntity, Long> {

    Optional<OrderChannelAttributionEntity> findBySubmittedOrderId(Long submittedOrderId);

    List<OrderChannelAttributionEntity> findByChannelIdAndCreatedAtBetween(Long channelId, OffsetDateTime start, OffsetDateTime end);
}
