package com.example.identityservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyMfaRequest(
        @NotNull Long userId,
        @NotBlank String code) {
}
