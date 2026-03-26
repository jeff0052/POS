package com.developer.pos.v2.agent.identity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "restaurant_agents")
public class RestaurantAgentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "agent_id") private String agentId;
    @Column(name = "merchant_id") private Long merchantId;
    @Column(name = "store_id") private Long storeId;
    @Column(name = "agent_name") private String agentName;
    @Column(name = "agent_status") private String agentStatus;
    @Column(name = "cuisine_type") private String cuisineType;
    @Column(name = "address") private String address;
    @Column(name = "operating_hours") private String operatingHours;
    @Column(name = "capabilities_json", columnDefinition = "JSON") private String capabilitiesJson;
    @Column(name = "agent_config_json", columnDefinition = "JSON") private String agentConfigJson;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String v) { this.agentId = v; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long v) { this.merchantId = v; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { this.storeId = v; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String v) { this.agentName = v; }
    public String getAgentStatus() { return agentStatus; }
    public void setAgentStatus(String v) { this.agentStatus = v; }
    public String getCuisineType() { return cuisineType; }
    public void setCuisineType(String v) { this.cuisineType = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String v) { this.operatingHours = v; }
    public String getCapabilitiesJson() { return capabilitiesJson; }
    public void setCapabilitiesJson(String v) { this.capabilitiesJson = v; }
    public String getAgentConfigJson() { return agentConfigJson; }
    public void setAgentConfigJson(String v) { this.agentConfigJson = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
}
