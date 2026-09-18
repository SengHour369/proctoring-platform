package com.example.identityservice.web.dto;

import com.example.identityservice.auth.enums.MfaMethod;
import jakarta.validation.constraints.NotNull;

public record EnrollMfaRequest(@NotNull Long userId, @NotNull MfaMethod method) {
}
