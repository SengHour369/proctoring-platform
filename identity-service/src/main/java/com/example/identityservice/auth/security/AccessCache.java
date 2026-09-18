package com.example.identityservice.auth.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived cache for {@code RoleService.checkAccess} results, keyed by {@code (userId,
 * permissionCode)}. An entry's TTL never outlives {@code expiresAt}, so a lookup for a permission
 * granted through a soon-to-expire {@link com.example.identityservice.auth.entity.UserRole} stops
 * being trusted the moment that grant would. Entries are also invalidated eagerly on any
 * grant/revoke for the affected user, so staleness in practice never exceeds the TTL — it exists
 * only to absorb read bursts between writes, not as the only defense against a stale answer.
 */
@Component
public class AccessCache {

    private record Entry(boolean allowed, Instant expiresAt) {
    }

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public Optional<Boolean> get(Long userId, String permissionCode) {
        Entry entry = cache.get(key(userId, permissionCode));
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.allowed());
    }

    public void put(Long userId, String permissionCode, boolean allowed, Instant expiresAt) {
        cache.put(key(userId, permissionCode), new Entry(allowed, expiresAt));
    }

    /** Called whenever a user's grants change, so a stale answer never survives a write. */
    public void invalidate(Long userId) {
        cache.keySet().removeIf(k -> k.startsWith(userId + ":"));
    }

    private String key(Long userId, String permissionCode) {
        return userId + ":" + permissionCode;
    }
}
