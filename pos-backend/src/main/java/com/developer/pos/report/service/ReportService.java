package com.developer.pos.report.service;

import com.developer.pos.report.dto.DailySummaryDto;
import com.developer.pos.report.dto.SalesReportSummaryDto;
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

    public SalesReportSummaryDto salesSummary() {
        return new SalesReportSummaryDto(
            1268000L,
            86000L,
            422000L,
            70000L,
            4.6,
            1
        );
    }
}
