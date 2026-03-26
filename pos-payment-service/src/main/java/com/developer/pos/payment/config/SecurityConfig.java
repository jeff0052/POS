package com.developer.pos.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Payment Service security:
 * - /api/v1/payments/health and /actuator/** are public
 * - /api/v1/payments/webhooks/** are public (verified by HMAC signature)
 * - /api/v1/payments/dcs/result is public (Android terminal posts here)
 * - Everything else requires a service API key via X-Service-Key header
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.service-api-key:}")
    private String serviceApiKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/payments/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Webhooks verified by HMAC signature, not by API key
                .requestMatchers("/api/v1/payments/webhooks/**").permitAll()
                // DCS result from Android terminal — will add device auth in Phase 3
                .requestMatchers("/api/v1/payments/dcs/result").permitAll()
                // Everything else requires service API key
                .anyRequest().authenticated()
            )
            .addFilterBefore(new ServiceApiKeyFilter(serviceApiKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Simple API key filter for service-to-service auth.
     * POS backend must send X-Service-Key header to create/query/cancel intents.
     */
    static class ServiceApiKeyFilter extends OncePerRequestFilter {

        private final String expectedKey;

        ServiceApiKeyFilter(String expectedKey) {
            this.expectedKey = expectedKey;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {

            // Skip if no key configured (fail-open only in dev — ProviderConfig enforces in prod)
            if (expectedKey == null || expectedKey.isBlank()) {
                // In dev mode without key, authenticate as SERVICE
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("SERVICE", null,
                                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))));
                chain.doFilter(request, response);
                return;
            }

            String providedKey = request.getHeader("X-Service-Key");
            if (providedKey != null && providedKey.equals(expectedKey)) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("SERVICE", null,
                                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))));
            }

            chain.doFilter(request, response);
        }
    }
}
