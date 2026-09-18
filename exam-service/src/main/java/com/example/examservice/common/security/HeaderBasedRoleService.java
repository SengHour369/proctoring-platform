package com.example.examservice.common.security;

import org.springframework.stereotype.Service;

/**
 * Checks the permission set the gateway attached to the current request. This is a stand-in for a
 * real call into {@code identity-service}'s {@code RoleService} (no shared auth client exists
 * across services yet — see {@link CallerContext}); the {@link RoleService} interface is the seam
 * to swap that in later without touching any of the exam/question services that depend on it.
 */
@Service
public class HeaderBasedRoleService implements RoleService {

    @Override
    public boolean checkAccess(Long userId, String permissionCode) {
        CallerContext context = CallerContextHolder.get();
        if (context == null || context.userId() == null || userId == null) {
            return false;
        }
        if (!context.userId().equals(userId)) {
            return false;
        }
        return context.hasPermission(permissionCode);
    }
}
