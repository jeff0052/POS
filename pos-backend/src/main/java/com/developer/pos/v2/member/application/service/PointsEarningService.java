package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberPointsLedgerEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberTierRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsBatchEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsExpiryRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberTierRuleRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsBatchRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsExpiryRuleRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PointsEarningService {

    private final JpaPointsRuleRepository pointsRuleRepo;
    private final JpaPointsBatchRepository pointsBatchRepo;
    private final JpaMemberAccountRepository memberAccountRepo;
    private final JpaMemberPointsLedgerRepository memberPointsLedgerRepo;
    private final JpaMemberRepository memberRepo;
    private final JpaMemberTierRuleRepository tierRuleRepo;
    private final JpaPointsExpiryRuleRepository expiryRuleRepo;

    public PointsEarningService(
            JpaPointsRuleRepository pointsRuleRepo,
            JpaPointsBatchRepository pointsBatchRepo,
            JpaMemberAccountRepository memberAccountRepo,
            JpaMemberPointsLedgerRepository memberPointsLedgerRepo,
            JpaMemberRepository memberRepo,
            JpaMemberTierRuleRepository tierRuleRepo,
            JpaPointsExpiryRuleRepository expiryRuleRepo) {
        this.pointsRuleRepo = pointsRuleRepo;
        this.pointsBatchRepo = pointsBatchRepo;
        this.memberAccountRepo = memberAccountRepo;
        this.memberPointsLedgerRepo = memberPointsLedgerRepo;
        this.memberRepo = memberRepo;
        this.tierRuleRepo = tierRuleRepo;
        this.expiryRuleRepo = expiryRuleRepo;
    }

    /**
     * Called after settlement is finalized (SETTLED) to award points to member.
     * No-op if memberId is null or no active SPEND rule found.
     */
    @Transactional
    public void awardPostSettlementPoints(Long memberId, Long merchantId, Long settlementId, long spendCents) {
        if (memberId == null) return;

        // 1. Load member (needed for tier-aware rule selection and tier multiplier)
        MemberEntity member = memberRepo.findById(memberId).orElse(null);
        if (member == null) return;

        // 2. Load active SPEND rule: filter by date window + applicable tiers, pick highest priority
        LocalDateTime now = LocalDateTime.now();
        List<PointsRuleEntity> rules = pointsRuleRepo.findByMerchantIdAndRuleStatus(merchantId, "ACTIVE");
        PointsRuleEntity rule = rules.stream()
                .filter(r -> "SPEND".equals(r.getRuleType()))
                .filter(r -> r.getStartsAt() == null || !now.isBefore(r.getStartsAt()))
                .filter(r -> r.getEndsAt() == null || !now.isAfter(r.getEndsAt()))
                .filter(r -> tierMatchesRule(r, member.getTierCode()))
                .max(java.util.Comparator.comparingInt(PointsRuleEntity::getPriority))
                .orElse(null);
        if (rule == null) return;

        // 3. Check min spend
        if (spendCents < rule.getMinSpendCents()) return;

        // 4. base_points = spendCents / 100 * rule.getPointsPerDollar()
        long basePoints = (spendCents / 100L) * rule.getPointsPerDollar();
        if (basePoints <= 0) return;

        // 5. Apply tier multiplier
        BigDecimal multiplier = getTierMultiplier(merchantId, member.getTierCode());
        long earnedPoints = new BigDecimal(basePoints).multiply(multiplier)
                .setScale(0, java.math.RoundingMode.HALF_UP).longValue();

        // 6. Cap at maxPointsPerOrder if set
        if (rule.getMaxPointsPerOrder() != null && rule.getMaxPointsPerOrder() > 0) {
            earnedPoints = Math.min(earnedPoints, rule.getMaxPointsPerOrder());
        }

        // 6.5 Cap at maxPointsPerDay if set
        if (rule.getMaxPointsPerDay() != null && rule.getMaxPointsPerDay() > 0) {
            OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            long todayEarned = memberPointsLedgerRepo.sumEarnedPointsToday(memberId, startOfDay);
            long remainingDailyCapacity = rule.getMaxPointsPerDay() - todayEarned;
            if (remainingDailyCapacity <= 0) return;
            earnedPoints = Math.min(earnedPoints, remainingDailyCapacity);
        }

        // 7.5 Idempotency guard
        if (settlementId != null && pointsBatchRepo.existsByMemberIdAndSourceTypeAndSourceRef(
                memberId, "SPEND", settlementId.toString())) {
            return; // already awarded, idempotent
        }

        // 8. Create PointsBatchEntity
        PointsBatchEntity batch = new PointsBatchEntity();
        batch.setMemberId(memberId);
        batch.setBatchNo("PTB" + System.currentTimeMillis());
        batch.setSourceType("SPEND");
        batch.setSourceRef(settlementId != null ? settlementId.toString() : null);
        batch.setRuleId(rule.getId());
        batch.setOriginalPoints(earnedPoints);
        batch.setRemainingPoints(earnedPoints);
        batch.setUsedPoints(0);
        batch.setExpiredPoints(0);
        batch.setEarnedAt(now);
        batch.setExpiresAt(calculateExpiry(merchantId, now));
        batch.setBatchStatus("ACTIVE");
        batch.setCreatedAt(now);
        pointsBatchRepo.save(batch);

        // 9. Update MemberAccountEntity
        MemberAccountEntity account = memberAccountRepo.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("Member account not found: " + memberId));
        account.setPointsBalance(account.getPointsBalance() + earnedPoints);
        account.setAvailablePoints(account.getAvailablePoints() + earnedPoints);
        memberAccountRepo.save(account);

        // 10. Write ledger entry
        MemberPointsLedgerEntity ledger = new MemberPointsLedgerEntity();
        ledger.setLedgerNo("PTS" + System.currentTimeMillis());
        ledger.setMerchantId(merchantId);
        ledger.setMemberId(memberId);
        ledger.setChangeType("EARN");
        ledger.setPointsDelta(earnedPoints);
        ledger.setBalanceAfter(account.getPointsBalance());
        ledger.setSourceType("SPEND");
        ledger.setSourceRef(settlementId != null ? settlementId.toString() : null);
        memberPointsLedgerRepo.save(ledger);
    }

    /**
     * Calculates expiry date from merchant's points_expiry_rules.
     * Falls back to 12-month rolling if no rule configured, so the expiry job can always find batches.
     */
    private LocalDateTime calculateExpiry(Long merchantId, LocalDateTime earnedAt) {
        return expiryRuleRepo.findByMerchantIdAndIsActiveTrue(merchantId)
                .map(rule -> {
                    if ("NEVER".equals(rule.getExpiryType())) return null;
                    if ("YEAR_END".equals(rule.getExpiryType())) {
                        int month = rule.getYearEndClearMonth() > 0 ? rule.getYearEndClearMonth() : 12;
                        int day = rule.getYearEndClearDay() > 0 ? rule.getYearEndClearDay() : 31;
                        LocalDateTime yearEnd = LocalDateTime.of(earnedAt.getYear(), month, day, 23, 59, 59);
                        return yearEnd.isAfter(earnedAt) ? yearEnd : yearEnd.plusYears(1);
                    }
                    // ROLLING (default)
                    int months = rule.getExpiryMonths() > 0 ? rule.getExpiryMonths() : 12;
                    return earnedAt.plusMonths(months);
                })
                .orElse(earnedAt.plusMonths(12)); // default 12-month rolling when no rule configured
    }

    /** Checks if a points rule applies to the given tier. Null/empty applicableTiers means "all tiers". */
    private boolean tierMatchesRule(PointsRuleEntity rule, String tierCode) {
        String tiers = rule.getApplicableTiers();
        if (tiers == null || tiers.isBlank() || "[]".equals(tiers.trim())) return true;
        // Simple JSON array contains check — applicableTiers is stored as ["GOLD","SILVER"]
        return tiers.contains("\"" + tierCode + "\"");
    }

    private BigDecimal getTierMultiplier(Long merchantId, String tierCode) {
        return tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId).stream()
                .filter(r -> r.getTierCode().equalsIgnoreCase(tierCode))
                .map(MemberTierRuleEntity::getPointsMultiplier)
                .findFirst()
                .orElse(BigDecimal.ONE);
    }
}
