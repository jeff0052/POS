package com.developer.pos.v2.agent.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.agent.identity.RestaurantAgentEntity;
import com.developer.pos.v2.agent.identity.RestaurantAgentService;
import com.developer.pos.v2.agent.protocol.AgentInteractionEntity;
import com.developer.pos.v2.agent.protocol.AgentInteractionService;
import com.developer.pos.v2.agent.wallet.AgentWalletEntity;
import com.developer.pos.v2.agent.wallet.AgentWalletService;
import com.developer.pos.v2.agent.wallet.WalletTransactionEntity;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/agents")
public class AgentController implements V2Api {

    private final RestaurantAgentService agentService;
    private final AgentWalletService walletService;
    private final AgentInteractionService interactionService;

    public AgentController(RestaurantAgentService agentService,
                           AgentWalletService walletService,
                           AgentInteractionService interactionService) {
        this.agentService = agentService;
        this.walletService = walletService;
        this.interactionService = interactionService;
    }

    // ── Agent Identity ──

    @PostMapping("/register")
    public ApiResponse<RestaurantAgentEntity> register(@RequestBody RegisterRequest req) {
        return ApiResponse.success(agentService.registerAgent(
                req.merchantId(), req.storeId(), req.agentName(),
                req.cuisineType(), req.address(), req.operatingHours()));
    }

    @GetMapping("/{agentId}")
    public ApiResponse<RestaurantAgentEntity> getAgent(@PathVariable String agentId) {
        return ApiResponse.success(agentService.getAgent(agentId));
    }

    @GetMapping("/by-store/{storeId}")
    public ApiResponse<RestaurantAgentEntity> getByStore(@PathVariable Long storeId) {
        return ApiResponse.success(agentService.getAgentByStore(storeId));
    }

    @PutMapping("/{agentId}")
    public ApiResponse<RestaurantAgentEntity> update(@PathVariable String agentId, @RequestBody UpdateAgentRequest req) {
        return ApiResponse.success(agentService.updateAgent(
                agentId, req.agentName(), req.cuisineType(), req.address(), req.operatingHours(), req.capabilitiesJson()));
    }

    // ── Wallet ──

    @GetMapping("/{agentId}/wallet")
    public ApiResponse<AgentWalletEntity> getWallet(@PathVariable String agentId) {
        return ApiResponse.success(walletService.getWallet(agentId));
    }

    @GetMapping("/{agentId}/wallet/transactions")
    public ApiResponse<Page<WalletTransactionEntity>> getTransactions(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(walletService.getTransactions(agentId, page, size));
    }

    @PostMapping("/{agentId}/wallet/income")
    public ApiResponse<WalletTransactionEntity> recordIncome(@PathVariable String agentId, @RequestBody WalletTxRequest req) {
        return ApiResponse.success(walletService.recordIncome(
                agentId, req.amountCents(), req.sourceType(), req.sourceRef(), req.counterparty(), req.description()));
    }

    @PostMapping("/{agentId}/wallet/expense")
    public ApiResponse<WalletTransactionEntity> recordExpense(@PathVariable String agentId, @RequestBody WalletTxRequest req) {
        return ApiResponse.success(walletService.recordExpense(
                agentId, req.amountCents(), req.sourceType(), req.sourceRef(), req.counterparty(), req.description()));
    }

    // ── Interactions ──

    @PostMapping("/{agentId}/interactions")
    public ApiResponse<AgentInteractionEntity> receiveInteraction(
            @PathVariable String agentId, @RequestBody InteractionRequest req) {
        return ApiResponse.success(interactionService.receiveRequest(
                agentId, req.interactionType(), req.requesterAgentId(),
                req.requestSummary(), req.requestDetailJson(), req.riskLevel()));
    }

    @GetMapping("/{agentId}/interactions/pending")
    public ApiResponse<List<AgentInteractionEntity>> getPending(@PathVariable String agentId) {
        return ApiResponse.success(interactionService.getPendingInteractions(agentId));
    }

    @GetMapping("/{agentId}/interactions")
    public ApiResponse<Page<AgentInteractionEntity>> listInteractions(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(interactionService.listInteractions(agentId, page, size));
    }

    @PostMapping("/{agentId}/interactions/{interactionId}/respond")
    public ApiResponse<AgentInteractionEntity> respond(
            @PathVariable String agentId,
            @PathVariable String interactionId,
            @RequestBody RespondRequest req) {
        return ApiResponse.success(interactionService.respond(
                interactionId, req.responseSummary(), req.responseDetailJson(), req.handledBy()));
    }

    // ── Request Records ──

    public record RegisterRequest(Long merchantId, Long storeId, String agentName,
                                  String cuisineType, String address, String operatingHours) {}
    public record UpdateAgentRequest(String agentName, String cuisineType, String address,
                                     String operatingHours, String capabilitiesJson) {}
    public record WalletTxRequest(long amountCents, String sourceType, String sourceRef,
                                  String counterparty, String description) {}
    public record InteractionRequest(String interactionType, String requesterAgentId,
                                     String requestSummary, String requestDetailJson, String riskLevel) {}
    public record RespondRequest(String responseSummary, String responseDetailJson, String handledBy) {}
}
