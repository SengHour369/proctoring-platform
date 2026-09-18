package com.example.identityservice.web;

import com.example.identityservice.auth.service.PermissionService;
import com.example.identityservice.web.dto.GrantPermissionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code X-Actor-User-Id} stands in for the authenticated caller — see {@link SecurityConfig}. */
@RestController
@RequestMapping("/api/roles/{roleId}/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    public ResponseEntity<Void> grantPermission(
            @PathVariable Long roleId,
            @Valid @RequestBody GrantPermissionRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        permissionService.grantPermission(roleId, request.permissionId(), actorUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> revokePermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        permissionService.revokePermission(roleId, permissionId, actorUserId);
        return ResponseEntity.noContent().build();
    }
}
