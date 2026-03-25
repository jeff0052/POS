package com.developer.pos.v2.mcp;

import com.developer.pos.v2.common.entity.BaseAuditableEntity;
import com.developer.pos.v2.mcp.model.ActionContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionContextAuditListenerTest {
    static class TestEntity extends BaseAuditableEntity {}

    @Test
    void setsAuditFieldsFromContext() {
        var holder = new ActionContextHolder();
        holder.setContext(new ActionContext(
            ActionContext.ActorType.AI, "promo-advisor",
            ActionContext.DecisionSource.AI_RECOMMENDATION,
            "rec-001", ActionContext.ApprovalStatus.APPROVED, "test reason"
        ));
        var listener = new ActionContextAuditListener();
        listener.setContextHolder(holder);
        var entity = new TestEntity();
        listener.setAuditFields(entity);
        assertEquals("AI", entity.getActorType());
        assertEquals("promo-advisor", entity.getActorId());
        assertEquals("AI_RECOMMENDATION", entity.getDecisionSource());
        assertEquals("test reason", entity.getChangeReason());
    }

    @Test
    void defaultsToHumanWhenNoContextSet() {
        var holder = new ActionContextHolder();
        var listener = new ActionContextAuditListener();
        listener.setContextHolder(holder);
        var entity = new TestEntity();
        listener.setAuditFields(entity);
        assertEquals("HUMAN", entity.getActorType());
        assertEquals("MANUAL", entity.getDecisionSource());
    }
}
