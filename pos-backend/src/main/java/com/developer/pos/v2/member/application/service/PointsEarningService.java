package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberPointsLedgerEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberTierRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsBatchEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsRuleEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberTierRuleRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsBatchRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PointsEarningService {

    private final JpaPointsRuleRepository pointsRuleRepo;
    private final JpaPointsBatchRepository pointsBatchRepo;
    private final JpaMemberAccountRepository memberAccountRepo;
    private final JpaMemberPointsLedgerRepository memberPointsLedgerRepo;
    private final JpaMemberRepository memberRepo;
    private final JpaMemberTierRuleRepository tierRuleRepo;

    public PointsEarningService(
            JpaPointsRuleRepository pointsRuleRepo,
            JpaPointsBatchRepository pointsBatchRepo,
            JpaMemberAccountRepository memberAccountRepo,
            JpaMemberPointsLedgerRepository memberPointsLedgerRepo,
            JpaMemberRepository memberRepo,
            JpaMemberTierRuleRepository tierRuleRepo) {
        this.pointsRuleRepo = pointsRuleRepo;
        this.pointsBatchRepo = pointsBatchRepo;
        this.memberAccountRepo = memberAccountRepo;
        this.memberPointsLedgerRepo = memberPointsLedgerRepo;
        this.memberRepo = memberRepo;
        this.tierRuleRepo = tierRuleRepo;
    }

    /**
     * Called after settlement is finalized (SETTLED) to award points to member.
     * No-op if memberId is null or no active SPEND rule found.
     */
    @Transactional
    public void awardPostSettlementPoints(Long memberId, Long merchantId, Long settlementId, long spendCents) {
        if (memberId == null) return;

        // 1. Load active SPEND rule for merchant
        List<PointsRuleEntity> rules = pointsRuleRepo.findByMerchantIdAndRuleStatus(merchantId, "ACTIVE");
        PointsRuleEntity rule = rules.stream()
                .filter(r -> "SPEND".equals(r.getRuleType()))
                .findFirst().orElse(null);
        if (rule == null) return;

        // 2. Check min spend
        if (spendCents < rule.getMinSpendCents()) return;

        // 3. base_points = spendCents / 100 * rule.getPointsPerDollar()
        long basePoints = (spendCents / 100L) * rule.getPointsPerDollar();
        if (basePoints <= 0) return;

        // 4. Apply tier multiplier
        MemberEntity member = memberRepo.findById(memberId).orElse(null);
        if (member == null) return;
        BigDecimal multiplier = getTierMultiplier(merchantId, member.getTierCode());
        long earnedPoints = new BigDecimal(basePoints).multiply(multiplier)
                .setScale(0, java.math.RoundingMode.HALF_UP).longValue();

        // 5. Cap at maxPointsPerOrder if set
        if (rule.getMaxPointsPerOrder() != null && rule.getMaxPointsPerOrder() > 0) {
            earnedPoints = Math.min(earnedPoints, rule.getMaxPointsPerOrder());
        }

        // 5.5 Idempotency guard
        if (settlementId != null && pointsBatchRepo.existsByMemberIdAndSourceTypeAndSourceRef(
                memberId, "SPEND", settlementId.toString())) {
            return; // already awarded, idempotent
        }

        // 6. Create PointsBatchEntity
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
        batch.setEarnedAt(LocalDateTime.now());
        batch.setExpiresAt(null); // no expiry (configurable later)
        batch.setBatchStatus("ACTIVE");
        batch.setCreatedAt(LocalDateTime.now());
        pointsBatchRepo.save(batch);

        // 7. Update MemberAccountEntity
        MemberAccountEntity account = memberAccountRepo.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("Member account not found: " + memberId));
        account.setPointsBalance(account.getPointsBalance() + earnedPoints);
        account.setAvailablePoints(account.getAvailablePoints() + earnedPoints);
        memberAccountRepo.save(account);

        // 8. Write ledger entry
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

    private BigDecimal getTierMultiplier(Long merchantId, String tierCode) {
        return tierRuleRepo.findByMerchantIdOrderByTierLevelAsc(merchantId).stream()
                .filter(r -> r.getTierCode().equalsIgnoreCase(tierCode))
                .map(MemberTierRuleEntity::getPointsMultiplier)
                .findFirst()
                .orElse(BigDecimal.ONE);
    }
}
