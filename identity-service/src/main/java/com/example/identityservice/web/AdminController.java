package com.example.identityservice.web;

import com.example.identityservice.user.dto.CreatedAccount;
import com.example.identityservice.user.dto.UserProfileView;
import com.example.identityservice.user.service.AdminService;
import com.example.identityservice.web.dto.CreateUserRequest;
import com.example.identityservice.web.dto.DisableUserRequest;
import com.example.identityservice.web.dto.UpdateUserRequest;
import com.example.identityservice.web.dto.mapper.UserRequestMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * §2.4 Admin Management. {@code X-Confirming-Admin-Id} carries the second admin's id required by
 * dual approval on create, and on a self-service email change.
 */
@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private final AdminService adminService;
    private final RequestContextResolver requestContextResolver;

    public AdminController(AdminService adminService, RequestContextResolver requestContextResolver) {
        this.adminService = adminService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping
    public ResponseEntity<CreatedAccount> createAdmin(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId,
            @RequestHeader(value = "X-Confirming-Admin-Id", required = false) Long confirmingAdminUserId,
            HttpServletRequest httpRequest) {
        CreatedAccount account = adminService.createAdmin(
                UserRequestMapper.toCommand(request), actorUserId, confirmingAdminUserId,
                requestContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateAdmin(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId,
            @RequestHeader(value = "X-Confirming-Admin-Id", required = false) Long confirmingAdminUserId,
            HttpServletRequest httpRequest) {
        adminService.updateAdmin(
                userId, UserRequestMapper.toCommand(request), actorUserId, confirmingAdminUserId,
                requestContextResolver.resolve(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/disable")
    public ResponseEntity<Void> disableAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody DisableUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        adminService.disableAdmin(userId, request.mode(), request.reason(), actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileView> getProfile(
            @PathVariable Long userId,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ResponseEntity.ok(adminService.getProfile(userId, actorUserId));
    }
}
