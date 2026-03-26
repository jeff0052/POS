package com.developer.pos.v2.agent.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaRestaurantAgentRepository extends JpaRepository<RestaurantAgentEntity, Long> {
    Optional<RestaurantAgentEntity> findByAgentId(String agentId);
    Optional<RestaurantAgentEntity> findByStoreId(Long storeId);
}
