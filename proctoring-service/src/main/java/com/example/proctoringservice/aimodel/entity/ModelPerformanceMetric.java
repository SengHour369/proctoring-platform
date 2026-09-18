package com.example.proctoringservice.aimodel.entity;

import com.example.proctoringservice.aimodel.enums.MetricType;
import com.example.proctoringservice.common.entity.BaseEntity;
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
 * One measured metric for one model version over one window. The ground truth comes from
 * reviewers: every alert marked a false positive in REVIEW_FINDINGS is a labelled sample, which is
 * what makes a real false-positive rate computable rather than estimated.
 *
 * <p>Stored per window rather than as a single running figure, so a regression introduced by a new
 * version is visible as a step change instead of being diluted into a lifetime average.
 */
@Entity
@Table(
        name = "model_performance_metrics",
        uniqueConstraints = @UniqueConstraint(name = "uk_model_performance",
                columnNames = {"ai_model_version_id", "metric_type", "window_start_at"}),
        indexes = @Index(name = "ix_model_performance_version", columnList = "ai_model_version_id, window_start_at"))
@Getter
@Setter
public class ModelPerformanceMetric extends BaseEntity {

    @Column(name = "ai_model_version_id", nullable = false)
    private Long aiModelVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 32)
    private MetricType metricType;

    @Column(name = "metric_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal metricValue;

    @Column(name = "window_start_at", nullable = false)
    private Instant windowStartAt;

    @Column(name = "window_end_at", nullable = false)
    private Instant windowEndAt;

    /** Detections in the window; a metric over forty samples deserves less trust than over forty thousand. */
    @Column(name = "sample_count", nullable = false)
    private long sampleCount;

    /** Reviewer-labelled counts behind the figure, where the metric derives from adjudications. */
    @Column(name = "true_positive_count")
    private Long truePositiveCount;

    @Column(name = "false_positive_count")
    private Long falsePositiveCount;

    @Column(name = "false_negative_count")
    private Long falseNegativeCount;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    @Column(length = 500)
    private String note;
}
