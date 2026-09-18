package com.example.identityservice.user.enums;

/**
 * The two ways {@code disableStudent}/{@code disableTeacher}/{@code disableReviewer}/
 * {@code disableAdmin} can deactivate an account. Deliberately not reusing every value of
 * {@link com.example.identityservice.auth.enums.UserStatus} — {@code PENDING_VERIFICATION} and
 * {@code ACTIVE} are not things a disable call can set.
 */
public enum DisableMode {

    /** Temporary, reversible — investigation, a policy hold, a pending review. */
    SUSPENDED,

    /** Permanent by default — the student left the institution, the employee was terminated. */
    DISABLED
}
