package com.example.proctoringservice.proctoring.enums;

/** State of the supervision session wrapping an attempt. */
public enum ProctoringSessionStatus {
    PENDING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    TERMINATED,
    FAILED
}
