package com.developer.pos.v2.store.infrastructure.persistence.repository;

import com.developer.pos.v2.store.infrastructure.persistence.entity.QrTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JpaQrTokenRepository extends JpaRepository<QrTokenEntity, Long> {

    Optional<QrTokenEntity> findByStoreIdAndTableIdAndTokenAndTokenStatus(
            Long storeId, Long tableId, String token, String tokenStatus);

    @Modifying
    @Query("UPDATE QrTokenEntity q SET q.tokenStatus = 'EXPIRED' WHERE q.tableId = :tableId AND q.tokenStatus = 'ACTIVE'")
    int expireAllActiveByTableId(Long tableId);
}
