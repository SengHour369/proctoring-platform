package com.example.identityservice.auth.service;

import com.example.identityservice.auth.audit.entity.AuditLog;
import com.example.identityservice.auth.audit.enums.AuditAction;
import com.example.identityservice.auth.audit.repository.AuditLogRepository;
import com.example.identityservice.auth.entity.RolePermission;
import com.example.identityservice.auth.repository.PermissionRepository;
import com.example.identityservice.auth.repository.RolePermissionRepository;
import com.example.identityservice.auth.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Manages {@link RolePermission} grants — pure grants with no lifecycle of their own. */
@Service
public class PermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditLogRepository auditLogRepository;

    public PermissionService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            AuditLogRepository auditLogRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /** Idempotent: granting an already-granted permission is a no-op, not an error. */
    @Transactional
    public void grantPermission(Long roleId, Long permissionId, Long grantedByUserId) {
        roleRepository.findById(roleId).orElseThrow();
        permissionRepository.findById(permissionId).orElseThrow();

        Optional<RolePermission> existing = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        if (existing.isPresent()) {
            return;
        }

        RolePermission grant = new RolePermission();
        grant.setRoleId(roleId);
        grant.setPermissionId(permissionId);
        grant.setGrantedByUserId(grantedByUserId);
        grant.setGrantedAt(Instant.now());
        rolePermissionRepository.save(grant);

        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(grantedByUserId);
        auditLog.setAction(AuditAction.PERMISSION_CHANGE);
        auditLog.setEntityType("RolePermission");
        auditLog.setEntityId(grant.getId());
        auditLog.setReason("permission_granted");
        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void revokePermission(Long roleId, Long permissionId, Long actorUserId) {
        roleRepository.findById(roleId).orElseThrow();
        permissionRepository.findById(permissionId).orElseThrow();

        RolePermission grant = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseThrow();
        rolePermissionRepository.delete(grant);

        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(AuditAction.PERMISSION_CHANGE);
        auditLog.setEntityType("RolePermission");
        auditLog.setEntityId(grant.getId());
        auditLog.setReason("permission_revoked");
        auditLogRepository.save(auditLog);
    }
}
