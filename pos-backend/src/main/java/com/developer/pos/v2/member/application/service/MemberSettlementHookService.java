package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.member.domain.policy.MemberTierPolicy;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberPointsLedgerEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Called after a settlement completes when a member is attached to the order.
 * Handles: lifetime spend tracking, auto points earning, tier auto-upgrade, balance deduction.
 */
@Service
public class MemberSettlementHookService implements UseCase {

    private final JpaMemberRepository memberRepository;
    private final JpaMemberAccountRepository accountRepository;
    private final JpaMemberPointsLedgerRepository pointsLedgerRepository;

    public MemberSettlementHookService(JpaMemberRepository memberRepository,
                                       JpaMemberAccountRepository accountRepository,
                                       JpaMemberPointsLedgerRepository pointsLedgerRepository) {
        this.memberRepository = memberRepository;
        this.accountRepository = accountRepository;
        this.pointsLedgerRepository = pointsLedgerRepository;
    }

    /**
     * Post-settlement hook: update member account after payment.
     * @param memberId the member
     * @param payableAmountCents amount settled
     * @param settlementNo reference for audit
     * @param paymentMethod CASH / QR / CARD / MEMBER_BALANCE
     */
    @Transactional
    public void onSettlementCompleted(Long memberId, long payableAmountCents, String settlementNo, String paymentMethod) {
        if (memberId == null || payableAmountCents <= 0) return;

        MemberEntity member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        MemberAccountEntity account = accountRepository.findByMemberId(memberId).orElse(null);
        if (account == null) return;

        // 1. Track lifetime spend
        account.setLifetimeSpendCents(account.getLifetimeSpendCents() + payableAmountCents);

        // 2. If paying with member balance, deduct
        if ("MEMBER_BALANCE".equals(paymentMethod)) {
            if (account.getCashBalanceCents() < payableAmountCents) {
                throw new IllegalStateException("Insufficient member balance. Available: " + account.getCashBalanceCents() + ", required: " + payableAmountCents);
            }
            account.setCashBalanceCents(account.getCashBalanceCents() - payableAmountCents);
        }

        // 3. Auto-earn points (1 point per $1)
        long pointsEarned = MemberTierPolicy.calculatePointsEarned(payableAmountCents);
        if (pointsEarned > 0) {
            account.setPointsBalance(account.getPointsBalance() + pointsEarned);

            MemberPointsLedgerEntity ledger = new MemberPointsLedgerEntity();
            ledger.setLedgerNo("PL" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            ledger.setMemberId(memberId);
            ledger.setMerchantId(member.getMerchantId());
            ledger.setChangeType("EARN");
            ledger.setPointsDelta(pointsEarned);
            ledger.setBalanceAfter(account.getPointsBalance());
            ledger.setSourceType("SETTLEMENT");
            ledger.setSourceRef(settlementNo);
            ledger.setOperatorName("SYSTEM");
            pointsLedgerRepository.save(ledger);
        }

        accountRepository.save(account);

        // 4. Auto-upgrade tier (never downgrade)
        String newTier = MemberTierPolicy.evaluate(account.getLifetimeSpendCents());
        if (tierRank(newTier) > tierRank(member.getTierCode())) {
            member.setTierCode(newTier);
            memberRepository.save(member);
        }
    }

    private static final Map<String, Integer> TIER_RANKS = Map.of(
            "STANDARD", 0, "SILVER", 1, "GOLD", 2, "VIP", 3
    );

    private int tierRank(String tierCode) {
        return TIER_RANKS.getOrDefault(tierCode == null ? "" : tierCode.toUpperCase(), 0);
    }
}
