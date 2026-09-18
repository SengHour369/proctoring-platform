package com.example.identityservice.web.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code code} is optional — a system role rejects a code change but accepts a new display name. */
public record RenameRoleRequest(@NotBlank String name, String code) {
}
