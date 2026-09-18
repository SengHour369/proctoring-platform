package com.example.test.aimodel.entity;

import com.example.test.aimodel.enums.ShadowRecommendation;
import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A measured comparison of one SHADOW model version against the currently ACTIVE one over the
 * same traffic — the difference between "the shadow looks fine" and knowing it. A shadow version
 * never drives an auto-action and never grades itself: its false-positive rate here comes only
 * from {@code ReviewFinding} verdicts on its own shadow-only detections, the same ground truth
 * {@code ModelPerformanceMetric} already relies on for the active model.
 */
@Entity
@Table(
        name = "shadow_evaluations",
        indexes = @Index(name = "ix_shadow_evaluations_versions", columnList = "shadow_version_id, active_version_id"))
@Getter
@Setter
public class ShadowEvaluation extends BaseEntity {

    @Column(name = "shadow_version_id", nullable = false)
    private Long shadowVersionId;

    @Column(name = "active_version_id", nullable = false)
    private Long activeVersionId;

    @Column(name = "window_start_at", nullable = false)
    private Instant windowStartAt;

    @Column(name = "window_end_at", nullable = false)
    private Instant windowEndAt;

    @Column(name = "agreement_count", nullable = false)
    private long agreementCount = 0;

    @Column(name = "disagreement_count", nullable = false)
    private long disagreementCount = 0;

    @Column(name = "shadow_only_detections", nullable = false)
    private long shadowOnlyDetections = 0;

    @Column(name = "active_only_detections", nullable = false)
    private long activeOnlyDetections = 0;

    @Column(name = "shadow_false_positive_rate", precision = 6, scale = 4)
    private BigDecimal shadowFalsePositiveRate;

    @Column(name = "active_false_positive_rate", precision = 6, scale = 4)
    private BigDecimal activeFalsePositiveRate;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ShadowRecommendation recommendation;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();
}
