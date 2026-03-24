package com.developer.pos.v2.member.domain.policy;

public final class MemberDiscountPolicy {

    private MemberDiscountPolicy() {
    }

    public static long calculate(long originalAmountCents, String tierCode) {
        if (originalAmountCents <= 0 || tierCode == null || tierCode.isBlank()) {
            return 0;
        }

        int discountBasisPoints = switch (tierCode.trim().toUpperCase()) {
            case "GOLD" -> 1000;
            case "SILVER" -> 500;
            case "VIP" -> 1500;
            default -> 0;
        };

        return (originalAmountCents * discountBasisPoints) / 10_000;
    }
}
