package com.developer.pos.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates X-Ordering-Token JWT on /api/v2/qr-ordering/** endpoints.
 * The QR scan flow (GET /qr/{storeId}/{tableId}/{token}) issues this JWT.
 * All QR ordering endpoints must present a valid ordering JWT.
 *
 * Stores parsed claims as request attributes for downstream use:
 *   qr.storeId, qr.tableId, qr.tableCode, qr.sessionId (nullable)
 */
@Component
public class QrOrderingFilter extends OncePerRequestFilter {

    private static final String ORDERING_TOKEN_HEADER = "X-Ordering-Token";
    private static final String QR_ORDERING_PATH = "/api/v2/qr-ordering/";

    private final JwtProvider jwtProvider;

    public QrOrderingFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(QR_ORDERING_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // /menu only needs storeCode, allow without token for backward compat
        if (request.getRequestURI().endsWith("/menu")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(ORDERING_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":40101,\"message\":\"Missing X-Ordering-Token header\",\"data\":null}");
            return;
        }

        Claims claims;
        try {
            claims = jwtProvider.parseToken(token);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":40102,\"message\":\"Invalid or expired ordering token\",\"data\":null}");
            return;
        }

        if (!"qr-ordering".equals(claims.getSubject())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":40103,\"message\":\"Token is not an ordering token\",\"data\":null}");
            return;
        }

        // Store parsed claims as request attributes
        request.setAttribute("qr.storeId", claims.get("storeId", Long.class));
        request.setAttribute("qr.tableId", claims.get("tableId", Long.class));
        request.setAttribute("qr.tableCode", claims.get("tableCode", String.class));
        Object sessionId = claims.get("sessionId");
        if (sessionId != null) {
            request.setAttribute("qr.sessionId", ((Number) sessionId).longValue());
        }

        filterChain.doFilter(request, response);
    }
}
