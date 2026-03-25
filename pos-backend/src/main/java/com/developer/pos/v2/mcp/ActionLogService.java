package com.developer.pos.v2.mcp;

import com.developer.pos.v2.mcp.infrastructure.ActionLogEntity;
import com.developer.pos.v2.mcp.infrastructure.JpaActionLogRepository;
import com.developer.pos.v2.mcp.model.ActionContext;
import com.developer.pos.v2.mcp.model.RiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionLogService {
    private final JpaActionLogRepository repository;
    private final ObjectMapper objectMapper;

    public ActionLogService(JpaActionLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void log(String toolName, ActionContext context, RiskLevel riskLevel,
                    Object params, Object result) {
        var entity = new ActionLogEntity();
        entity.setToolName(toolName);
        entity.setActorType(context.actorType().name());
        entity.setActorId(context.actorId());
        entity.setDecisionSource(context.decisionSource().name());
        entity.setRecommendationId(context.recommendationId());
        entity.setApprovalStatus(
            context.approvalStatus() != null ? context.approvalStatus().name() : null);
        entity.setRiskLevel(riskLevel.name());
        entity.setChangeReason(context.reason());
        try {
            entity.setParamsJson(objectMapper.writeValueAsString(params));
            entity.setResultJson(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            entity.setParamsJson("{}");
            entity.setResultJson("{}");
        }
        repository.save(entity);
    }
}
