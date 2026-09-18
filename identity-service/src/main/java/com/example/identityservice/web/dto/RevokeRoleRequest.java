package com.example.identityservice.web.dto;

import jakarta.validation.constraints.NotNull;

public record RevokeRoleRequest(@NotNull Long userId, @NotNull Long roleId) {
}
