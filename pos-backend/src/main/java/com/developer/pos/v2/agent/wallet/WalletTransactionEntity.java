package com.developer.pos.v2.agent.wallet;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransactionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "transaction_id") private String transactionId;
    @Column(name = "wallet_id") private String walletId;
    @Column(name = "transaction_type") private String transactionType;
    @Column(name = "amount_cents") private long amountCents;
    @Column(name = "balance_after_cents") private long balanceAfterCents;
    @Column(name = "source_type") private String sourceType;
    @Column(name = "source_ref") private String sourceRef;
    @Column(name = "counterparty") private String counterparty;
    @Column(name = "description") private String description;
    @Column(name = "created_at") private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String v) { this.transactionId = v; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String v) { this.walletId = v; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String v) { this.transactionType = v; }
    public long getAmountCents() { return amountCents; }
    public void setAmountCents(long v) { this.amountCents = v; }
    public long getBalanceAfterCents() { return balanceAfterCents; }
    public void setBalanceAfterCents(long v) { this.balanceAfterCents = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String v) { this.sourceRef = v; }
    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String v) { this.counterparty = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
}
