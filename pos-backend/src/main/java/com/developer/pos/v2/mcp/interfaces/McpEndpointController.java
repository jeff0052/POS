package com.developer.pos.v2.mcp.interfaces;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.mcp.ActionContextHolder;
import com.developer.pos.v2.mcp.ActionLogService;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.mcp.model.ActionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/mcp")
public class McpEndpointController implements V2Api {

    record ToolInfo(String name, String description, String domain, String category, String riskLevel) {}
    record ToolExecuteRequest(Map<String, Object> params, ActionContext context) {}

    private final McpToolRegistry registry;
    private final ActionContextHolder contextHolder;
    private final ActionLogService actionLogService;
    private final ObjectMapper objectMapper;

    public McpEndpointController(McpToolRegistry registry,
                                 ActionContextHolder contextHolder,
                                 ActionLogService actionLogService,
                                 ObjectMapper objectMapper) {
        this.registry = registry;
        this.contextHolder = contextHolder;
        this.actionLogService = actionLogService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/tools")
    public ApiResponse<List<ToolInfo>> listTools(@RequestParam(required = false) String domain) {
        List<McpToolRegistry.ToolDefinition> defs = (domain != null && !domain.isBlank())
                ? registry.listByDomain(domain)
                : registry.listTools();

        List<ToolInfo> tools = defs.stream()
                .map(d -> new ToolInfo(
                        d.name(),
                        d.description(),
                        d.domain(),
                        d.category(),
                        d.riskLevel() != null ? d.riskLevel().name() : null))
                .toList();

        return ApiResponse.success(tools);
    }

    @PostMapping("/tools/{toolName}/execute")
    public ApiResponse<Object> executeTool(@PathVariable String toolName,
                                           @RequestBody ToolExecuteRequest request) {
        var toolOpt = registry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            return new ApiResponse<>(404, "Tool not found: " + toolName, null);
        }

        McpToolRegistry.ToolDefinition tool = toolOpt.get();

        // Override request-scoped context if provided in body
        if (request.context() != null) {
            contextHolder.setContext(request.context());
        }

        ActionContext ctx = contextHolder.getContext();
        Map<String, Object> params = request.params() != null ? request.params() : Map.of();

        try {
            Object result = tool.handler().apply(params);
            actionLogService.log(toolName, ctx, tool.riskLevel() != null ? tool.riskLevel()
                    : com.developer.pos.v2.mcp.model.RiskLevel.LOW, params, result);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return new ApiResponse<>(500, "Tool execution failed: " + e.getMessage(), null);
        }
    }
}
