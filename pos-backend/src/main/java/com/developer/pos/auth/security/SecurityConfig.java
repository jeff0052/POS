package com.developer.pos.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/login", "/api/v2/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/logout", "/api/v2/auth/logout").permitAll()
                .requestMatchers("/api/v1/auth/bootstrap", "/api/v2/auth/bootstrap").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // QR ordering is public (customer-facing)
                .requestMatchers("/api/v2/qr-ordering/**").permitAll()
                // VibeCash webhook is public (verified by signature)
                .requestMatchers("/api/v2/payments/vibecash/webhook").permitAll()
                // Internal payment callback (verified by HMAC signature)
                .requestMatchers("/api/v2/internal/**").permitAll()
                // OPTIONS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public image serving (QR/POS need unauthenticated access)
                .requestMatchers("/api/v2/images/**").permitAll()
                // All admin operations require ADMIN or PLATFORM_ADMIN
                .requestMatchers("/api/v2/admin/**").hasAnyRole("ADMIN", "PLATFORM_ADMIN")
                // MCP requires ADMIN or PLATFORM_ADMIN
                .requestMatchers("/api/v2/mcp/**").hasAnyRole("ADMIN", "PLATFORM_ADMIN")
                // Platform admin
                .requestMatchers("/api/v2/platform/**").hasRole("PLATFORM_ADMIN")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://54.237.230.5",
                "http://54.237.230.5:*",
                "https://*.github.io"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
