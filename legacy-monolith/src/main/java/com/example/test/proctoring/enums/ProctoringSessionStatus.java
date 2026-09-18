package com.example.test.proctoring.enums;

/** State of the supervision session wrapping an attempt. */
public enum ProctoringSessionStatus {
    PENDING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    TERMINATED,
    FAILED
}
