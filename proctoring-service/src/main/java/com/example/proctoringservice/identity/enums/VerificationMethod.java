package com.example.proctoringservice.identity.enums;

/** How a candidate's identity was established. */
public enum VerificationMethod {
    FACE_MATCH,
    ID_DOCUMENT,
    MANUAL_PROCTOR,
    KNOWLEDGE_CHALLENGE,
    SECOND_FACTOR
}
