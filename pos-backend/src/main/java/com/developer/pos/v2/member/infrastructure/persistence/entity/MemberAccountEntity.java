package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2MemberAccountEntity")
@Table(name = "member_accounts")
public class MemberAccountEntity {

    @Id
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "points_balance", nullable = false)
    private long pointsBalance;

    @Column(name = "cash_balance_cents", nullable = false)
    private long cashBalanceCents;

    @Column(name = "lifetime_spend_cents", nullable = false)
    private long lifetimeSpendCents;

    @Column(name = "lifetime_recharge_cents", nullable = false)
    private long lifetimeRechargeCents;

    protected MemberAccountEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public long getPointsBalance() {
        return pointsBalance;
    }

    public long getCashBalanceCents() {
        return cashBalanceCents;
    }

    public long getLifetimeSpendCents() {
        return lifetimeSpendCents;
    }

    public long getLifetimeRechargeCents() {
        return lifetimeRechargeCents;
    }
}
