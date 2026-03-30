package com.developer.pos.v2.channel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity(name = "V2OrderChannelAttributionEntity")
@Table(name = "order_channel_attribution")
public class OrderChannelAttributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submitted_order_id", nullable = false)
    private Long submittedOrderId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "attribution_type")
    private String attributionType;

    @Column(name = "tracking_value")
    private String trackingValue;

    @Column(name = "landing_url")
    private String landingUrl;

    @Column(name = "first_touch_at")
    private OffsetDateTime firstTouchAt;

    @Column(name = "conversion_at")
    private OffsetDateTime conversionAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public OrderChannelAttributionEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSubmittedOrderId() { return submittedOrderId; }
    public void setSubmittedOrderId(Long submittedOrderId) { this.submittedOrderId = submittedOrderId; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public String getAttributionType() { return attributionType; }
    public void setAttributionType(String attributionType) { this.attributionType = attributionType; }
    public String getTrackingValue() { return trackingValue; }
    public void setTrackingValue(String trackingValue) { this.trackingValue = trackingValue; }
    public String getLandingUrl() { return landingUrl; }
    public void setLandingUrl(String landingUrl) { this.landingUrl = landingUrl; }
    public OffsetDateTime getFirstTouchAt() { return firstTouchAt; }
    public void setFirstTouchAt(OffsetDateTime firstTouchAt) { this.firstTouchAt = firstTouchAt; }
    public OffsetDateTime getConversionAt() { return conversionAt; }
    public void setConversionAt(OffsetDateTime conversionAt) { this.conversionAt = conversionAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
