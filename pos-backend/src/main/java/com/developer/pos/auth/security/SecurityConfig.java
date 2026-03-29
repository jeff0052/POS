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

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final JwtAuthFilter jwtAuthFilter;
    private final QrOrderingFilter qrOrderingFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, QrOrderingFilter qrOrderingFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.qrOrderingFilter = qrOrderingFilter;
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
                // QR ordering is public (protected by QrOrderingFilter, not Spring Security)
                .requestMatchers("/api/v2/qr-ordering/**").permitAll()
                // QR scan entry point is public (302 redirect to ordering page)
                .requestMatchers("/qr/**").permitAll()
                // VibeCash webhook is public (verified by signature)
                .requestMatchers("/api/v2/payments/vibecash/webhook").permitAll()
                // Internal payment callback (verified by HMAC signature)
                .requestMatchers("/api/v2/internal/**").permitAll()
                // OPTIONS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Buffet status polling is public (frontend polling, no auth needed)
                .requestMatchers(HttpMethod.GET, "/api/v2/stores/*/tables/*/buffet/status").permitAll()

                // Table operations requiring specific permissions (BEFORE the broad /stores/** matcher)
                .requestMatchers(HttpMethod.POST, "/api/v2/stores/*/tables/merge").hasAuthority("TABLE_MERGE")
                .requestMatchers(HttpMethod.POST, "/api/v2/stores/*/tables/unmerge").hasAuthority("TABLE_MERGE")
                .requestMatchers(HttpMethod.POST, "/api/v2/stores/*/tables/*/mark-clean").hasAuthority("TABLE_CLEAN")
                .requestMatchers(HttpMethod.POST, "/api/v2/stores/*/tables/*/qr/refresh").hasAuthority("TABLE_MANAGE")

                // Settlement/payment endpoints require SETTLEMENT_COLLECT (BEFORE the broad /stores/** matcher)
                .requestMatchers("/api/v2/stores/*/tables/*/payment/**").hasAuthority("SETTLEMENT_COLLECT")

                // Kitchen endpoints (BEFORE the broad /stores/** permitAll)
                .requestMatchers("/api/v2/stores/*/kitchen-tickets").hasAuthority("KDS_OPERATE")
                .requestMatchers(HttpMethod.GET, "/api/v2/stores/*/kitchen-stations").hasAnyAuthority("KDS_OPERATE", "KDS_MANAGE")
                .requestMatchers(HttpMethod.POST, "/api/v2/stores/*/kitchen-stations").hasAuthority("KDS_MANAGE")

                // POS tablet endpoints (WebView, no auth token)
                .requestMatchers("/api/v2/stores/**").permitAll()
                .requestMatchers("/api/v1/stores/**").permitAll()
                .requestMatchers("/api/v1/categories/**").permitAll()
                .requestMatchers("/api/v1/products/**").permitAll()
                .requestMatchers("/api/v1/orders/**").permitAll()
                .requestMatchers("/api/v2/settlement/**").permitAll()
                .requestMatchers("/api/v2/promotions/**").permitAll()
                .requestMatchers("/api/v2/members/**").permitAll()
                .requestMatchers("/api/v2/reports/**").permitAll()
                .requestMatchers("/api/v2/reservations/**").permitAll()
                .requestMatchers("/api/v2/shifts/**").permitAll()
                .requestMatchers("/api/v2/ai/**").permitAll()
                // Public image serving (QR/POS need unauthenticated access)
                .requestMatchers("/api/v2/images/**").permitAll()
                // PIN login is public
                .requestMatchers("/api/v2/auth/pin-login").permitAll()
                // RBAC management requires USER_MANAGE or ROLE_MANAGE
                .requestMatchers("/api/v2/rbac/**").hasAnyAuthority("USER_MANAGE", "ROLE_MANAGE")
                // Audit logs require AUDIT_VIEW; approval requires AUDIT_APPROVE
                .requestMatchers(HttpMethod.GET, "/api/v2/audit/**").hasAuthority("AUDIT_VIEW")
                .requestMatchers(HttpMethod.POST, "/api/v2/audit/**").hasAuthority("AUDIT_APPROVE")
                // All admin operations require ADMIN/PLATFORM_ADMIN role OR appropriate permission
                .requestMatchers("/api/v2/admin/**").hasAnyAuthority("USER_MANAGE", "STORE_MANAGE", "ROLE_ADMIN", "ROLE_PLATFORM_ADMIN")
                // MCP requires ADMIN/PLATFORM_ADMIN role OR appropriate permission
                .requestMatchers("/api/v2/mcp/**").hasAnyAuthority("AI_RECOMMENDATION_VIEW", "AI_RECOMMENDATION_APPROVE", "ROLE_ADMIN", "ROLE_PLATFORM_ADMIN")
                // Platform admin — SUPER_ADMIN only (not MERCHANT_OWNER even though they have STORE_MANAGE)
                .requestMatchers("/api/v2/platform/**").hasAuthority("ROLE_SUPER_ADMIN")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(qrOrderingFilter, JwtAuthFilter.class);

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
