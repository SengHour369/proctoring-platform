package com.example.test.detection.enums;

/** Human adjudication of a raised alert; feeds model performance. */
public enum ActivityVerdict {
    DETECTED,
    CONFIRMED,
    FALSE_POSITIVE,
    UNDER_REVIEW,
    DISMISSED
}
