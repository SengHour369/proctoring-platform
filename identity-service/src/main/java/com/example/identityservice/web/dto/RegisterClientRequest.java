package com.example.identityservice.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterClientRequest(
        @NotBlank String name,
        @NotBlank String allowedScopes,
        Integer rateLimitPerMinute,
        String allowedIpRanges) {
}
