package com.developer.pos.v2.mcp.model;

public record ActionContext(
    ActorType actorType,
    String actorId,
    DecisionSource decisionSource,
    String recommendationId,
    ApprovalStatus approvalStatus,
    String reason
) {
    public enum ActorType { HUMAN, AI, EXTERNAL_AGENT }
    public enum DecisionSource { MANUAL, AI_RECOMMENDATION, AI_AUTO }
    public enum ApprovalStatus { APPROVED, PENDING, REJECTED, NOT_REQUIRED }

    public static ActionContext humanDefault() {
        return new ActionContext(
            ActorType.HUMAN, null, DecisionSource.MANUAL,
            null, ApprovalStatus.NOT_REQUIRED, null
        );
    }
}
