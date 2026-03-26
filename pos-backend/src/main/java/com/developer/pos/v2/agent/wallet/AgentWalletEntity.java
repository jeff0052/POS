package com.developer.pos.v2.agent.wallet;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "agent_wallets")
public class AgentWalletEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "wallet_id") private String walletId;
    @Column(name = "agent_id") private String agentId;
    @Column(name = "balance_cents") private long balanceCents;
    @Column(name = "total_income_cents") private long totalIncomeCents;
    @Column(name = "total_expense_cents") private long totalExpenseCents;
    @Column(name = "currency_code") private String currencyCode;
    @Column(name = "wallet_status") private String walletStatus;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String v) { this.walletId = v; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String v) { this.agentId = v; }
    public long getBalanceCents() { return balanceCents; }
    public void setBalanceCents(long v) { this.balanceCents = v; }
    public long getTotalIncomeCents() { return totalIncomeCents; }
    public void setTotalIncomeCents(long v) { this.totalIncomeCents = v; }
    public long getTotalExpenseCents() { return totalExpenseCents; }
    public void setTotalExpenseCents(long v) { this.totalExpenseCents = v; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String v) { this.currencyCode = v; }
    public String getWalletStatus() { return walletStatus; }
    public void setWalletStatus(String v) { this.walletStatus = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
}
