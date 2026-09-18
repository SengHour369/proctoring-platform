package com.example.proctoringservice.detection.entity;

import com.example.proctoringservice.detection.enums.BehaviorType;
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
 * Behavior-model findings over a time window rather than a single frame — looking away repeatedly,
 * leaving the seat, talking, typing in bursts that do not match the candidate's own baseline.
 *
 * <p>Because the unit of analysis is an interval, this subtype carries its own window bounds; the
 * inherited {@code capturedAt} marks when the window was evaluated.
 */
@Entity
@Table(
        name = "behavior_detections",
        uniqueConstraints = @UniqueConstraint(name = "uk_behavior_detections_detection", columnNames = "ai_detection_id"),
        indexes = @Index(name = "ix_behavior_detections_type", columnList = "behavior_type"))
@Getter
@Setter
public class BehaviorDetection extends BaseEntity {

    /** The AI_DETECTIONS row this detail belongs to; exactly one detail row per detection. */
    @Column(name = "ai_detection_id", nullable = false)
    private Long aiDetectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "behavior_type", nullable = false, length = 32)
    private BehaviorType behaviorType;

    @Column(name = "window_start_at", nullable = false)
    private Instant windowStartAt;

    @Column(name = "window_end_at", nullable = false)
    private Instant windowEndAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount = 1;

    /** How pronounced the behavior was within the window, 0.0000–1.0000. */
    @Column(name = "intensity_score", precision = 5, scale = 4)
    private BigDecimal intensityScore;

    /**
     * Standard deviations from this candidate's own calibration baseline. Absolute thresholds
     * misjudge people who simply fidget, so the deviation is what the risk model consumes.
     */
    @Column(name = "baseline_deviation", precision = 8, scale = 4)
    private BigDecimal baselineDeviation;

    @Column(name = "audio_related", nullable = false)
    private boolean audioRelated = false;

}
