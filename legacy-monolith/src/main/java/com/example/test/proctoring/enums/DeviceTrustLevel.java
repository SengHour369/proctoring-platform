package com.example.test.proctoring.enums;

/** Standing of one device fingerprint against one exam, built up over sightings. */
public enum DeviceTrustLevel {
    UNKNOWN,
    KNOWN,
    SHARED_SUSPECTED,
    SHARED_CONFIRMED,
    BLOCKED
}
