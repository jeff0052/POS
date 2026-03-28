package com.developer.pos.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.developer.pos.v2.rbac.application.dto.ResolvedPermissions;
import com.developer.pos.v2.rbac.application.service.PermissionCacheService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final PermissionCacheService permissionCacheService;

    public JwtAuthFilter(JwtProvider jwtProvider, PermissionCacheService permissionCacheService) {
        this.jwtProvider = jwtProvider;
        this.permissionCacheService = permissionCacheService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtProvider.isValid(token)) {
                Claims claims = jwtProvider.parseToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                String userCode = claims.get("userCode", String.class);
                Long merchantId = claims.get("merchantId", Long.class);
                Long storeId = claims.get("storeId", Long.class);

                // Resolve permissions from RBAC tables (cached)
                Set<String> permissions;
                Set<Long> accessibleStoreIds;
                String resolvedRole = role;
                try {
                    ResolvedPermissions resolved = permissionCacheService.resolve(userId);
                    permissions = resolved.permissions();
                    accessibleStoreIds = resolved.accessibleStoreIds();
                    if (resolved.primaryRoleCode() != null) {
                        resolvedRole = resolved.primaryRoleCode();
                    }
                } catch (Exception e) {
                    // Fallback for users not yet in RBAC tables (legacy tokens)
                    permissions = Set.of();
                    accessibleStoreIds = storeId != null ? Set.of(storeId) : Set.of();
                }

                AuthenticatedActor actor = new AuthenticatedActor(
                        userId, username, userCode, resolvedRole, merchantId, storeId,
                        accessibleStoreIds, permissions
                );

                // Build authorities: permission codes + ROLE_ prefix for backward compat
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                for (String perm : permissions) {
                    authorities.add(new SimpleGrantedAuthority(perm));
                }
                authorities.add(new SimpleGrantedAuthority("ROLE_" + resolvedRole));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        actor, null, authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
