package com.example.test.attempt.enums;

/** Why a candidate's session had to be re-established mid-attempt. */
public enum ResumptionReason {
    BROWSER_CRASH,
    NETWORK_DROP,
    DEVICE_REBOOT,
    POWER_LOSS,
    PROCTOR_INITIATED
}
