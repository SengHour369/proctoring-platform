package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record GrantWindowOverrideRequest(
        @NotNull Instant overriddenWindowStartAt,
        @NotNull Instant overriddenWindowEndAt,
        @NotBlank String reason,
        @NotBlank String justificationRef,
        Instant expiresAt
) {
}
