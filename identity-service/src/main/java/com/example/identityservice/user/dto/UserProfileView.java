package com.example.identityservice.user.dto;

import com.example.identityservice.auth.enums.MfaMethod;
import com.example.identityservice.auth.enums.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * §2.1.4's read-only aggregation. {@code passwordHash} and {@code mfaSecretEncrypted} never
 * appear here by construction — there is simply no field for them.
 */
public record UserProfileView(
        PersonalInfo personalInfo,
        List<GroupMembershipView> groupMemberships,
        List<RoleGrantView> roleGrants,
        List<LoginAttemptView> recentLoginAttempts,
        List<SessionView> recentSessions,
        List<ActivityEntry> activityTrail) {

    public record PersonalInfo(
            UUID publicId,
            String fullName,
            String email,
            String externalRef,
            String phoneNumber,
            String timeZone,
            String locale,
            UserStatus status,
            Instant emailVerifiedAt,
            Instant lastLoginAt,
            boolean mfaEnabled,
            MfaMethod mfaMethod,
            Instant mfaEnrolledAt,
            String enrolmentPhotoPath,
            String voiceprintPath,
            /** Only populated for a caller holding the sensitive-field view permission; null otherwise. */
            Integer failedLoginCount,
            Instant lockedUntil) {
    }

    public record GroupMembershipView(Long studentGroupId, String groupCode, String groupName,
                                       boolean active, Instant joinedAt, Instant leftAt) {
    }

    public record RoleGrantView(String roleCode, Instant grantedAt, Instant expiresAt, boolean active) {
    }

    public record LoginAttemptView(Instant attemptedAt, String outcome, String ipAddress, String geoCountry) {
    }

    public record SessionView(Instant issuedAt, Instant lastSeenAt, Instant revokedAt, String revokedReason) {
    }
}
