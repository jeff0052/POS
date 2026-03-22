package com.developer.pos.report.service;

import com.developer.pos.report.dto.DailySummaryDto;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    public DailySummaryDto dailySummary() {
        return new DailySummaryDto(
            "2026-03-20",
            1268000L,
            128,
            32000L,
            210000L,
            1058000L
        );
    }
}
