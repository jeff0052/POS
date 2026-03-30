package com.developer.pos.v2.channel.application.service;

import com.developer.pos.v2.channel.application.dto.CommissionResultDto;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelCommissionRecordEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelCommissionRuleEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.OrderChannelAttributionEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelCommissionRecordRepository;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelCommissionRuleRepository;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaOrderChannelAttributionRepository;
import com.developer.pos.v2.common.application.UseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChannelAttributionService implements UseCase {

    private final JpaOrderChannelAttributionRepository attributionRepository;
    private final JpaChannelCommissionRuleRepository commissionRuleRepository;
    private final JpaChannelCommissionRecordRepository commissionRecordRepository;

    public ChannelAttributionService(
            JpaOrderChannelAttributionRepository attributionRepository,
            JpaChannelCommissionRuleRepository commissionRuleRepository,
            JpaChannelCommissionRecordRepository commissionRecordRepository
    ) {
        this.attributionRepository = attributionRepository;
        this.commissionRuleRepository = commissionRuleRepository;
        this.commissionRecordRepository = commissionRecordRepository;
    }

    @Transactional
    public void attributeOrder(Long submittedOrderId, Long channelId, String trackingValue) {
        OffsetDateTime now = OffsetDateTime.now();
        OrderChannelAttributionEntity entity = new OrderChannelAttributionEntity();
        entity.setSubmittedOrderId(submittedOrderId);
        entity.setChannelId(channelId);
        entity.setAttributionType("TRACKING");
        entity.setTrackingValue(trackingValue);
        entity.setConversionAt(now);
        entity.setCreatedAt(now);
        attributionRepository.save(entity);
    }

    @Transactional
    public CommissionResultDto calculateCommission(Long channelId, Long submittedOrderId,
                                                   Long storeId, long orderAmountCents) {
        List<ChannelCommissionRuleEntity> rules =
                commissionRuleRepository.findByChannelIdAndRuleStatusOrderByIdDesc(channelId, "ACTIVE");

        if (rules.isEmpty()) {
            throw new IllegalStateException("No active commission rule found for channel: " + channelId);
        }

        ChannelCommissionRuleEntity rule = rules.get(0);
        long commissionAmountCents = calculateAmount(rule, orderAmountCents);

        // Apply min/max cap
        if (rule.getMinCommissionCents() != null && commissionAmountCents < rule.getMinCommissionCents()) {
            commissionAmountCents = rule.getMinCommissionCents();
        }
        if (rule.getMaxCommissionCents() != null && commissionAmountCents > rule.getMaxCommissionCents()) {
            commissionAmountCents = rule.getMaxCommissionCents();
        }

        String commissionNo = "COM" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        ChannelCommissionRecordEntity record = new ChannelCommissionRecordEntity();
        record.setCommissionNo(commissionNo);
        record.setChannelId(channelId);
        record.setRuleId(rule.getId());
        record.setSubmittedOrderId(submittedOrderId);
        record.setStoreId(storeId);
        record.setOrderAmountCents(orderAmountCents);
        record.setCalculationBaseCents(orderAmountCents);
        record.setCommissionType(rule.getCommissionType());
        record.setCommissionRatePercent(rule.getCommissionRatePercent());
        record.setCommissionFixedCents(rule.getCommissionFixedCents());
        record.setCommissionAmountCents(commissionAmountCents);
        record.setCommissionStatus("PENDING");
        record.setCreatedAt(OffsetDateTime.now());

        ChannelCommissionRecordEntity saved = commissionRecordRepository.save(record);

        return new CommissionResultDto(saved.getId(), saved.getCommissionNo(), saved.getCommissionAmountCents());
    }

    private long calculateAmount(ChannelCommissionRuleEntity rule, long orderAmountCents) {
        if ("PERCENTAGE".equalsIgnoreCase(rule.getCommissionType())) {
            BigDecimal rate = rule.getCommissionRatePercent();
            if (rate == null) return 0;
            return BigDecimal.valueOf(orderAmountCents)
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValue();
        } else if ("FIXED".equalsIgnoreCase(rule.getCommissionType())) {
            return rule.getCommissionFixedCents() == null ? 0 : rule.getCommissionFixedCents();
        }
        return 0;
    }
}
