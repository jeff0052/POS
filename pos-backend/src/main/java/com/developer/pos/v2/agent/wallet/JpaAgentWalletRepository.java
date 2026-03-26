package com.developer.pos.v2.agent.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaAgentWalletRepository extends JpaRepository<AgentWalletEntity, Long> {
    Optional<AgentWalletEntity> findByWalletId(String walletId);
    Optional<AgentWalletEntity> findByAgentId(String agentId);
}
