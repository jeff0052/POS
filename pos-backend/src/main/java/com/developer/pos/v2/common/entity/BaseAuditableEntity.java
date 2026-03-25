package com.developer.pos.v2.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseAuditableEntity {
    @Column(name = "actor_type")
    private String actorType;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "decision_source")
    private String decisionSource;

    @Column(name = "change_reason")
    private String changeReason;

    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getDecisionSource() { return decisionSource; }
    public void setDecisionSource(String decisionSource) { this.decisionSource = decisionSource; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}
