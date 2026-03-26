package com.developer.pos.v2.ai;

import com.developer.pos.v2.ai.advisor.AdvisorContext;
import com.developer.pos.v2.ai.advisor.AdvisorPromptBuilder;
import com.developer.pos.v2.ai.advisor.AdvisorRole;
import com.developer.pos.v2.ai.recommendation.AiRecommendationEntity;
import com.developer.pos.v2.ai.recommendation.JpaAiRecommendationRepository;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.mcp.model.ActionContext;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Operator — the brain that drives restaurant operations.
 *
 * Flow: Sense (read data via MCP tools) → Think (build prompt) → Propose (save recommendations)
 *       → Approve (owner reviews) → Act (execute via MCP tools)
 */
@Service
public class AiOperatorService implements UseCase {

    private final McpToolRegistry toolRegistry;
    private final JpaAiRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    public AiOperatorService(McpToolRegistry toolRegistry,
                             JpaAiRecommendationRepository recommendationRepository,
                             ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
    }

    // ── Sense: assemble context for an advisor ──

    public AdvisorContext assembleContext(Long merchantId, Long storeId, AdvisorRole role) {
        ActionContext ctx = new ActionContext(ActionContext.ActorType.AI, role.name(), ActionContext.DecisionSource.AI_AUTO, null, ActionContext.ApprovalStatus.NOT_REQUIRED, "Scheduled advisor check");

        Object salesData = callTool("report.daily_summary", Map.of("storeId", storeId), ctx);
        Object orderData = callTool("order.list_all", Map.of("storeId", storeId), ctx);
        Object catalogData = callTool("catalog.get_menu", Map.of("storeCode", "STORE" + storeId), ctx);
        Object promotionData = callTool("promotion.list_rules", Map.of("storeId", storeId), ctx);

        return new AdvisorContext(merchantId, storeId, role, salesData, orderData, null, promotionData, catalogData);
    }

    // ── Think: build prompt for LLM ──

    public String buildPrompt(AdvisorContext context) {
        return AdvisorPromptBuilder.build(context);
    }

    // ── Propose: save AI-generated recommendations ──

    @Transactional
    public AiRecommendationEntity propose(Long merchantId, Long storeId, AdvisorRole role,
                                           String title, String summary, String riskLevel,
                                           String proposedAction, Map<String, Object> proposedParams) {
        AiRecommendationEntity rec = new AiRecommendationEntity();
        rec.setRecommendationId("REC" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        rec.setMerchantId(merchantId);
        rec.setStoreId(storeId);
        rec.setAdvisorRole(role.name());
        rec.setTitle(title);
        rec.setSummary(summary);
        rec.setRiskLevel(riskLevel);
        rec.setStatus("PENDING");
        rec.setProposedAction(proposedAction);
        rec.setCreatedAt(OffsetDateTime.now());
        rec.setExpiresAt(OffsetDateTime.now().plusDays(7));

        if (proposedParams != null) {
            try {
                rec.setProposedParamsJson(objectMapper.writeValueAsString(proposedParams));
            } catch (Exception ignored) {}
        }

        // Low risk → auto-approve
        if ("LOW".equalsIgnoreCase(riskLevel)) {
            rec.setStatus("AUTO_APPROVED");
            rec.setApprovedBy("SYSTEM");
            rec.setApprovedAt(OffsetDateTime.now());
        }

        return recommendationRepository.save(rec);
    }

    // ── Approve: owner approves or rejects ──

    @Transactional
    public AiRecommendationEntity approve(String recommendationId, String approvedBy) {
        AiRecommendationEntity rec = recommendationRepository.findByRecommendationId(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));
        if (!"PENDING".equals(rec.getStatus())) {
            throw new IllegalStateException("Only PENDING recommendations can be approved. Current: " + rec.getStatus());
        }
        rec.setStatus("APPROVED");
        rec.setApprovedBy(approvedBy);
        rec.setApprovedAt(OffsetDateTime.now());
        return recommendationRepository.save(rec);
    }

    @Transactional
    public AiRecommendationEntity reject(String recommendationId, String reason) {
        AiRecommendationEntity rec = recommendationRepository.findByRecommendationId(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));
        if (!"PENDING".equals(rec.getStatus())) {
            throw new IllegalStateException("Only PENDING recommendations can be rejected. Current: " + rec.getStatus());
        }
        rec.setStatus("REJECTED");
        rec.setRejectedReason(reason);
        return recommendationRepository.save(rec);
    }

    // ── Act: execute an approved recommendation via MCP tool ──

    @Transactional
    public AiRecommendationEntity execute(String recommendationId) {
        AiRecommendationEntity rec = recommendationRepository.findByRecommendationId(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));
        if (!"APPROVED".equals(rec.getStatus()) && !"AUTO_APPROVED".equals(rec.getStatus())) {
            throw new IllegalStateException("Only approved recommendations can be executed. Current: " + rec.getStatus());
        }
        if (rec.getProposedAction() == null || rec.getProposedAction().isBlank()) {
            rec.setStatus("EXECUTED");
            rec.setExecutedAt(OffsetDateTime.now());
            rec.setExecutionResultJson("{\"result\": \"No action required — informational only\"}");
            return recommendationRepository.save(rec);
        }

        ActionContext ctx = new ActionContext(
                ActionContext.ActorType.AI, rec.getAdvisorRole(),
                ActionContext.DecisionSource.AI_RECOMMENDATION,
                rec.getRecommendationId(), ActionContext.ApprovalStatus.APPROVED, rec.getTitle());

        Map<String, Object> params = Map.of();
        if (rec.getProposedParamsJson() != null) {
            try {
                params = objectMapper.readValue(rec.getProposedParamsJson(), Map.class);
            } catch (Exception ignored) {}
        }

        Object result = callTool(rec.getProposedAction(), params, ctx);

        rec.setStatus("EXECUTED");
        rec.setExecutedAt(OffsetDateTime.now());
        try {
            rec.setExecutionResultJson(objectMapper.writeValueAsString(result));
        } catch (Exception ignored) {}

        return recommendationRepository.save(rec);
    }

    // ── Query ──

    @Transactional(readOnly = true)
    public List<AiRecommendationEntity> getPendingRecommendations(Long storeId) {
        return recommendationRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, "PENDING");
    }

    @Transactional(readOnly = true)
    public Page<AiRecommendationEntity> listRecommendations(Long storeId, int page, int size) {
        return recommendationRepository.findByStoreIdOrderByCreatedAtDesc(storeId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public AiRecommendationEntity getRecommendation(String recommendationId) {
        return recommendationRepository.findByRecommendationId(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + recommendationId));
    }

    // ── Internal ──

    private Object callTool(String toolName, Map<String, Object> params, ActionContext ctx) {
        return toolRegistry.getTool(toolName)
                .map(tool -> {
                    try {
                        return tool.handler().apply(params);
                    } catch (Exception e) {
                        return (Object) Map.of("error", e.getMessage());
                    }
                })
                .orElse(null);
    }
}
