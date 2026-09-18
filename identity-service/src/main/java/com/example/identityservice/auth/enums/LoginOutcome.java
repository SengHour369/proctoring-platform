package com.example.identityservice.auth.enums;

/** Result of one authentication attempt. */
public enum LoginOutcome {
    SUCCESS,
    BAD_CREDENTIALS,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED,
    EMAIL_NOT_VERIFIED,
    TOKEN_EXPIRED,
    MFA_REQUIRED,
    MFA_FAILED
}
