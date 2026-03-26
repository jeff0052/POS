package com.developer.pos.v2.agent.identity;

import com.developer.pos.v2.agent.wallet.AgentWalletEntity;
import com.developer.pos.v2.agent.wallet.JpaAgentWalletRepository;
import com.developer.pos.v2.common.application.UseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RestaurantAgentService implements UseCase {

    private final JpaRestaurantAgentRepository agentRepository;
    private final JpaAgentWalletRepository walletRepository;

    public RestaurantAgentService(JpaRestaurantAgentRepository agentRepository,
                                  JpaAgentWalletRepository walletRepository) {
        this.agentRepository = agentRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public RestaurantAgentEntity registerAgent(Long merchantId, Long storeId, String agentName,
                                                String cuisineType, String address, String operatingHours) {
        agentRepository.findByStoreId(storeId).ifPresent(existing -> {
            throw new IllegalStateException("Agent already registered for store: " + storeId);
        });

        RestaurantAgentEntity agent = new RestaurantAgentEntity();
        agent.setAgentId("AGT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        agent.setMerchantId(merchantId);
        agent.setStoreId(storeId);
        agent.setAgentName(agentName);
        agent.setAgentStatus("ACTIVE");
        agent.setCuisineType(cuisineType);
        agent.setAddress(address);
        agent.setOperatingHours(operatingHours);
        agent.setCreatedAt(OffsetDateTime.now());
        agent.setUpdatedAt(OffsetDateTime.now());
        RestaurantAgentEntity saved = agentRepository.save(agent);

        // Auto-create wallet
        AgentWalletEntity wallet = new AgentWalletEntity();
        wallet.setWalletId("WLT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        wallet.setAgentId(saved.getAgentId());
        wallet.setCurrencyCode("SGD");
        wallet.setWalletStatus("ACTIVE");
        wallet.setCreatedAt(OffsetDateTime.now());
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepository.save(wallet);

        return saved;
    }

    @Transactional(readOnly = true)
    public RestaurantAgentEntity getAgent(String agentId) {
        return agentRepository.findByAgentId(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
    }

    @Transactional(readOnly = true)
    public RestaurantAgentEntity getAgentByStore(Long storeId) {
        return agentRepository.findByStoreId(storeId)
                .orElseThrow(() -> new IllegalArgumentException("No agent for store: " + storeId));
    }

    @Transactional
    public RestaurantAgentEntity updateAgent(String agentId, String agentName, String cuisineType,
                                              String address, String operatingHours, String capabilitiesJson) {
        RestaurantAgentEntity agent = getAgent(agentId);
        if (agentName != null) agent.setAgentName(agentName);
        if (cuisineType != null) agent.setCuisineType(cuisineType);
        if (address != null) agent.setAddress(address);
        if (operatingHours != null) agent.setOperatingHours(operatingHours);
        if (capabilitiesJson != null) agent.setCapabilitiesJson(capabilitiesJson);
        agent.setUpdatedAt(OffsetDateTime.now());
        return agentRepository.save(agent);
    }
}
