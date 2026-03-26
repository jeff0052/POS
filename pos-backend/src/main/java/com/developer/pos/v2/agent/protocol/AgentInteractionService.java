package com.developer.pos.v2.agent.protocol;

import com.developer.pos.v2.common.application.UseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles external agent interactions — reservations, venue inquiries, supplier quotes.
 * Flow: receive request → AI analyzes → auto-respond or escalate to owner.
 */
@Service
public class AgentInteractionService implements UseCase {

    private final JpaAgentInteractionRepository interactionRepository;

    public AgentInteractionService(JpaAgentInteractionRepository interactionRepository) {
        this.interactionRepository = interactionRepository;
    }

    @Transactional
    public AgentInteractionEntity receiveRequest(String agentId, String interactionType,
                                                  String requesterAgentId, String requestSummary,
                                                  String requestDetailJson, String riskLevel) {
        AgentInteractionEntity interaction = new AgentInteractionEntity();
        interaction.setInteractionId("INT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        interaction.setAgentId(agentId);
        interaction.setDirection("INBOUND");
        interaction.setInteractionType(interactionType);
        interaction.setRequesterAgentId(requesterAgentId);
        interaction.setRequestSummary(requestSummary);
        interaction.setRequestDetailJson(requestDetailJson);
        interaction.setRiskLevel(riskLevel != null ? riskLevel : "MEDIUM");
        interaction.setStatus("PENDING");
        interaction.setCreatedAt(OffsetDateTime.now());

        // Low risk → auto-handle
        if ("LOW".equalsIgnoreCase(riskLevel)) {
            interaction.setStatus("AUTO_PENDING");
            interaction.setAutoHandled(true);
        }

        return interactionRepository.save(interaction);
    }

    @Transactional
    public AgentInteractionEntity respond(String interactionId, String responseSummary,
                                           String responseDetailJson, String handledBy) {
        AgentInteractionEntity interaction = interactionRepository.findByInteractionId(interactionId)
                .orElseThrow(() -> new IllegalArgumentException("Interaction not found: " + interactionId));
        interaction.setResponseSummary(responseSummary);
        interaction.setResponseDetailJson(responseDetailJson);
        interaction.setStatus("RESPONDED");
        interaction.setHandledBy(handledBy);
        interaction.setRespondedAt(OffsetDateTime.now());
        return interactionRepository.save(interaction);
    }

    @Transactional(readOnly = true)
    public List<AgentInteractionEntity> getPendingInteractions(String agentId) {
        return interactionRepository.findByAgentIdAndStatusOrderByCreatedAtDesc(agentId, "PENDING");
    }

    @Transactional(readOnly = true)
    public Page<AgentInteractionEntity> listInteractions(String agentId, int page, int size) {
        return interactionRepository.findByAgentIdOrderByCreatedAtDesc(agentId, PageRequest.of(page, size));
    }
}
