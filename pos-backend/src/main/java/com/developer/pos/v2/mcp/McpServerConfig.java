package com.developer.pos.v2.mcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {
    @Bean
    public McpToolRegistry mcpToolRegistry() {
        return new McpToolRegistry();
    }
}
