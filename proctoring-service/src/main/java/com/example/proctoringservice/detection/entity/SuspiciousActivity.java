package com.example.proctoringservice.detection.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.detection.enums.ActivityVerdict;
import com.example.proctoringservice.detection.enums.SuspiciousActivityType;
import com.example.proctoringservice.proctoring.enums.EventSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A correlated alert: the layer above raw detections, where "looked away", "phone visible" and
 * "voice heard" within the same thirty seconds become one incident a human can act on.
 *
 * <p>Distinct from AI_DETECTIONS (one model, one frame) and from PROCTORING_EVENTS (one
 * observation): an activity spans a window, cites several signals, and is the unit that gets
 * confirmed or dismissed. The {@code verdict} closing the loop is also the only honest source of
 * false-positive rates for AI_MODEL_PERFORMANCE.
 */
@Entity
@Table(
        name = "suspicious_activities",
        indexes = {
                @Index(name = "ix_suspicious_activities_session", columnList = "proctoring_session_id, first_seen_at"),
                @Index(name = "ix_suspicious_activities_attempt", columnList = "exam_attempt_id"),
                @Index(name = "ix_suspicious_activities_type", columnList = "activity_type"),
                @Index(name = "ix_suspicious_activities_verdict", columnList = "verdict")
        })
@Getter
@Setter
public class SuspiciousActivity extends BaseEntity {

    @Column(name = "proctoring_session_id", nullable = false)
    private Long proctoringSessionId;

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 32)
    private SuspiciousActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventSeverity severity = EventSeverity.MEDIUM;

    /** Combined confidence across the contributing signals, 0.0000–1.0000. */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    /** Repeats folded into this incident, rather than one alert per frame. */
    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount = 1;

    @Column(name = "detection_count", nullable = false)
    private int detectionCount = 0;

    @Column(name = "event_count", nullable = false)
    private int eventCount = 0;

    /** The single most indicative detection, for the reviewer's first click. */
    @Column(name = "primary_detection_id")
    private Long primaryDetectionId;

    /** Ids of every contributing detection and event — the correlation rule's working. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contributing_signals")
    private String contributingSignals;

    @Column(name = "rule_code", length = 64)
    private String ruleCode;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ActivityVerdict verdict = ActivityVerdict.DETECTED;

    @Column(name = "adjudicated_by_user_id")
    private Long adjudicatedByUserId;

    @Column(name = "adjudicated_at")
    private Instant adjudicatedAt;

    @Column(name = "adjudication_note", length = 1000)
    private String adjudicationNote;
}
