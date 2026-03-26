package com.developer.pos.v2.agent.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaWalletTransactionRepository extends JpaRepository<WalletTransactionEntity, Long> {
    Page<WalletTransactionEntity> findByWalletIdOrderByCreatedAtDesc(String walletId, Pageable pageable);
}
