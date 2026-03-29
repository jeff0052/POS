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
     * Validates the current actor can access the given store.
     * SUPER_ADMIN actors (merchantId null/0) bypass merchant isolation by design.
     */
    public void enforce(Long storeId) {
        AuthenticatedActor actor = AuthContext.current();
        if (actor.merchantId() != null && actor.merchantId() != 0L) {
            StoreEntity store = storeLookupRepository.findById(storeId)
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
            if (!Objects.equals(store.getMerchantId(), actor.merchantId())) {
                throw new SecurityException("Store does not belong to your merchant");
            }
            if (actor.accessibleStoreIds() != null && !actor.accessibleStoreIds().isEmpty()
                    && !actor.hasStoreAccess(storeId)) {
                throw new SecurityException("You do not have access to store: " + storeId);
            }
        }
    }

    public void enforcePermission(String permissionCode) {
        AuthenticatedActor actor = AuthContext.current();
        if (!actor.hasPermission(permissionCode)) {
            throw new SecurityException("Missing permission: " + permissionCode);
        }
    }
}
