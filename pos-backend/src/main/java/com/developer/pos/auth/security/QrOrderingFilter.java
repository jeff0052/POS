package com.developer.pos.auth.security;

import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaQrTokenRepository;
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
 * All QR ordering endpoints (except /menu) must present a valid ordering JWT.
 *
 * Also verifies that the underlying qr_tokens record is still ACTIVE,
 * so QR rotation at table clean invalidates all previously issued ordering JWTs.
 *
 * Stores parsed claims as request attributes for downstream use:
 *   qr.storeId, qr.tableId, qr.tableCode, qr.sessionId (nullable)
 */
@Component
public class QrOrderingFilter extends OncePerRequestFilter {

    private static final String ORDERING_TOKEN_HEADER = "X-Ordering-Token";
    private static final String QR_ORDERING_PATH = "/api/v2/qr-ordering/";

    private final JwtProvider jwtProvider;
    private final JpaQrTokenRepository qrTokenRepository;

    public QrOrderingFilter(JwtProvider jwtProvider, JpaQrTokenRepository qrTokenRepository) {
        this.jwtProvider = jwtProvider;
        this.qrTokenRepository = qrTokenRepository;
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
            sendError(response, 40101, "Missing X-Ordering-Token header");
            return;
        }

        Claims claims;
        try {
            claims = jwtProvider.parseToken(token);
        } catch (Exception e) {
            sendError(response, 40102, "Invalid or expired ordering token");
            return;
        }

        if (!"qr-ordering".equals(claims.getSubject())) {
            sendError(response, 40103, "Token is not an ordering token");
            return;
        }

        // Verify the underlying QR token is still ACTIVE (revoked on table clean/refresh)
        Long qrTokenId = claims.get("qrTokenId", Long.class);
        if (qrTokenId != null) {
            boolean stillActive = qrTokenRepository.findById(qrTokenId)
                    .map(qrToken -> "ACTIVE".equals(qrToken.getTokenStatus()))
                    .orElse(false);
            if (!stillActive) {
                sendError(response, 40104, "QR token has been revoked. Please re-scan the QR code.");
                return;
            }
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

    private void sendError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
