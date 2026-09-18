package com.example.identityservice.web.dto;

import jakarta.validation.constraints.NotNull;

public record GrantPermissionRequest(@NotNull Long permissionId) {
}
