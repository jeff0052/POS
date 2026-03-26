package com.developer.pos.payment.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfig.class);

    @Value("${pos.callback-secret:}")
    private String callbackSecret;

    @Value("${security.service-api-key:}")
    private String serviceApiKey;

    @PostConstruct
    public void validate() {
        if (callbackSecret == null || callbackSecret.isBlank()) {
            log.error("POS_CALLBACK_SECRET is not set. Payment callbacks to POS will be unsigned. Set POS_CALLBACK_SECRET for production.");
        }

        if (serviceApiKey == null || serviceApiKey.isBlank()) {
            log.warn("PAYMENT_SERVICE_API_KEY is not set. Payment service endpoints are open. Set PAYMENT_SERVICE_API_KEY for production.");
        }
    }
}
