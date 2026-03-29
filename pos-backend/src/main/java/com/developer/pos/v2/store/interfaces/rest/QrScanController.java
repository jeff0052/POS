package com.developer.pos.v2.store.interfaces.rest;

import com.developer.pos.v2.store.application.service.QrTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class QrScanController {

    private final QrTokenService qrTokenService;

    @Value("${app.ordering.base-url:https://localhost:3000}")
    private String orderingBaseUrl;

    public QrScanController(QrTokenService qrTokenService) {
        this.qrTokenService = qrTokenService;
    }

    @GetMapping("/qr/{storeId}/{tableId}/{token}")
    public ResponseEntity<Void> scanQr(
            @PathVariable Long storeId,
            @PathVariable Long tableId,
            @PathVariable String token
    ) {
        String jwt = qrTokenService.validateQrAndIssueJwt(storeId, tableId, token);
        String redirectUrl = orderingBaseUrl + "/ordering?token=" + jwt;
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
