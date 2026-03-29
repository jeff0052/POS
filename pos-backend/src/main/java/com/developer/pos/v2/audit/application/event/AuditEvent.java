package com.developer.pos.v2.audit.application.event;

/**
 * Immutable event published by AuditAspect, consumed asynchronously by AuditEventListener.
 * All data is pre-resolved in the request thread (actor, IP, snapshots) so the
 * async listener has no dependency on SecurityContext or RequestContext.
 */
public record AuditEvent(
        String trailNo,
        Long storeId,
        String actorType,
        Long actorId,
        String actorName,
        String action,
        String targetType,
        String targetId,
        String beforeSnapshot,
        String afterSnapshot,
        String riskLevel,
        boolean requiresApproval,
        String ipAddress
) {}
