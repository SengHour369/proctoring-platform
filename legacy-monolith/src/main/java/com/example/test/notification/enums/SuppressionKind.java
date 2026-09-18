package com.example.test.notification.enums;

/** The mechanism by which a notification rule holds back or blocks a send. */
public enum SuppressionKind {
    QUIET_HOURS,
    OPT_OUT,
    RATE_LIMIT,
    DUPLICATE_WINDOW
}
