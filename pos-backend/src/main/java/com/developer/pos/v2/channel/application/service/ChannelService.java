package com.developer.pos.v2.channel.application.service;

import com.developer.pos.v2.channel.application.dto.ChannelDto;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelRepository;
import com.developer.pos.v2.common.application.UseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ChannelService implements UseCase {

    private final JpaChannelRepository channelRepository;

    public ChannelService(JpaChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Transactional(readOnly = true)
    public List<ChannelDto> listChannels(Long merchantId) {
        return channelRepository.findByMerchantIdAndChannelStatusOrderByIdDesc(merchantId, "ACTIVE")
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChannelDto getChannel(Long merchantId, Long channelId) {
        ChannelEntity entity = channelRepository.findByIdAndMerchantId(channelId, merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        return toDto(entity);
    }

    @Transactional
    public ChannelDto createChannel(Long merchantId, String channelCode, String channelName,
                                    String channelType, String contactName, String contactPhone,
                                    String contactEmail) {
        channelRepository.findByMerchantIdAndChannelCode(merchantId, channelCode)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Channel code already exists: " + channelCode);
                });

        OffsetDateTime now = OffsetDateTime.now();
        ChannelEntity entity = new ChannelEntity();
        entity.setMerchantId(merchantId);
        entity.setChannelCode(channelCode);
        entity.setChannelName(channelName);
        entity.setChannelType(channelType);
        entity.setContactName(contactName);
        entity.setContactPhone(contactPhone);
        entity.setContactEmail(contactEmail);
        entity.setChannelStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return toDto(channelRepository.save(entity));
    }

    @Transactional
    public ChannelDto updateChannel(Long merchantId, Long channelId, String channelName,
                                    String channelType, String contactName, String contactPhone,
                                    String contactEmail) {
        ChannelEntity entity = channelRepository.findByIdAndMerchantId(channelId, merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));

        if (channelName != null) entity.setChannelName(channelName);
        if (channelType != null) entity.setChannelType(channelType);
        if (contactName != null) entity.setContactName(contactName);
        if (contactPhone != null) entity.setContactPhone(contactPhone);
        if (contactEmail != null) entity.setContactEmail(contactEmail);
        entity.setUpdatedAt(OffsetDateTime.now());

        return toDto(channelRepository.save(entity));
    }

    @Transactional
    public void deactivateChannel(Long merchantId, Long channelId) {
        ChannelEntity entity = channelRepository.findByIdAndMerchantId(channelId, merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        entity.setChannelStatus("INACTIVE");
        entity.setUpdatedAt(OffsetDateTime.now());
        channelRepository.save(entity);
    }

    private ChannelDto toDto(ChannelEntity e) {
        return new ChannelDto(
                e.getId(), e.getMerchantId(), e.getChannelCode(), e.getChannelName(),
                e.getChannelType(), e.getContactName(), e.getContactPhone(), e.getContactEmail(),
                e.getTrackingParam(), e.getTrackingUrlPrefix(), e.getChannelStatus(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
