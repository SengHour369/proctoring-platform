package com.example.proctoringservice.identity.enums;

/** State of an identity verification attempt. */
public enum VerificationStatus {
    PENDING,
    IN_PROGRESS,
    PASSED,
    FAILED,
    MANUAL_OVERRIDE,
    EXPIRED
}
