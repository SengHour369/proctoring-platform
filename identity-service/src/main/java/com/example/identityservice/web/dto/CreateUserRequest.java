package com.example.identityservice.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Shared request body for {@code create*} across Student/Teacher/Reviewer/Admin. {@code groupId}
 * is honoured only by the student endpoint; {@code roleExpiresAt} only by the reviewer endpoint
 * (see {@link com.example.identityservice.user.config.RoleProfile}).
 */
public record CreateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String externalRef,
        String phoneNumber,
        String timeZone,
        String locale,
        Long groupId,
        Instant roleExpiresAt) {
}
