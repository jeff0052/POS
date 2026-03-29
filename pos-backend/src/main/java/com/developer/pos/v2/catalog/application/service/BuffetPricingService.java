package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.BuffetBillDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageRepository;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class BuffetPricingService implements UseCase {

    private final JpaTableSessionRepository sessionRepo;
    private final JpaBuffetPackageRepository packageRepo;
    private final JpaSubmittedOrderRepository submittedOrderRepo;
    private final JpaStoreLookupRepository storeLookupRepository;

    public BuffetPricingService(
            JpaTableSessionRepository sessionRepo,
            JpaBuffetPackageRepository packageRepo,
            JpaSubmittedOrderRepository submittedOrderRepo,
            JpaStoreLookupRepository storeLookupRepository
    ) {
        this.sessionRepo = sessionRepo;
        this.packageRepo = packageRepo;
        this.submittedOrderRepo = submittedOrderRepo;
        this.storeLookupRepository = storeLookupRepository;
    }

    @Transactional(readOnly = true)
    public BuffetBillDto calculateBuffetTotal(Long storeId, Long sessionId) {
        enforceBuffetStart();
        enforceStoreAccess(storeId);

        // Load and validate session
        TableSessionEntity session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!"BUFFET".equals(session.getDiningMode())) {
            throw new IllegalArgumentException("Session is not in buffet mode");
        }
        if (!Objects.equals(session.getStoreId(), storeId)) {
            throw new IllegalArgumentException("Session does not belong to this store");
        }

        // Load package
        BuffetPackageEntity pkg = packageRepo.findById(session.getBuffetPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Buffet package not found: " + session.getBuffetPackageId()));

        // Calculate head fee
        long headFee = pkg.getPriceCents() * session.getGuestCount();
        if (pkg.getChildPriceCents() != null) {
            headFee += pkg.getChildPriceCents() * session.getChildCount();
        }

        // Load all submitted orders for session
        List<SubmittedOrderEntity> orders = submittedOrderRepo.findAllByTableSessionIdOrderByIdAsc(session.getId());

        // Aggregate items
        long surchargeTotal = 0L;
        long extraTotal = 0L;

        for (SubmittedOrderEntity order : orders) {
            for (SubmittedOrderItemEntity item : order.getItems()) {
                if (item.isBuffetIncluded()) {
                    if (item.getBuffetSurchargeCents() > 0) {
                        surchargeTotal += (long) item.getQuantity() * item.getBuffetSurchargeCents();
                    }
                } else {
                    extraTotal += item.getLineTotalCents();
                }
            }
        }

        // Calculate overtime
        OffsetDateTime now = OffsetDateTime.now();
        long actualMinutes = Duration.between(session.getBuffetStartedAt(), now).toMinutes();
        long overtimeMinutes = Math.max(0L, actualMinutes - pkg.getDurationMinutes());
        long billableOvertime = Math.max(0L, overtimeMinutes - pkg.getOvertimeGraceMinutes());
        billableOvertime = Math.min(billableOvertime, pkg.getMaxOvertimeMinutes());
        long overtimeFeeCents = billableOvertime * pkg.getOvertimeFeePerMinuteCents();

        long grandTotal = headFee + surchargeTotal + extraTotal + overtimeFeeCents;

        return new BuffetBillDto(
                session.getGuestCount(),
                session.getChildCount(),
                headFee,
                surchargeTotal,
                extraTotal,
                overtimeMinutes,
                overtimeFeeCents,
                grandTotal,
                pkg.getPackageName(),
                pkg.getPriceCents(),
                pkg.getChildPriceCents(),
                pkg.getDurationMinutes(),
                pkg.getOvertimeGraceMinutes(),
                pkg.getMaxOvertimeMinutes()
        );
    }

    // ─── Permission Helpers ──────────────────────────────────────────────

    private void enforceBuffetStart() {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasPermission("BUFFET_START")) {
            throw new SecurityException("Missing BUFFET_START permission");
        }
    }

    private void enforceStoreAccess(Long storeId) {
        AuthenticatedActor actor = AuthContext.current();
        if (actor.merchantId() != null && actor.merchantId() != 0L) {
            StoreEntity store = storeLookupRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
            if (!Objects.equals(store.getMerchantId(), actor.merchantId())) {
                throw new SecurityException("Store does not belong to your merchant");
            }
            if (actor.accessibleStoreIds() != null && !actor.accessibleStoreIds().isEmpty()
                    && !actor.hasStoreAccess(storeId)) {
                throw new SecurityException("You do not have access to store: " + storeId);
            }
        }
    }
}
