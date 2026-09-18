package com.example.test.auth.enums;

/** Second factor a user has enrolled in, when MFA is turned on. */
public enum MfaMethod {
    TOTP,
    SMS,
    EMAIL
}
