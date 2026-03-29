package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "V2BuffetPackageEntity")
@Table(name = "buffet_packages")
public class BuffetPackageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "package_code", nullable = false, length = 64)
    private String packageCode;

    @Column(name = "package_name", nullable = false, length = 255)
    private String packageName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "child_price_cents")
    private Long childPriceCents;

    @Column(name = "child_age_max")
    private Integer childAgeMax;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "warning_before_minutes", nullable = false)
    private int warningBeforeMinutes;

    @Column(name = "overtime_fee_per_minute_cents", nullable = false)
    private long overtimeFeePerMinuteCents;

    @Column(name = "overtime_grace_minutes", nullable = false)
    private int overtimeGraceMinutes;

    @Column(name = "max_overtime_minutes", nullable = false)
    private int maxOvertimeMinutes;

    @Column(name = "package_status", nullable = false, length = 32)
    private String packageStatus;

    @Column(name = "applicable_time_slots", columnDefinition = "JSON")
    private String applicableTimeSlots;

    @Column(name = "applicable_days", columnDefinition = "JSON")
    private String applicableDays;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    protected BuffetPackageEntity() {}

    public BuffetPackageEntity(Long storeId, String packageCode, String packageName,
                               String description, long priceCents, Long childPriceCents,
                               Integer childAgeMax, int durationMinutes, int warningBeforeMinutes,
                               long overtimeFeePerMinuteCents, int overtimeGraceMinutes,
                               int maxOvertimeMinutes, String packageStatus,
                               String applicableTimeSlots, String applicableDays,
                               int sortOrder, Long imageId, Long createdBy) {
        this.storeId = storeId;
        this.packageCode = packageCode;
        this.packageName = packageName;
        this.description = description;
        this.priceCents = priceCents;
        this.childPriceCents = childPriceCents;
        this.childAgeMax = childAgeMax;
        this.durationMinutes = durationMinutes;
        this.warningBeforeMinutes = warningBeforeMinutes;
        this.overtimeFeePerMinuteCents = overtimeFeePerMinuteCents;
        this.overtimeGraceMinutes = overtimeGraceMinutes;
        this.maxOvertimeMinutes = maxOvertimeMinutes;
        this.packageStatus = packageStatus;
        this.applicableTimeSlots = applicableTimeSlots;
        this.applicableDays = applicableDays;
        this.sortOrder = sortOrder;
        this.imageId = imageId;
        this.createdBy = createdBy;
    }

    public void update(String packageCode, String packageName, String description,
                       long priceCents, Long childPriceCents, Integer childAgeMax,
                       int durationMinutes, int warningBeforeMinutes,
                       long overtimeFeePerMinuteCents, int overtimeGraceMinutes,
                       int maxOvertimeMinutes, String packageStatus,
                       String applicableTimeSlots, String applicableDays,
                       int sortOrder, Long imageId, Long updatedBy) {
        this.packageCode = packageCode;
        this.packageName = packageName;
        this.description = description;
        this.priceCents = priceCents;
        this.childPriceCents = childPriceCents;
        this.childAgeMax = childAgeMax;
        this.durationMinutes = durationMinutes;
        this.warningBeforeMinutes = warningBeforeMinutes;
        this.overtimeFeePerMinuteCents = overtimeFeePerMinuteCents;
        this.overtimeGraceMinutes = overtimeGraceMinutes;
        this.maxOvertimeMinutes = maxOvertimeMinutes;
        this.packageStatus = packageStatus;
        this.applicableTimeSlots = applicableTimeSlots;
        this.applicableDays = applicableDays;
        this.sortOrder = sortOrder;
        this.imageId = imageId;
        this.updatedBy = updatedBy;
    }

    // Getters
    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public String getPackageCode() { return packageCode; }
    public String getPackageName() { return packageName; }
    public String getDescription() { return description; }
    public long getPriceCents() { return priceCents; }
    public Long getChildPriceCents() { return childPriceCents; }
    public Integer getChildAgeMax() { return childAgeMax; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getWarningBeforeMinutes() { return warningBeforeMinutes; }
    public long getOvertimeFeePerMinuteCents() { return overtimeFeePerMinuteCents; }
    public int getOvertimeGraceMinutes() { return overtimeGraceMinutes; }
    public int getMaxOvertimeMinutes() { return maxOvertimeMinutes; }
    public String getPackageStatus() { return packageStatus; }
    public String getApplicableTimeSlots() { return applicableTimeSlots; }
    public String getApplicableDays() { return applicableDays; }
    public int getSortOrder() { return sortOrder; }
    public Long getImageId() { return imageId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
}
