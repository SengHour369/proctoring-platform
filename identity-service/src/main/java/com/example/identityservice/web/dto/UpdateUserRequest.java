package com.example.identityservice.web.dto;

/** Shared request body for {@code update*}. Every field is optional; {@code null} means unchanged. */
public record UpdateUserRequest(
        String fullName,
        String phoneNumber,
        String timeZone,
        String locale,
        String enrolmentPhotoPath,
        String voiceprintPath,
        String email,
        String externalRef,
        String reason) {
}
