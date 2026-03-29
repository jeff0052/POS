package com.developer.pos.v2.audit.application.dto;

import java.time.OffsetDateTime;

public record AuditTrailDto(
        Long id,
        String trailNo,
        Long storeId,
        String actorType,
        Long actorId,
        String actorName,
        String action,
        String targetType,
        String targetId,
        String riskLevel,
        Boolean requiresApproval,
        String approvalStatus,
        Long approvedBy,
        OffsetDateTime approvedAt,
        String approvalNote,
        OffsetDateTime createdAt
) {}
