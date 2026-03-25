package com.developer.pos.v2.mcp;

import com.developer.pos.v2.common.entity.BaseAuditableEntity;
import com.developer.pos.v2.mcp.model.ActionContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActionContextAuditListener {
    private static ActionContextHolder contextHolder;

    @Autowired
    public void setContextHolder(ActionContextHolder holder) {
        ActionContextAuditListener.contextHolder = holder;
    }

    @PrePersist
    @PreUpdate
    public void setAuditFields(Object entity) {
        if (!(entity instanceof BaseAuditableEntity auditable)) return;
        if (contextHolder == null) return;
        ActionContext ctx = contextHolder.getContext();
        auditable.setActorType(ctx.actorType().name());
        auditable.setActorId(ctx.actorId());
        auditable.setDecisionSource(ctx.decisionSource().name());
        auditable.setChangeReason(ctx.reason());
    }
}
