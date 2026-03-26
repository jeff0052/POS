package com.developer.pos.v2.report.application.dto;

import java.util.List;

public record MemberConsumptionReportDto(
        MemberConsumptionOverviewDto overview,
        List<TopMemberConsumptionDto> topMembers
) {
}
