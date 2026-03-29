package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.catalog.application.dto.BuffetStatusDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageItemEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageItemRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageRepository;
import com.developer.pos.v2.catalog.interfaces.rest.request.StartBuffetRequest;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class BuffetSessionService implements UseCase {

    private final JpaTableSessionRepository sessionRepo;
    private final JpaBuffetPackageRepository packageRepo;
    private final JpaBuffetPackageItemRepository itemRepo;
    private final JpaStoreLookupRepository storeLookupRepository;

    public BuffetSessionService(
            JpaTableSessionRepository sessionRepo,
            JpaBuffetPackageRepository packageRepo,
            JpaBuffetPackageItemRepository itemRepo,
            JpaStoreLookupRepository storeLookupRepository
    ) {
        this.sessionRepo = sessionRepo;
        this.packageRepo = packageRepo;
        this.itemRepo = itemRepo;
        this.storeLookupRepository = storeLookupRepository;
    }

    // ─── Start Buffet ────────────────────────────────────────────────────

    @Transactional
    public BuffetStatusDto startBuffet(Long storeId, Long tableId, StartBuffetRequest req) {
        enforceBuffetStart();
        enforceStoreAccess(storeId);

        BuffetPackageEntity pkg = packageRepo.findById(req.packageId())
                .orElseThrow(() -> new IllegalArgumentException("Buffet package not found: " + req.packageId()));
        if (!Objects.equals(pkg.getStoreId(), storeId)) {
            throw new IllegalArgumentException("Buffet package does not belong to this store");
        }
        if (!"ACTIVE".equals(pkg.getPackageStatus())) {
            throw new IllegalArgumentException("Buffet package is not active: " + pkg.getPackageStatus());
        }

        TableSessionEntity session = sessionRepo
                .findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .orElseThrow(() -> new IllegalArgumentException("No open session for this table"));

        if ("BUFFET".equals(session.getDiningMode())) {
            throw new IllegalStateException("Buffet already started for this table");
        }

        OffsetDateTime now = OffsetDateTime.now();
        session.setDiningMode("BUFFET");
        session.setGuestCount(req.guestCount());
        session.setChildCount(req.childCount());
        session.setBuffetPackageId(pkg.getId());
        session.setBuffetStartedAt(now);
        session.setBuffetEndsAt(now.plusMinutes(pkg.getDurationMinutes()));
        session.setBuffetStatus("ACTIVE");
        sessionRepo.save(session);

        return new BuffetStatusDto(
                session.getId(),
                "ACTIVE",
                session.getBuffetStartedAt(),
                session.getBuffetEndsAt(),
                pkg.getDurationMinutes(),
                0L,
                0L,
                session.getGuestCount(),
                session.getChildCount(),
                pkg.getPackageName(),
                pkg.getPriceCents(),
                false
        );
    }

    // ─── Get Buffet Status ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BuffetStatusDto getBuffetStatus(Long storeId, Long tableId) {
        TableSessionEntity session = sessionRepo
                .findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .orElseThrow(() -> new IllegalArgumentException("No open session for this table"));

        if (!"BUFFET".equals(session.getDiningMode())) {
            throw new IllegalArgumentException("Session is not in buffet mode");
        }

        BuffetPackageEntity pkg = packageRepo.findById(session.getBuffetPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Buffet package not found: " + session.getBuffetPackageId()));

        OffsetDateTime now = OffsetDateTime.now();
        long remainingMinutes = Duration.between(now, session.getBuffetEndsAt()).toMinutes();

        String status;
        long overtimeMinutes = 0;
        long overtimeFeeCents = 0;
        boolean forceCheckout = false;

        if (remainingMinutes > pkg.getWarningBeforeMinutes()) {
            status = "ACTIVE";
        } else if (remainingMinutes > 0) {
            status = "WARNING";
        } else {
            status = "OVERTIME";
            overtimeMinutes = -remainingMinutes;
            long chargeableMinutes = Math.max(0, overtimeMinutes - pkg.getOvertimeGraceMinutes());
            chargeableMinutes = Math.min(chargeableMinutes, pkg.getMaxOvertimeMinutes());
            overtimeFeeCents = chargeableMinutes * pkg.getOvertimeFeePerMinuteCents();
            forceCheckout = overtimeMinutes > pkg.getMaxOvertimeMinutes();
            remainingMinutes = 0;
        }

        return new BuffetStatusDto(
                session.getId(),
                status,
                session.getBuffetStartedAt(),
                session.getBuffetEndsAt(),
                remainingMinutes,
                overtimeMinutes,
                overtimeFeeCents,
                session.getGuestCount(),
                session.getChildCount(),
                pkg.getPackageName(),
                pkg.getPriceCents(),
                forceCheckout
        );
    }

    // ─── Validate Buffet Order ───────────────────────────────────────────

    public record ValidateOrderItemRequest(Long skuId, int quantity) {}

    public record ValidatedItem(
            Long skuId, boolean buffetIncluded, long surchargeCents,
            Long buffetPackageId, boolean rejected, String rejectionReason
    ) {}

    public record BuffetOrderValidationResult(List<ValidatedItem> items) {}

    @Transactional(readOnly = true)
    public BuffetOrderValidationResult validateBuffetOrder(Long sessionId, List<ValidateOrderItemRequest> items) {
        TableSessionEntity session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!"BUFFET".equals(session.getDiningMode())) {
            throw new IllegalArgumentException("Session is not in buffet mode");
        }

        List<ValidatedItem> results = new ArrayList<>();
        for (ValidateOrderItemRequest item : items) {
            Optional<BuffetPackageItemEntity> packageItem =
                    itemRepo.findByPackageIdAndSkuId(session.getBuffetPackageId(), item.skuId());

            if (packageItem.isEmpty()) {
                // Extra item — not in package
                results.add(new ValidatedItem(item.skuId(), false, 0, null, false, null));
                continue;
            }

            BuffetPackageItemEntity pi = packageItem.get();

            if ("EXCLUDED".equals(pi.getInclusionType())) {
                results.add(new ValidatedItem(item.skuId(), false, 0, null, true, "excluded from package"));
                continue;
            }

            if (pi.getMaxQtyPerPerson() != null) {
                // TODO: When wired into real order flow, query already-ordered qty from
                // submitted_order_items + active_table_order_items for this session and add to item.quantity()
                // before comparing against the limit. Currently only validates the current request batch.
                int limit = pi.getMaxQtyPerPerson() * session.getGuestCount();
                if (item.quantity() > limit) {
                    results.add(new ValidatedItem(
                            item.skuId(), true, pi.getSurchargeCents(),
                            session.getBuffetPackageId(), true,
                            "exceeds limit of " + pi.getMaxQtyPerPerson() + " per person"
                    ));
                    continue;
                }
            }

            results.add(new ValidatedItem(
                    item.skuId(), true, pi.getSurchargeCents(),
                    session.getBuffetPackageId(), false, null
            ));
        }

        return new BuffetOrderValidationResult(results);
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
