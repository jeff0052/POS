package com.developer.pos.v2.channel.infrastructure.persistence.repository;

import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelPerformanceDailyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JpaChannelPerformanceDailyRepository extends JpaRepository<ChannelPerformanceDailyEntity, Long> {

    List<ChannelPerformanceDailyEntity> findByChannelIdAndReportDateBetweenOrderByReportDateDesc(Long channelId, LocalDate start, LocalDate end);
}
