package com.developer.pos.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtProvider(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException("auth.jwt.secret must be set and at least 32 characters. Application cannot start without a JWT secret.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String username, String role, Long merchantId, Long storeId) {
        return generateToken(userId, username, null, role, merchantId, storeId);
    }

    public String generateToken(Long userId, String username, String userCode, String role, Long merchantId, Long storeId) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .claim("merchantId", merchantId)
                .claim("storeId", storeId);
        if (userCode != null) {
            builder.claim("userCode", userCode);
        }
        return builder
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
