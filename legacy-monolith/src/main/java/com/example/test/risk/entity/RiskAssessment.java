package com.example.test.risk.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.risk.enums.RiskLevel;
import com.example.test.risk.enums.RiskRecommendation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aggregated integrity score for one attempt: the point where thousands of events and detections
 * collapse into a single number a human can act on.
 *
 * <p>Attached to the attempt rather than to AI_DETECTIONS as the sketch shows. Risk is not a
 * property of one inference — it is computed over every event, every detection and the device
 * record together, and a single detection contributing to it is recorded as a {@link RiskEvent}.
 *
 * <p>Versioned rather than overwritten. Re-scoring happens (a model is retuned, late evidence
 * lands, a reviewer requests a recompute) and the score that drove the original decision must
 * remain readable. Exactly one row per attempt carries {@code latest = true}.
 */
@Entity
@Table(
        name = "risk_assessments",
        uniqueConstraints = @UniqueConstraint(name = "uk_risk_assessments_version", columnNames = {"exam_attempt_id", "version"}),
        indexes = {
                @Index(name = "ix_risk_assessments_latest", columnList = "exam_attempt_id, is_latest"),
                @Index(name = "ix_risk_assessments_level", columnList = "risk_level"),
                @Index(name = "ix_risk_assessments_score", columnList = "risk_score")
        })
@Getter
@Setter
public class RiskAssessment extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    /** Null for an attempt sat without supervision, where risk rests on delivery signals only. */
    @Column(name = "proctoring_session_id")
    private Long proctoringSessionId;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "is_latest", nullable = false)
    private boolean latest = true;

    /** 0–100. Bands are configured, not hardcoded, hence {@code thresholdVersion}. */
    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskRecommendation recommendation = RiskRecommendation.ALLOW;

    @Column(name = "scoring_model", nullable = false, length = 100)
    private String scoringModel;

    @Column(name = "scoring_model_version", nullable = false, length = 32)
    private String scoringModelVersion;

    @Column(name = "threshold_version", length = 32)
    private String thresholdVersion;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    @Column(name = "recompute_reason", length = 255)
    private String recomputeReason;

    /** Component scores, so a reviewer can see which dimension drove the total. */
    @Column(name = "identity_score", precision = 5, scale = 2)
    private BigDecimal identityScore;

    @Column(name = "face_anomaly_score", precision = 5, scale = 2)
    private BigDecimal faceAnomalyScore;

    @Column(name = "object_anomaly_score", precision = 5, scale = 2)
    private BigDecimal objectAnomalyScore;

    @Column(name = "behavior_anomaly_score", precision = 5, scale = 2)
    private BigDecimal behaviorAnomalyScore;

    @Column(name = "environment_score", precision = 5, scale = 2)
    private BigDecimal environmentScore;

    @Column(name = "total_event_count", nullable = false)
    private int totalEventCount = 0;

    @Column(name = "critical_event_count", nullable = false)
    private int criticalEventCount = 0;

    @Column(name = "auto_action_applied", nullable = false)
    private boolean autoActionApplied = false;
}
