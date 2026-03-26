package com.developer.pos.v2.mcp.core;

/**
 * Tracks who initiated an action and why.
 * Every MCP tool call carries this context for audit.
 */
public record ActionContext(
    String actorType,       // HUMAN | AI
    String actorId,         // "jeff" | "menu-advisor" | "cashier-001"
    String decisionSource,  // MANUAL | AI_RECOMMENDATION | AI_AUTO
    String recommendationId,// nullable — links to AI suggestion
    String changeReason     // "周三客流低，建议满减"
) {
    public static ActionContext human(String actorId, String reason) {
        return new ActionContext("HUMAN", actorId, "MANUAL", null, reason);
    }

    public static ActionContext ai(String actorId, String decisionSource, String recommendationId, String reason) {
        return new ActionContext("AI", actorId, decisionSource, recommendationId, reason);
    }

    public static ActionContext system() {
        return new ActionContext("SYSTEM", "mcp-server", "AI_AUTO", null, null);
    }
}
