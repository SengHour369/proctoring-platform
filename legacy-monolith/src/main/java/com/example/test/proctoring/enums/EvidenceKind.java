package com.example.test.proctoring.enums;

/** Type of captured artefact backing an event. */
public enum EvidenceKind {
    WEBCAM_SNAPSHOT,
    WEBCAM_CLIP,
    SCREEN_SNAPSHOT,
    SCREEN_CLIP,
    AUDIO_CLIP,
    ID_DOCUMENT,
    ENVIRONMENT_SCAN,
    KEYSTROKE_LOG,
    EVENT_LOG_BUNDLE,
    EXCEL_WORKBOOK_SNAPSHOT,
    EXCEL_FINAL_WORKBOOK,
    EXCEL_EDIT_LOG_BUNDLE,
    EXCEL_DIFF_REPORT
}
