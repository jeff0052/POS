package com.developer.pos.v2.store.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.auth.security.JwtProvider;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.application.dto.QrTokenResultDto;
import com.developer.pos.v2.store.infrastructure.persistence.entity.QrTokenEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaQrTokenRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class QrTokenService implements UseCase {

    private final JpaQrTokenRepository qrTokenRepository;
    private final JpaStoreTableRepository storeTableRepository;
    private final JpaTableSessionRepository tableSessionRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.qr.base-url:https://localhost:3000}")
    private String qrBaseUrl;

    public QrTokenService(
            JpaQrTokenRepository qrTokenRepository,
            JpaStoreTableRepository storeTableRepository,
            JpaTableSessionRepository tableSessionRepository,
            JwtProvider jwtProvider
    ) {
        this.qrTokenRepository = qrTokenRepository;
        this.storeTableRepository = storeTableRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public QrTokenResultDto refreshQr(Long storeId, Long tableId) {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasStoreAccess(storeId)) {
            throw new IllegalArgumentException("No access to store: " + storeId);
        }

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(tableId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found."));

        qrTokenRepository.expireAllActiveByTableId(tableId);

        String token = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);

        QrTokenEntity qrToken = new QrTokenEntity(storeId, tableId, token, "ACTIVE", expiresAt);
        qrTokenRepository.saveAndFlush(qrToken);

        String qrUrl = qrBaseUrl + "/qr/" + storeId + "/" + tableId + "/" + token;
        return new QrTokenResultDto(token, expiresAt, qrUrl);
    }

    @Transactional(readOnly = true)
    public String validateQrAndIssueJwt(Long storeId, Long tableId, String token) {
        QrTokenEntity qrToken = qrTokenRepository
                .findByStoreIdAndTableIdAndTokenAndTokenStatus(storeId, tableId, token, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired QR token."));

        if (qrToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("QR token has expired.");
        }

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(tableId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found."));

        TableSessionEntity session = tableSessionRepository
                .findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .orElse(null);

        Long sessionId = session != null ? session.getId() : null;

        return jwtProvider.generateOrderingToken(storeId, tableId, sessionId, table.getTableCode());
    }
}
