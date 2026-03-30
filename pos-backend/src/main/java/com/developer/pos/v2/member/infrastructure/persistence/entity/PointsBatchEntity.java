package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity(name = "V2PointsBatchEntity")
@Table(name = "points_batches")
public class PointsBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "batch_no", nullable = false)
    private String batchNo;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "original_points", nullable = false)
    private long originalPoints;

    @Column(name = "remaining_points", nullable = false)
    private long remainingPoints;

    @Column(name = "used_points", nullable = false)
    private long usedPoints;

    @Column(name = "expired_points", nullable = false)
    private long expiredPoints;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "batch_status", nullable = false)
    private String batchStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PointsBatchEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public long getOriginalPoints() {
        return originalPoints;
    }

    public void setOriginalPoints(long originalPoints) {
        this.originalPoints = originalPoints;
    }

    public long getRemainingPoints() {
        return remainingPoints;
    }

    public void setRemainingPoints(long remainingPoints) {
        this.remainingPoints = remainingPoints;
    }

    public long getUsedPoints() {
        return usedPoints;
    }

    public void setUsedPoints(long usedPoints) {
        this.usedPoints = usedPoints;
    }

    public long getExpiredPoints() {
        return expiredPoints;
    }

    public void setExpiredPoints(long expiredPoints) {
        this.expiredPoints = expiredPoints;
    }

    public LocalDateTime getEarnedAt() {
        return earnedAt;
    }

    public void setEarnedAt(LocalDateTime earnedAt) {
        this.earnedAt = earnedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
