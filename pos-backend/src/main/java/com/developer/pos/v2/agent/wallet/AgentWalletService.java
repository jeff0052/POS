package com.developer.pos.v2.agent.wallet;

import com.developer.pos.v2.common.application.UseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AgentWalletService implements UseCase {

    private final JpaAgentWalletRepository walletRepository;
    private final JpaWalletTransactionRepository transactionRepository;

    public AgentWalletService(JpaAgentWalletRepository walletRepository,
                              JpaWalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public AgentWalletEntity getWallet(String agentId) {
        return walletRepository.findByAgentId(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for agent: " + agentId));
    }

    @Transactional
    public WalletTransactionEntity recordIncome(String agentId, long amountCents,
                                                 String sourceType, String sourceRef, String counterparty, String description) {
        AgentWalletEntity wallet = getWallet(agentId);
        wallet.setBalanceCents(wallet.getBalanceCents() + amountCents);
        wallet.setTotalIncomeCents(wallet.getTotalIncomeCents() + amountCents);
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepository.save(wallet);

        return saveTransaction(wallet.getWalletId(), "INCOME", amountCents,
                wallet.getBalanceCents(), sourceType, sourceRef, counterparty, description);
    }

    @Transactional
    public WalletTransactionEntity recordExpense(String agentId, long amountCents,
                                                  String sourceType, String sourceRef, String counterparty, String description) {
        AgentWalletEntity wallet = getWallet(agentId);
        if (wallet.getBalanceCents() < amountCents) {
            throw new IllegalStateException("Insufficient wallet balance. Available: " + wallet.getBalanceCents());
        }
        wallet.setBalanceCents(wallet.getBalanceCents() - amountCents);
        wallet.setTotalExpenseCents(wallet.getTotalExpenseCents() + amountCents);
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepository.save(wallet);

        return saveTransaction(wallet.getWalletId(), "EXPENSE", amountCents,
                wallet.getBalanceCents(), sourceType, sourceRef, counterparty, description);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionEntity> getTransactions(String agentId, int page, int size) {
        AgentWalletEntity wallet = getWallet(agentId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId(), PageRequest.of(page, size));
    }

    private WalletTransactionEntity saveTransaction(String walletId, String type, long amount,
                                                     long balanceAfter, String sourceType, String sourceRef,
                                                     String counterparty, String description) {
        WalletTransactionEntity tx = new WalletTransactionEntity();
        tx.setTransactionId("TX" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        tx.setWalletId(walletId);
        tx.setTransactionType(type);
        tx.setAmountCents(amount);
        tx.setBalanceAfterCents(balanceAfter);
        tx.setSourceType(sourceType);
        tx.setSourceRef(sourceRef);
        tx.setCounterparty(counterparty);
        tx.setDescription(description);
        tx.setCreatedAt(OffsetDateTime.now());
        return transactionRepository.save(tx);
    }
}
