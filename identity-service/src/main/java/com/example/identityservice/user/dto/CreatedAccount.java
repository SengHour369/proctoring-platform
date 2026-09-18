package com.example.identityservice.user.dto;

import com.example.identityservice.auth.enums.UserStatus;

import java.util.UUID;

/** What a create call returns — never the internal id, never the placeholder password hash. */
public record CreatedAccount(UUID publicId, String email, UserStatus status) {
}
