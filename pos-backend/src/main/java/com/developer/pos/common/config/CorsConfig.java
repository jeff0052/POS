package com.developer.pos.common.config;

// CORS is now handled by SecurityConfig.corsConfigurationSource()
// This class is intentionally empty to avoid conflict with Spring Security's CORS handling.

import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsConfig {
    // Intentionally empty - CORS moved to SecurityConfig
}
