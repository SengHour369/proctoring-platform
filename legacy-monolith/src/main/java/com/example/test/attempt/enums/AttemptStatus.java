package com.example.test.attempt.enums;

/** State machine of a single sitting. */
public enum AttemptStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    SUBMITTED,
    AUTO_SUBMITTED,
    ABANDONED,
    EXPIRED,
    INVALIDATED,
    GRADED
}
