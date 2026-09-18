package com.example.configservice.sharedenums;

/** What the scoring model advises; humans may override in review. */
public enum RiskRecommendation {
    ALLOW,
    MONITOR,
    FLAG_FOR_REVIEW,
    INVALIDATE_ATTEMPT,
    REQUIRE_RETAKE
}
