package com.example.identityservice.web.dto;

import com.example.identityservice.user.enums.DisableMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DisableUserRequest(@NotNull DisableMode mode, @NotBlank String reason) {
}
