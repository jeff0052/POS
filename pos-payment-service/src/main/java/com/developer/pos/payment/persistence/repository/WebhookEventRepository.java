package com.developer.pos.payment.persistence.repository;

import com.developer.pos.payment.core.WebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, Long> {
    Optional<WebhookEventEntity> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
