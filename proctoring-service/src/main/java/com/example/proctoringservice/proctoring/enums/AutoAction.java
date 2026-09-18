package com.example.proctoringservice.proctoring.enums;

/** Automated response the platform applied when the event fired. */
public enum AutoAction {
    NONE,
    LOG_ONLY,
    WARN_CANDIDATE,
    PAUSE_ATTEMPT,
    LOCK_SCREEN,
    NOTIFY_PROCTOR,
    TERMINATE_ATTEMPT
}
