package com.developer.pos.v2.channel.application.service;

import com.developer.pos.v2.channel.application.dto.ChannelSettlementBatchDto;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelCommissionRecordEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.entity.ChannelSettlementBatchEntity;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelCommissionRecordRepository;
import com.developer.pos.v2.channel.infrastructure.persistence.repository.JpaChannelSettlementBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelSettlementServiceTest {

    @Mock JpaChannelCommissionRecordRepository recordRepo;
    @Mock JpaChannelSettlementBatchRepository batchRepo;
    @InjectMocks ChannelSettlementService service;

    @Test
    void generateBatch_sumsCommissionsInPeriod() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        ChannelCommissionRecordEntity r1 = buildRecord(1L, 10000L, 1000L, "2026-03-10");
        ChannelCommissionRecordEntity r2 = buildRecord(2L, 20000L, 2000L, "2026-03-15");
        ChannelCommissionRecordEntity r3 = buildRecord(3L, 5000L, 500L, "2026-04-01"); // outside period

        when(recordRepo.findByChannelIdAndCommissionStatusOrderByCreatedAtDesc(eq(10L), eq("PENDING")))
                .thenReturn(List.of(r1, r2, r3));
        when(batchRepo.save(any())).thenAnswer(inv -> {
            ChannelSettlementBatchEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(recordRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChannelSettlementBatchDto result = service.generateBatch(10L, start, end);

        assertThat(result.totalOrders()).isEqualTo(2);
        assertThat(result.totalOrderAmountCents()).isEqualTo(30000L);
        assertThat(result.totalCommissionCents()).isEqualTo(3000L);
        assertThat(result.finalSettlementCents()).isEqualTo(3000L);
        assertThat(result.batchStatus()).isEqualTo("DRAFT");
        assertThat(result.batchNo()).startsWith("CSB");
    }

    private ChannelCommissionRecordEntity buildRecord(Long id, long orderCents, long commissionCents, String date) {
        ChannelCommissionRecordEntity r = new ChannelCommissionRecordEntity();
        r.setId(id);
        r.setChannelId(10L);
        r.setOrderAmountCents(orderCents);
        r.setCommissionAmountCents(commissionCents);
        r.setCommissionStatus("PENDING");
        r.setCreatedAt(OffsetDateTime.of(
                LocalDate.parse(date).atStartOfDay(), ZoneOffset.UTC
        ));
        return r;
    }
}
