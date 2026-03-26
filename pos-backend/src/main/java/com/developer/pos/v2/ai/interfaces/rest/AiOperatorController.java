package com.developer.pos.v2.ai.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.ai.AiOperatorService;
import com.developer.pos.v2.ai.advisor.AdvisorContext;
import com.developer.pos.v2.ai.advisor.AdvisorRole;
import com.developer.pos.v2.ai.recommendation.AiRecommendationEntity;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/ai")
public class AiOperatorController implements V2Api {

    private final AiOperatorService aiOperatorService;

    public AiOperatorController(AiOperatorService aiOperatorService) {
        this.aiOperatorService = aiOperatorService;
    }

    /** Get available advisor roles */
    @GetMapping("/advisors")
    public ApiResponse<List<Map<String, String>>> listAdvisors() {
        List<Map<String, String>> advisors = java.util.Arrays.stream(AdvisorRole.values())
                .map(role -> Map.of(
                        "role", role.name(),
                        "displayName", role.getDisplayName(),
                        "description", role.getDescription()
                )).toList();
        return ApiResponse.success(advisors);
    }

    /** Trigger an advisor check — assembles context and returns the LLM prompt */
    @PostMapping("/advisors/{role}/check")
    public ApiResponse<Map<String, Object>> triggerAdvisorCheck(
            @PathVariable String role,
            @RequestParam Long merchantId,
            @RequestParam Long storeId) {
        AdvisorRole advisorRole = AdvisorRole.valueOf(role.toUpperCase());
        AdvisorContext context = aiOperatorService.assembleContext(merchantId, storeId, advisorRole);
        String prompt = aiOperatorService.buildPrompt(context);
        return ApiResponse.success(Map.of(
                "advisorRole", advisorRole.name(),
                "prompt", prompt,
                "instruction", "Send this prompt to an LLM. Parse the JSON response and call POST /api/v2/ai/recommendations to save each suggestion."
        ));
    }

    /** Save a recommendation (from LLM response parsing) */
    @PostMapping("/recommendations")
    public ApiResponse<AiRecommendationEntity> createRecommendation(@RequestBody CreateRecommendationRequest request) {
        AiRecommendationEntity rec = aiOperatorService.propose(
                request.merchantId(), request.storeId(),
                AdvisorRole.valueOf(request.advisorRole().toUpperCase()),
                request.title(), request.summary(), request.riskLevel(),
                request.proposedAction(), request.proposedParams()
        );
        return ApiResponse.success(rec);
    }

    /** List pending recommendations for owner to review */
    @GetMapping("/recommendations/pending")
    public ApiResponse<List<AiRecommendationEntity>> getPending(@RequestParam Long storeId) {
        return ApiResponse.success(aiOperatorService.getPendingRecommendations(storeId));
    }

    /** List all recommendations with pagination */
    @GetMapping("/recommendations")
    public ApiResponse<Page<AiRecommendationEntity>> listAll(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(aiOperatorService.listRecommendations(storeId, page, size));
    }

    /** Get single recommendation detail */
    @GetMapping("/recommendations/{recommendationId}")
    public ApiResponse<AiRecommendationEntity> getDetail(@PathVariable String recommendationId) {
        return ApiResponse.success(aiOperatorService.getRecommendation(recommendationId));
    }

    /** Owner approves a recommendation */
    @PostMapping("/recommendations/{recommendationId}/approve")
    public ApiResponse<AiRecommendationEntity> approve(
            @PathVariable String recommendationId,
            @RequestBody ApproveRequest request) {
        return ApiResponse.success(aiOperatorService.approve(recommendationId, request.approvedBy()));
    }

    /** Owner rejects a recommendation */
    @PostMapping("/recommendations/{recommendationId}/reject")
    public ApiResponse<AiRecommendationEntity> reject(
            @PathVariable String recommendationId,
            @RequestBody RejectRequest request) {
        return ApiResponse.success(aiOperatorService.reject(recommendationId, request.reason()));
    }

    /** Execute an approved recommendation */
    @PostMapping("/recommendations/{recommendationId}/execute")
    public ApiResponse<AiRecommendationEntity> execute(@PathVariable String recommendationId) {
        return ApiResponse.success(aiOperatorService.execute(recommendationId));
    }

    public record CreateRecommendationRequest(
            Long merchantId, Long storeId, String advisorRole,
            String title, String summary, String riskLevel,
            String proposedAction, Map<String, Object> proposedParams) {}
    public record ApproveRequest(String approvedBy) {}
    public record RejectRequest(String reason) {}
}
