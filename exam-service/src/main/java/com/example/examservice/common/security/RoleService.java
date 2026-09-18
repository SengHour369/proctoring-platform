package com.example.examservice.common.security;

/**
 * The single enforcement point every write path in exam-service calls through before mutating
 * anything — mirrors {@code identity-service}'s {@code RoleService.checkAccess(userId, permissionCode)}.
 * Every rejection here is expected to be paired with an {@code AuditLog} row by the caller.
 */
public interface RoleService {

    boolean checkAccess(Long userId, String permissionCode);
}
