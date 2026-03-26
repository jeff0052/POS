package com.developer.pos.v2.ai.recommendation;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_recommendations")
public class AiRecommendationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recommendation_id") private String recommendationId;
    @Column(name = "merchant_id") private Long merchantId;
    @Column(name = "store_id") private Long storeId;
    @Column(name = "advisor_role") private String advisorRole;
    @Column(name = "title") private String title;
    @Column(name = "summary", columnDefinition = "TEXT") private String summary;
    @Column(name = "detail_json", columnDefinition = "JSON") private String detailJson;
    @Column(name = "risk_level") private String riskLevel;
    @Column(name = "status") private String status;
    @Column(name = "proposed_action") private String proposedAction;
    @Column(name = "proposed_params_json", columnDefinition = "JSON") private String proposedParamsJson;
    @Column(name = "approved_by") private String approvedBy;
    @Column(name = "approved_at") private OffsetDateTime approvedAt;
    @Column(name = "rejected_reason", columnDefinition = "TEXT") private String rejectedReason;
    @Column(name = "executed_at") private OffsetDateTime executedAt;
    @Column(name = "execution_result_json", columnDefinition = "JSON") private String executionResultJson;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "expires_at") private OffsetDateTime expiresAt;

    public Long getId() { return id; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String v) { this.recommendationId = v; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long v) { this.merchantId = v; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { this.storeId = v; }
    public String getAdvisorRole() { return advisorRole; }
    public void setAdvisorRole(String v) { this.advisorRole = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getSummary() { return summary; }
    public void setSummary(String v) { this.summary = v; }
    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String v) { this.detailJson = v; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String v) { this.riskLevel = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getProposedAction() { return proposedAction; }
    public void setProposedAction(String v) { this.proposedAction = v; }
    public String getProposedParamsJson() { return proposedParamsJson; }
    public void setProposedParamsJson(String v) { this.proposedParamsJson = v; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String v) { this.approvedBy = v; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime v) { this.approvedAt = v; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String v) { this.rejectedReason = v; }
    public OffsetDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(OffsetDateTime v) { this.executedAt = v; }
    public String getExecutionResultJson() { return executionResultJson; }
    public void setExecutionResultJson(String v) { this.executionResultJson = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime v) { this.expiresAt = v; }
}
