package com.developer.pos.v2.rbac.application.service;

import com.developer.pos.v2.rbac.application.dto.ResolvedPermissions;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PermissionCacheService {

    private static final long TTL_SECONDS = 300; // 5 minutes

    private final PermissionResolver permissionResolver;
    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    public PermissionCacheService(PermissionResolver permissionResolver) {
        this.permissionResolver = permissionResolver;
    }

    public ResolvedPermissions resolve(Long userId) {
        CacheEntry entry = cache.get(userId);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        ResolvedPermissions resolved = permissionResolver.resolve(userId);
        cache.put(userId, new CacheEntry(resolved));
        return resolved;
    }

    public void evict(Long userId) {
        cache.remove(userId);
    }

    public void evictAll() {
        cache.clear();
    }

    private static class CacheEntry {
        final ResolvedPermissions value;
        final Instant createdAt;

        CacheEntry(ResolvedPermissions value) {
            this.value = value;
            this.createdAt = Instant.now();
        }

        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(TTL_SECONDS));
        }
    }
}
