package com.example.identityservice.user.config;

/**
 * The knobs that differ between Student/Teacher/Reviewer/Admin management, per the "shared
 * management contract" — everything else (create/update/disable/profile shape, invitation flow,
 * session revocation, audit writes) is identical and lives once in
 * {@link com.example.identityservice.user.service.UserLifecycleService}.
 *
 * @param roleCode                 the {@link com.example.identityservice.auth.entity.Role#getCode()} granted on create
 * @param createPermission         e.g. {@code "student:create"}
 * @param updatePermission         e.g. {@code "student:update"}
 * @param disablePermission        e.g. {@code "student:disable"}
 * @param viewPermission           e.g. {@code "student:view"}
 * @param selfUpdatePermission     lets the subject edit their own profile, e.g. {@code "profile:update_own"}
 * @param selfViewPermission       lets the subject view their own profile, e.g. {@code "profile:view_own"}
 * @param supportsGroupMembership  true for STUDENT only — the create command's {@code groupId} is honoured
 * @param grantsExpireByDefault    true for REVIEWER — a grant with no caller-supplied expiry still gets one
 * @param permanentGrantOverride   permission that lets a caller skip the default expiry, e.g. {@code "reviewer:grant_permanent"}
 * @param blockSelfDisable         true for TEACHER and ADMIN — the subject cannot disable their own account
 * @param requireSecondApprover    true for ADMIN — create and self-email-change need a second admin's id
 * @param terminatesAttemptsOnDisable  true for STUDENT — disabling ends any in-flight exam attempt
 * @param releasesReviewCasesOnDisable true for REVIEWER — disabling returns assigned cases to the open queue
 */
public record RoleProfile(
        String roleCode,
        String createPermission,
        String updatePermission,
        String disablePermission,
        String viewPermission,
        String selfUpdatePermission,
        String selfViewPermission,
        boolean supportsGroupMembership,
        boolean grantsExpireByDefault,
        String permanentGrantOverride,
        boolean blockSelfDisable,
        boolean requireSecondApprover,
        boolean terminatesAttemptsOnDisable,
        boolean releasesReviewCasesOnDisable) {
}
