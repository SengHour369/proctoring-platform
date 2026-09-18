package com.example.test.risk.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.proctoring.enums.EventSeverity;
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
 * One line of the risk score's working: which signal contributed, with what weight, for how many
 * points. Without this table a score is a bare number nobody can defend in an appeal; with it,
 * "72, of which 30 came from a phone visible for 40 seconds" is reconstructable.
 */
@Entity
@Table(
        name = "risk_events",
        indexes = {
                @Index(name = "ix_risk_events_assessment", columnList = "risk_assessment_id"),
                @Index(name = "ix_risk_events_factor", columnList = "factor_code")
        })
@Getter
@Setter
public class RiskEvent extends BaseEntity {

    @Column(name = "risk_assessment_id", nullable = false)
    private Long riskAssessmentId;

    /** The observation behind the contribution, when it came from a session event. */
    @Column(name = "proctoring_event_id")
    private Long proctoringEventId;

    /** The inference behind the contribution, when it came from a model rather than an event. */
    @Column(name = "ai_detection_id")
    private Long aiDetectionId;

    /** Stable identifier of the scoring rule, e.g. PROHIBITED_OBJECT_SUSTAINED. */
    @Column(name = "factor_code", nullable = false, length = 64)
    private String factorCode;

    @Column(name = "factor_label", length = 150)
    private String factorLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventSeverity severity;

    /** Rule weight at scoring time, kept so a later reweighting cannot rewrite this explanation. */
    @Column(name = "weight", nullable = false, precision = 6, scale = 3)
    private BigDecimal weight;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount = 1;

    /** Points this factor actually added to the total. */
    @Column(name = "contributed_points", nullable = false, precision = 6, scale = 2)
    private BigDecimal contributedPoints;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(length = 500)
    private String note;
}
