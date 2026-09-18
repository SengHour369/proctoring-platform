package com.example.identityservice.auth.enums;

/**
 * The four seeded {@link com.example.identityservice.auth.entity.Role#getCode()} values every
 * user-management operation grants or checks against. Roles are still data — an operator can add
 * a fifth via {@code RoleController} without a redeploy — but these four are the ones the
 * platform's own onboarding flows know about by name.
 */
public final class RoleCode {

    public static final String STUDENT = "STUDENT";
    public static final String TEACHER = "TEACHER";
    public static final String REVIEWER = "REVIEWER";
    public static final String ADMIN = "ADMIN";

    private RoleCode() {
    }
}
