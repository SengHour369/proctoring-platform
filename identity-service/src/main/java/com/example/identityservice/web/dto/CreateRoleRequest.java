package com.example.identityservice.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description) {
}
