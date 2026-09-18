package com.example.identityservice.web;

import com.example.identityservice.auth.entity.Role;
import com.example.identityservice.auth.entity.UserRole;
import com.example.identityservice.auth.service.RoleService;
import com.example.identityservice.web.dto.CheckAccessResponse;
import com.example.identityservice.web.dto.CreateRoleRequest;
import com.example.identityservice.web.dto.GrantRoleRequest;
import com.example.identityservice.web.dto.RenameRoleRequest;
import com.example.identityservice.web.dto.RevokeRoleRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin surface over RBAC. {@code X-Actor-User-Id} stands in for the authenticated caller until a
 * JWT resource filter is wired into {@link SecurityConfig} — see that class for why.
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<Role> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        Role role = roleService.createRole(request.code(), request.name(), request.description(), actorUserId);
        return ResponseEntity.ok(role);
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<Role> renameRole(
            @PathVariable Long roleId,
            @Valid @RequestBody RenameRoleRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        Role role = roleService.renameRole(roleId, request.name(), request.code(), actorUserId);
        return ResponseEntity.ok(role);
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable Long roleId,
            @RequestHeader("X-Actor-User-Id") Long actorUserId,
            @RequestParam String reason) {
        roleService.deleteRole(roleId, actorUserId, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/grants")
    public ResponseEntity<UserRole> grantRole(
            @Valid @RequestBody GrantRoleRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        UserRole grant = roleService.grantRole(request.userId(), request.roleId(), actorUserId, request.expiresAt());
        return ResponseEntity.ok(grant);
    }

    @PostMapping("/grants/revoke")
    public ResponseEntity<Void> revokeRole(
            @Valid @RequestBody RevokeRoleRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        roleService.revokeRole(request.userId(), request.roleId(), actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-access")
    public ResponseEntity<CheckAccessResponse> checkAccess(
            @RequestParam Long userId,
            @RequestParam String permissionCode) {
        boolean allowed = roleService.checkAccess(userId, permissionCode);
        return ResponseEntity.ok(new CheckAccessResponse(allowed));
    }
}
