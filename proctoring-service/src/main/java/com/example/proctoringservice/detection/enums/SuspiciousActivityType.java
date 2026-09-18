package com.example.proctoringservice.detection.enums;

/** Alert class raised by the suspicious-activity engine. */
public enum SuspiciousActivityType {
    REPEATED_LOOKING_AWAY,
    MULTIPLE_FACES,
    PHONE_DETECTED,
    NO_FACE,
    SUSPICIOUS_OBJECT,
    TAB_SWITCHING,
    FULLSCREEN_EXIT,
    AUDIO_EVENT,
    IMPERSONATION_SUSPECTED,
    COMBINED_BEHAVIOR
}
