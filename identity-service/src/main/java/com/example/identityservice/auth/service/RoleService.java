package com.example.identityservice.auth.service;

import com.example.identityservice.auth.audit.AuditWriter;
import com.example.identityservice.auth.audit.enums.AuditAction;
import com.example.identityservice.auth.entity.Permission;
import com.example.identityservice.auth.entity.Role;
import com.example.identityservice.auth.entity.RolePermission;
import com.example.identityservice.auth.entity.UserRole;
import com.example.identityservice.auth.enums.RoleCode;
import com.example.identityservice.auth.exception.SystemRoleModificationException;
import com.example.identityservice.auth.repository.PermissionRepository;
import com.example.identityservice.auth.repository.RolePermissionRepository;
import com.example.identityservice.auth.repository.RoleRepository;
import com.example.identityservice.auth.repository.UserRepository;
import com.example.identityservice.auth.repository.UserRoleRepository;
import com.example.identityservice.auth.security.AccessCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Manages the RBAC tables ({@link Role}, {@link UserRole}) and is the enforcement point every
 * other service calls through via {@link #checkAccess}. {@link Role#isSystem()} roles are
 * protected from deletion and renaming-by-code.
 */
@Service
public class RoleService {

    /** Upper bound on how long a {@code checkAccess} answer is trusted, absent a sooner grant expiry. */
    private static final Duration MAX_ACCESS_CACHE_TTL = Duration.ofSeconds(30);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final AuditWriter auditWriter;
    private final AccessCache accessCache;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRoleRepository userRoleRepository,
            UserRepository userRepository,
            AuditWriter auditWriter,
            AccessCache accessCache) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.auditWriter = auditWriter;
        this.accessCache = accessCache;
    }

    @Transactional
    public Role createRole(String code, String name, String description, Long actorUserId) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        Role saved = roleRepository.save(role);

        auditWriter.write(actorUserId, AuditAction.CREATE, "Role", saved.getId(), null,
                "{\"code\":\"" + saved.getCode() + "\",\"name\":\"" + saved.getName() + "\"}", "role_created");

        return saved;
    }

    /** Renaming the display name is always allowed; the {@code code} of a system role cannot change. */
    @Transactional
    public Role renameRole(Long roleId, String newName, String newCode, Long actorUserId) {
        Role role = roleRepository.findById(roleId).orElseThrow();
        if (role.isSystem() && newCode != null && !newCode.equals(role.getCode())) {
            throw new SystemRoleModificationException("Cannot change the code of a system role");
        }
        String beforeState = "{\"name\":\"" + role.getName() + "\",\"code\":\"" + role.getCode() + "\"}";
        role.setName(newName);
        if (newCode != null) {
            role.setCode(newCode);
        }
        Role saved = roleRepository.save(role);

        auditWriter.write(actorUserId, AuditAction.UPDATE, "Role", saved.getId(), beforeState,
                "{\"name\":\"" + saved.getName() + "\",\"code\":\"" + saved.getCode() + "\"}", "role_renamed");

        return saved;
    }

    /**
     * Deletes a role and its {@link RolePermission} grants. Existing {@link UserRole} grants for
     * the role are deactivated by expiry, matching how every other grant in this model is retired.
     */
    @Transactional
    public void deleteRole(Long roleId, Long actorUserId, String reason) {
        Role role = roleRepository.findById(roleId).orElseThrow();
        if (role.isSystem()) {
            throw new SystemRoleModificationException("Cannot delete a system role");
        }

        List<UserRole> grants = userRoleRepository.findByRoleId(roleId);
        Instant now = Instant.now();
        for (UserRole grant : grants) {
            grant.setExpiresAt(now);
        }
        userRoleRepository.saveAll(grants);

        rolePermissionRepository.deleteByRoleId(roleId);
        roleRepository.delete(role);

        auditWriter.write(actorUserId, AuditAction.PERMISSION_CHANGE, "Role", roleId, null, null, reason);
    }

    /** {@code expiresAt} may be null for a permanent grant, but if set it must be in the future. */
    @Transactional
    public UserRole grantRole(Long userId, Long roleId, Long grantedByUserId, Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
        userRepository.findById(userId).orElseThrow();
        roleRepository.findById(roleId).orElseThrow();

        Optional<UserRole> existing = userRoleRepository.findByUserIdAndRoleId(userId, roleId);
        UserRole grant = existing.filter(g -> g.getExpiresAt() == null || g.getExpiresAt().isAfter(Instant.now()))
                .orElseGet(UserRole::new);
        grant.setUserId(userId);
        grant.setRoleId(roleId);
        grant.setGrantedByUserId(grantedByUserId);
        grant.setGrantedAt(Instant.now());
        grant.setExpiresAt(expiresAt);
        UserRole saved = userRoleRepository.save(grant);

        auditWriter.write(grantedByUserId, AuditAction.PERMISSION_CHANGE, "UserRole", saved.getId(), null, null,
                "role_granted");
        accessCache.invalidate(userId);

        return saved;
    }

    /**
     * Deactivates the grant by expiring it now — the row is never deleted.
     *
     * <p>An admin cannot revoke their own {@code ADMIN} grant: the last admin must always be able
     * to act, so self-service must never be the path that leaves the platform without one.
     */
    @Transactional
    public void revokeRole(Long userId, Long roleId, Long actorUserId) {
        UserRole grant = userRoleRepository.findByUserIdAndRoleId(userId, roleId).orElseThrow();
        Role role = roleRepository.findById(roleId).orElseThrow();
        if (userId.equals(actorUserId) && RoleCode.ADMIN.equals(role.getCode())) {
            throw new SystemRoleModificationException("An admin cannot revoke their own ADMIN grant");
        }
        grant.setExpiresAt(Instant.now());
        userRoleRepository.save(grant);

        auditWriter.write(actorUserId, AuditAction.PERMISSION_CHANGE, "UserRole", grant.getId(), null, null,
                "role_revoked");
        accessCache.invalidate(userId);
    }

    /**
     * Unions permissions across every non-expired {@link UserRole} grant. A pure read: an expired
     * grant is simply skipped, with no cleanup job required.
     *
     * <p>Backed by {@link AccessCache}: a hit is trusted until the earliest of {@link
     * #MAX_ACCESS_CACHE_TTL} and the nearest grant's {@code expiresAt}, so a cached answer never
     * outlives the grant it was computed from. Granting or revoking a role for this user
     * invalidates its entries immediately regardless of TTL.
     */
    @Transactional(readOnly = true)
    public boolean checkAccess(Long userId, String permissionCode) {
        Optional<Boolean> cached = accessCache.get(userId, permissionCode);
        if (cached.isPresent()) {
            return cached.get();
        }

        Instant now = Instant.now();
        List<UserRole> grants = userRoleRepository.findByUserId(userId);

        Set<String> permissionCodes = new HashSet<>();
        Instant nearestExpiry = null;
        for (UserRole grant : grants) {
            if (grant.getExpiresAt() != null && !grant.getExpiresAt().isAfter(now)) {
                continue;
            }
            if (grant.getExpiresAt() != null && (nearestExpiry == null || grant.getExpiresAt().isBefore(nearestExpiry))) {
                nearestExpiry = grant.getExpiresAt();
            }
            for (RolePermission rolePermission : rolePermissionRepository.findByRoleId(grant.getRoleId())) {
                permissionRepository.findById(rolePermission.getPermissionId())
                        .map(Permission::getCode)
                        .ifPresent(permissionCodes::add);
            }
        }

        boolean allowed = permissionCodes.contains(permissionCode);
        Instant cacheExpiry = now.plus(MAX_ACCESS_CACHE_TTL);
        if (nearestExpiry != null && nearestExpiry.isBefore(cacheExpiry)) {
            cacheExpiry = nearestExpiry;
        }
        accessCache.put(userId, permissionCode, allowed, cacheExpiry);
        return allowed;
    }
}
