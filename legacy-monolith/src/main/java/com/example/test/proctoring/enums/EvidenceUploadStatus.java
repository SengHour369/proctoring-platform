package com.example.test.proctoring.enums;

/** Upload and retention state of an evidence file. */
public enum EvidenceUploadStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED,
    QUARANTINED,
    PURGED
}
