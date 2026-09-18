package com.example.identityservice.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeClientRequest(@NotBlank String reason) {
}
