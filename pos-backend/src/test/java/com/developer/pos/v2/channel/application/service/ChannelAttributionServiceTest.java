package com.developer.pos.v2.channel.application.service;

import com.developer.pos.v2.channel.application.dto.CommissionResultDto;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelCommissionRecordEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelCommissionRuleEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.OrderChannelAttributionEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelCommissionRecordRepository;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelCommissionRuleRepository;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaOrderChannelAttributionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelAttributionServiceTest {

    @Mock JpaOrderChannelAttributionRepository attributionRepo;
    @Mock JpaChannelCommissionRuleRepository ruleRepo;
    @Mock JpaChannelCommissionRecordRepository recordRepo;
    @InjectMocks ChannelAttributionService service;

    @Test
    void attributeOrder_createsAttribution() {
        when(attributionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.attributeOrder(100L, 10L, "utm_abc");

        ArgumentCaptor<OrderChannelAttributionEntity> captor =
                ArgumentCaptor.forClass(OrderChannelAttributionEntity.class);
        verify(attributionRepo).save(captor.capture());

        OrderChannelAttributionEntity saved = captor.getValue();
        assertThat(saved.getSubmittedOrderId()).isEqualTo(100L);
        assertThat(saved.getChannelId()).isEqualTo(10L);
        assertThat(saved.getTrackingValue()).isEqualTo("utm_abc");
        assertThat(saved.getAttributionType()).isEqualTo("TRACKING");
    }

    @Test
    void calculateCommission_percentage() {
        ChannelCommissionRuleEntity rule = buildRule(1L, "PERCENTAGE", new BigDecimal("10.0000"), null, null, null);
        when(ruleRepo.findByChannelIdAndRuleStatusOrderByIdDesc(eq(10L), eq("ACTIVE")))
                .thenReturn(List.of(rule));
        when(recordRepo.save(any())).thenAnswer(inv -> {
            ChannelCommissionRecordEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        CommissionResultDto result = service.calculateCommission(10L, 100L, 1L, 10000L);

        assertThat(result.commissionAmountCents()).isEqualTo(1000L); // 10% of 10000
        assertThat(result.commissionNo()).startsWith("COM");
    }

    @Test
    void calculateCommission_fixed() {
        ChannelCommissionRuleEntity rule = buildRule(2L, "FIXED", null, 500L, null, null);
        when(ruleRepo.findByChannelIdAndRuleStatusOrderByIdDesc(eq(10L), eq("ACTIVE")))
                .thenReturn(List.of(rule));
        when(recordRepo.save(any())).thenAnswer(inv -> {
            ChannelCommissionRecordEntity e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        CommissionResultDto result = service.calculateCommission(10L, 101L, 1L, 8000L);

        assertThat(result.commissionAmountCents()).isEqualTo(500L);
    }

    @Test
    void calculateCommission_minCap() {
        ChannelCommissionRuleEntity rule = buildRule(3L, "PERCENTAGE", new BigDecimal("1.0000"), null, 200L, null);
        when(ruleRepo.findByChannelIdAndRuleStatusOrderByIdDesc(eq(10L), eq("ACTIVE")))
                .thenReturn(List.of(rule));
        when(recordRepo.save(any())).thenAnswer(inv -> {
            ChannelCommissionRecordEntity e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });

        // 1% of 5000 = 50, but min is 200
        CommissionResultDto result = service.calculateCommission(10L, 102L, 1L, 5000L);

        assertThat(result.commissionAmountCents()).isEqualTo(200L);
    }

    @Test
    void calculateCommission_maxCap() {
        ChannelCommissionRuleEntity rule = buildRule(4L, "PERCENTAGE", new BigDecimal("50.0000"), null, null, 3000L);
        when(ruleRepo.findByChannelIdAndRuleStatusOrderByIdDesc(eq(10L), eq("ACTIVE")))
                .thenReturn(List.of(rule));
        when(recordRepo.save(any())).thenAnswer(inv -> {
            ChannelCommissionRecordEntity e = inv.getArgument(0);
            e.setId(4L);
            return e;
        });

        // 50% of 10000 = 5000, but max is 3000
        CommissionResultDto result = service.calculateCommission(10L, 103L, 1L, 10000L);

        assertThat(result.commissionAmountCents()).isEqualTo(3000L);
    }

    private ChannelCommissionRuleEntity buildRule(Long id, String type, BigDecimal rate,
                                                  Long fixedCents, Long minCents, Long maxCents) {
        ChannelCommissionRuleEntity rule = new ChannelCommissionRuleEntity();
        rule.setId(id);
        rule.setCommissionType(type);
        rule.setCommissionRatePercent(rate);
        rule.setCommissionFixedCents(fixedCents);
        rule.setMinCommissionCents(minCents);
        rule.setMaxCommissionCents(maxCents);
        rule.setRuleStatus("ACTIVE");
        return rule;
    }
}
