package com.developer.pos.v2.store.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "table_merge_records")
public class TableMergeRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "master_table_id", nullable = false)
    private Long masterTableId;

    @Column(name = "master_session_id", nullable = false)
    private Long masterSessionId;

    @Column(name = "merged_table_id", nullable = false)
    private Long mergedTableId;

    @Column(name = "merged_session_id", nullable = false)
    private Long mergedSessionId;

    @Column(name = "merged_at", insertable = false, updatable = false)
    private OffsetDateTime mergedAt;

    @Column(name = "unmerged_at")
    private OffsetDateTime unmergedAt;

    @Column(name = "unmerged_by")
    private Long unmergedBy;

    @Column(name = "merge_status", nullable = false)
    private String mergeStatus;

    protected TableMergeRecordEntity() {
    }

    public TableMergeRecordEntity(Long storeId, Long masterTableId, Long masterSessionId,
                                   Long mergedTableId, Long mergedSessionId) {
        this.storeId = storeId;
        this.masterTableId = masterTableId;
        this.masterSessionId = masterSessionId;
        this.mergedTableId = mergedTableId;
        this.mergedSessionId = mergedSessionId;
        this.mergeStatus = "ACTIVE";
    }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public Long getMasterTableId() { return masterTableId; }
    public Long getMasterSessionId() { return masterSessionId; }
    public Long getMergedTableId() { return mergedTableId; }
    public Long getMergedSessionId() { return mergedSessionId; }
    public OffsetDateTime getMergedAt() { return mergedAt; }
    public OffsetDateTime getUnmergedAt() { return unmergedAt; }
    public Long getUnmergedBy() { return unmergedBy; }
    public String getMergeStatus() { return mergeStatus; }

    public void setMergeStatus(String mergeStatus) { this.mergeStatus = mergeStatus; }
    public void setUnmergedAt(OffsetDateTime unmergedAt) { this.unmergedAt = unmergedAt; }
    public void setUnmergedBy(Long unmergedBy) { this.unmergedBy = unmergedBy; }
}
