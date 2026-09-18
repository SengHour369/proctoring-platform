package com.example.test.proctoring.enums;

/** One step in an evidence file's chain of custody, from capture to purge. */
public enum CustodyTransition {
    CAPTURED,
    HASHED,
    UPLOADED,
    VERIFIED,
    ACCESSED,
    EXPORTED,
    PURGED
}
