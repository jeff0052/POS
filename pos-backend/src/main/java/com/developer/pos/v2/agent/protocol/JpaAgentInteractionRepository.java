package com.developer.pos.v2.agent.protocol;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaAgentInteractionRepository extends JpaRepository<AgentInteractionEntity, Long> {
    Optional<AgentInteractionEntity> findByInteractionId(String interactionId);
    List<AgentInteractionEntity> findByAgentIdAndStatusOrderByCreatedAtDesc(String agentId, String status);
    Page<AgentInteractionEntity> findByAgentIdOrderByCreatedAtDesc(String agentId, Pageable pageable);
}
