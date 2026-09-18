package com.example.examservice.common.security;

import java.util.Set;

/**
 * The authenticated caller for the current request thread.
 *
 * <p>There is no shared auth client across services yet — the API gateway does not verify JWTs or
 * inject identity headers, and {@code identity-service}'s {@code RoleService.checkAccess} is
 * in-process only. Until that's wired up, exam-service trusts an upstream gateway to have set
 * {@code X-User-Id} and {@code X-User-Permissions} the same way it will once real verification
 * lands, so swapping this out later is a matter of replacing {@link CallerContextFilter}, not the
 * call sites that use {@link RoleService}.
 */
public record CallerContext(Long userId, Set<String> permissions) {

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
