package com.example.test.identity.enums;

/** State of an identity verification attempt. */
public enum VerificationStatus {
    PENDING,
    IN_PROGRESS,
    PASSED,
    FAILED,
    MANUAL_OVERRIDE,
    EXPIRED
}
