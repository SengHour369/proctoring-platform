package com.example.proctoringservice.detection.enums;

/** What the audio model heard in the analysed window. */
public enum AudioEventType {
    SPEECH_DETECTED,
    MULTIPLE_VOICES,
    BACKGROUND_NOISE,
    AUDIO_INTERRUPTION,
    PROLONGED_SILENCE,
    SUSPICIOUS_AUDIO
}
