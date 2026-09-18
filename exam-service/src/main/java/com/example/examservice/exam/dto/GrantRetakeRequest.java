package com.example.examservice.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record GrantRetakeRequest(
        @NotNull Long reviewDecisionId,
        @Min(1) int additionalAttempts,
        @NotBlank String reason,
        Instant expiresAt
) {
}
