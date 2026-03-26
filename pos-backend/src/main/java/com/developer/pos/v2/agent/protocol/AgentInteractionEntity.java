package com.developer.pos.v2.agent.protocol;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "agent_interactions")
public class AgentInteractionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "interaction_id") private String interactionId;
    @Column(name = "agent_id") private String agentId;
    @Column(name = "direction") private String direction;
    @Column(name = "interaction_type") private String interactionType;
    @Column(name = "requester_agent_id") private String requesterAgentId;
    @Column(name = "request_summary", columnDefinition = "TEXT") private String requestSummary;
    @Column(name = "request_detail_json", columnDefinition = "JSON") private String requestDetailJson;
    @Column(name = "response_summary", columnDefinition = "TEXT") private String responseSummary;
    @Column(name = "response_detail_json", columnDefinition = "JSON") private String responseDetailJson;
    @Column(name = "risk_level") private String riskLevel;
    @Column(name = "status") private String status;
    @Column(name = "auto_handled") private boolean autoHandled;
    @Column(name = "handled_by") private String handledBy;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "responded_at") private OffsetDateTime respondedAt;

    public Long getId() { return id; }
    public String getInteractionId() { return interactionId; }
    public void setInteractionId(String v) { this.interactionId = v; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String v) { this.agentId = v; }
    public String getDirection() { return direction; }
    public void setDirection(String v) { this.direction = v; }
    public String getInteractionType() { return interactionType; }
    public void setInteractionType(String v) { this.interactionType = v; }
    public String getRequesterAgentId() { return requesterAgentId; }
    public void setRequesterAgentId(String v) { this.requesterAgentId = v; }
    public String getRequestSummary() { return requestSummary; }
    public void setRequestSummary(String v) { this.requestSummary = v; }
    public String getRequestDetailJson() { return requestDetailJson; }
    public void setRequestDetailJson(String v) { this.requestDetailJson = v; }
    public String getResponseSummary() { return responseSummary; }
    public void setResponseSummary(String v) { this.responseSummary = v; }
    public String getResponseDetailJson() { return responseDetailJson; }
    public void setResponseDetailJson(String v) { this.responseDetailJson = v; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String v) { this.riskLevel = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isAutoHandled() { return autoHandled; }
    public void setAutoHandled(boolean v) { this.autoHandled = v; }
    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String v) { this.handledBy = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(OffsetDateTime v) { this.respondedAt = v; }
}
