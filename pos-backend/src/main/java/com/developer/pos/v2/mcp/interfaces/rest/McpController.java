package com.developer.pos.v2.mcp.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.mcp.core.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool Server endpoint.
 * AI agents call POST /api/v2/mcp/execute to invoke tools.
 * GET /api/v2/mcp/tools returns available tool definitions (for agent discovery).
 */
@RestController
@RequestMapping("/api/v2/mcp")
public class McpController {

    private final McpToolRegistry registry;

    public McpController(McpToolRegistry registry) {
        this.registry = registry;
    }

    /**
     * List all available tools with their schemas.
     * AI agent calls this to discover what operations are available.
     */
    @GetMapping("/tools")
    public ApiResponse<List<Map<String, Object>>> listTools() {
        return ApiResponse.success(registry.listToolDefinitions());
    }

    /**
     * Execute a tool by name with parameters.
     * This is the main entry point for AI agent operations.
     */
    @PostMapping("/execute")
    public ApiResponse<McpResponse> execute(@RequestBody McpRequest request) {
        McpTool tool = registry.get(request.tool());
        if (tool == null) {
            return ApiResponse.success(McpResponse.fail(request.tool(), "Unknown tool: " + request.tool()));
        }

        ActionContext context = request.context() != null ? request.context() : ActionContext.system();

        try {
            Object result = tool.execute(
                    request.params() != null ? request.params() : Map.of(),
                    context
            );
            return ApiResponse.success(McpResponse.ok(request.tool(), result));
        } catch (Exception e) {
            return ApiResponse.success(McpResponse.fail(request.tool(), e.getMessage()));
        }
    }

    /**
     * Batch execute multiple tools in sequence.
     * Useful for AI agents that need to chain operations.
     */
    @PostMapping("/batch")
    public ApiResponse<List<McpResponse>> batch(@RequestBody List<McpRequest> requests) {
        List<McpResponse> results = requests.stream().map(request -> {
            McpTool tool = registry.get(request.tool());
            if (tool == null) {
                return McpResponse.fail(request.tool(), "Unknown tool: " + request.tool());
            }
            ActionContext context = request.context() != null ? request.context() : ActionContext.system();
            try {
                Object result = tool.execute(request.params() != null ? request.params() : Map.of(), context);
                return McpResponse.ok(request.tool(), result);
            } catch (Exception e) {
                return McpResponse.fail(request.tool(), e.getMessage());
            }
        }).toList();
        return ApiResponse.success(results);
    }
}
