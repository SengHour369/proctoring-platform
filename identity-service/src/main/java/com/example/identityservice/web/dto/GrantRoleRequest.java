package com.example.identityservice.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record GrantRoleRequest(@NotNull Long userId, @NotNull Long roleId, Instant expiresAt) {
}
