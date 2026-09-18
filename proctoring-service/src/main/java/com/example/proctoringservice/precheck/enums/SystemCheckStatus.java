package com.example.proctoringservice.precheck.enums;

/** Overall verdict of a pre-flight check run. */
public enum SystemCheckStatus {
    IN_PROGRESS,
    PASSED,
    PASSED_WITH_WARNINGS,
    FAILED,
    EXPIRED
}
