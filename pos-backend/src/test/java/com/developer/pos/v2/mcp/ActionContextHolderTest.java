package com.developer.pos.v2.mcp;

import com.developer.pos.v2.mcp.model.ActionContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionContextHolderTest {
    @Test
    void defaultContextIsHuman() {
        var holder = new ActionContextHolder();
        var ctx = holder.getContext();
        assertEquals(ActionContext.ActorType.HUMAN, ctx.actorType());
        assertEquals(ActionContext.DecisionSource.MANUAL, ctx.decisionSource());
    }

    @Test
    void canSetAiContext() {
        var holder = new ActionContextHolder();
        var aiCtx = new ActionContext(
            ActionContext.ActorType.AI, "menu-advisor",
            ActionContext.DecisionSource.AI_RECOMMENDATION,
            "rec-001", ActionContext.ApprovalStatus.APPROVED,
            "test reason"
        );
        holder.setContext(aiCtx);
        assertEquals(ActionContext.ActorType.AI, holder.getContext().actorType());
        assertEquals("menu-advisor", holder.getContext().actorId());
    }
}
