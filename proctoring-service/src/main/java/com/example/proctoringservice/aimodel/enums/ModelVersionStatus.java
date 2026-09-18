package com.example.proctoringservice.aimodel.enums;

/** Rollout state of one model version. */
public enum ModelVersionStatus {
    DRAFT,
    CANDIDATE,
    SHADOW,
    ACTIVE,
    DEPRECATED,
    RETIRED
}
