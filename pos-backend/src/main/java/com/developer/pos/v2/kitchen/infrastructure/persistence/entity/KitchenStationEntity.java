package com.developer.pos.v2.kitchen.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "V2KitchenStationEntity")
@Table(name = "kitchen_stations")
public class KitchenStationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "station_code", nullable = false, length = 64)
    private String stationCode;

    @Column(name = "station_name", nullable = false, length = 128)
    private String stationName;

    @Column(name = "station_type", length = 32)
    private String stationType = "KITCHEN";

    @Column(name = "printer_ip", length = 64)
    private String printerIp;

    @Column(name = "fallback_printer_ip", length = 64)
    private String fallbackPrinterIp;

    @Column(name = "fallback_mode", length = 32)
    private String fallbackMode = "AUTO";

    @Column(name = "kds_health_status", length = 32)
    private String kdsHealthStatus = "ONLINE";

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "kds_display_id", length = 64)
    private String kdsDisplayId;

    @Column(name = "station_status", nullable = false, length = 32)
    private String stationStatus = "ACTIVE";

    @Column(name = "sort_order")
    private int sortOrder = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected KitchenStationEntity() {}

    public KitchenStationEntity(Long storeId, String stationCode, String stationName, int sortOrder) {
        this.storeId = storeId;
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.sortOrder = sortOrder;
    }

    public boolean isOnline() {
        return "ONLINE".equals(kdsHealthStatus);
    }

    public boolean isHeartbeatExpired(int timeoutSeconds) {
        if (lastHeartbeatAt == null) return false;
        return lastHeartbeatAt.plusSeconds(timeoutSeconds).isBefore(LocalDateTime.now());
    }

    public void markOffline() { this.kdsHealthStatus = "OFFLINE"; }
    public void markOnline()  { this.kdsHealthStatus = "ONLINE"; }

    // Getters
    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public String getStationCode() { return stationCode; }
    public String getStationName() { return stationName; }
    public String getStationType() { return stationType; }
    public String getPrinterIp() { return printerIp; }
    public void setPrinterIp(String printerIp) { this.printerIp = printerIp; }
    public String getFallbackPrinterIp() { return fallbackPrinterIp; }
    public void setFallbackPrinterIp(String fallbackPrinterIp) { this.fallbackPrinterIp = fallbackPrinterIp; }
    public String getFallbackMode() { return fallbackMode; }
    public void setFallbackMode(String fallbackMode) { this.fallbackMode = fallbackMode; }
    public String getKdsHealthStatus() { return kdsHealthStatus; }
    public LocalDateTime getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }
    public String getStationStatus() { return stationStatus; }
    public void setStationStatus(String stationStatus) { this.stationStatus = stationStatus; }
    public int getSortOrder() { return sortOrder; }
}
