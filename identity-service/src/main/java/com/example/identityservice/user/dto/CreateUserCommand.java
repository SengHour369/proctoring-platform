package com.example.identityservice.user.dto;

import java.time.Instant;

/**
 * Input to {@code create*} across Student/Teacher/Reviewer/Admin — the shared shape from
 * §2.1.1's steps 2-3.
 *
 * @param groupId       student-only; other role families ignore it
 * @param roleExpiresAt reviewer-only grant expiry; ignored unless the profile is time-boxed by
 *                       default, in which case a caller without the permanent-grant override must
 *                       supply one
 */
public record CreateUserCommand(
        String fullName,
        String email,
        String externalRef,
        String phoneNumber,
        String timeZone,
        String locale,
        Long groupId,
        Instant roleExpiresAt) {
}
