package com.developer.pos.v2.member.domain.policy;

/**
 * Auto-upgrade tier based on lifetime spend.
 * Tier ladder: STANDARD → SILVER → GOLD → VIP
 */
public final class MemberTierPolicy {

    private MemberTierPolicy() {}

    private static final long SILVER_THRESHOLD_CENTS = 500_00L;   // $500
    private static final long GOLD_THRESHOLD_CENTS   = 2000_00L;  // $2,000
    private static final long VIP_THRESHOLD_CENTS    = 5000_00L;  // $5,000

    public static String evaluate(long lifetimeSpendCents) {
        if (lifetimeSpendCents >= VIP_THRESHOLD_CENTS) return "VIP";
        if (lifetimeSpendCents >= GOLD_THRESHOLD_CENTS) return "GOLD";
        if (lifetimeSpendCents >= SILVER_THRESHOLD_CENTS) return "SILVER";
        return "STANDARD";
    }

    /**
     * Points earned per settlement.
     * 1 point per $1 spent (100 cents).
     */
    public static long calculatePointsEarned(long payableAmountCents) {
        return payableAmountCents / 100;
    }
}
