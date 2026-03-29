package com.developer.pos.v2.common.application;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class StoreAccessEnforcer {

    private final JpaStoreLookupRepository storeLookupRepository;

    public StoreAccessEnforcer(JpaStoreLookupRepository storeLookupRepository) {
        this.storeLookupRepository = storeLookupRepository;
    }

    /**
     * 校验当前 actor 对 storeId 的访问权限：
     * 1. store 属于 actor.merchantId
     * 2. actor 的 accessibleStoreIds 包含该 storeId（若非空）
     */
    public void enforce(Long storeId) {
        AuthenticatedActor actor = AuthContext.current();
        if (actor.merchantId() == null || actor.merchantId() == 0L) return;
        StoreEntity store = storeLookupRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        if (!Objects.equals(store.getMerchantId(), actor.merchantId())) {
            throw new SecurityException("Store does not belong to your merchant");
        }
        if (actor.accessibleStoreIds() != null && !actor.accessibleStoreIds().isEmpty()
                && !actor.accessibleStoreIds().contains(storeId)) {
            throw new SecurityException("You do not have access to store: " + storeId);
        }
    }
}
