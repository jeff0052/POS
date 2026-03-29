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
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final PermissionCacheService permissionCacheService;
    private final JpaUserRepository userRepository;

    public JwtAuthFilter(JwtProvider jwtProvider, PermissionCacheService permissionCacheService, JpaUserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.permissionCacheService = permissionCacheService;
        this.userRepository = userRepository;
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

                // Legacy token detection: userCode claim is absent in old JWTs.
                // Legacy tokens carry auth_users.id as subject, which differs from users.id.
                // V103 migrated auth_users with user_code = 'AU-' + auth_users.id,
                // so we derive the stable mapping key from the token subject.
                Long rbacUserId = userId;
                if (userCode == null) {
                    String derivedUserCode = "AU-" + userId;
                    Long resolvedMerchantId = merchantId != null ? merchantId : 0L;
                    Optional<UserEntity> rbacUser = userRepository.findByUserCodeAndMerchantId(derivedUserCode, resolvedMerchantId);
                    if (rbacUser.isPresent()) {
                        rbacUserId = rbacUser.get().getId();
                    } else {
                        // Not migrated or skipped — pure legacy, skip RBAC resolution
                        rbacUserId = null;
                    }
                }

                // Resolve permissions from RBAC tables (cached)
                Set<String> permissions;
                Set<Long> accessibleStoreIds;
                String resolvedRole = role;
                long maxRefundCents = 0L;
                if (rbacUserId != null) {
                    try {
                        ResolvedPermissions resolved = permissionCacheService.resolve(rbacUserId);
                        permissions = resolved.permissions();
                        accessibleStoreIds = resolved.accessibleStoreIds();
                        if (resolved.primaryRoleCode() != null) {
                            resolvedRole = resolved.primaryRoleCode();
                        }
                        maxRefundCents = resolved.maxRefundCents() != null ? resolved.maxRefundCents() : 0L;
                    } catch (Exception e) {
                        // RBAC resolution failed — safe legacy fallback
                        permissions = Set.of();
                        accessibleStoreIds = storeId != null ? Set.of(storeId) : Set.of();
                    }
                } else {
                    // Pure legacy token, no RBAC data
                    permissions = Set.of();
                    accessibleStoreIds = storeId != null ? Set.of(storeId) : Set.of();
                }

                AuthenticatedActor actor = new AuthenticatedActor(
                        userId, username, userCode, resolvedRole, merchantId, storeId,
                        accessibleStoreIds, permissions, maxRefundCents
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
