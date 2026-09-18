package com.example.configservice.config.entity;

import com.example.configservice.common.entity.BaseEntity;
import com.example.configservice.sharedenums.AutoAction;
import com.example.configservice.sharedenums.RiskLevel;
import com.example.configservice.sharedenums.RiskRecommendation;
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
 * The band boundaries that turn a numeric risk score into LOW / MEDIUM / HIGH / CRITICAL, and what
 * each band does.
 *
 * <p>These lived nowhere before: {@code RiskAssessment} stored both a score and a level, and
 * {@code RiskFactorConfig} stored the weights that produce the score, but the mapping between them
 * was implicit in code. That is the one part of the risk engine most likely to be tuned by a
 * non-developer — "stop auto-terminating at 85, flag for review at 60 instead" — and the one whose
 * historical values a candidate is most likely to challenge.
 *
 * <p>Versioned by {@code configVersion}, matching {@code RiskAssessment.thresholdVersion}, so the
 * bands in force when a decision was taken can always be reconstructed. Rows are inserted, never
 * edited: a band whose meaning changed is a new version.
 */
@Entity
@Table(
        name = "risk_level_thresholds",
        uniqueConstraints = @UniqueConstraint(name = "uk_risk_level_thresholds",
                columnNames = {"config_version", "risk_level"}),
        indexes = @Index(name = "ix_risk_level_thresholds_active", columnList = "is_active, min_score"))
@Getter
@Setter
public class RiskLevelThreshold extends BaseEntity {

    @Column(name = "config_version", nullable = false, length = 32)
    private String configVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    /** Inclusive lower bound of the band, 0–100. */
    @Column(name = "min_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minScore;

    /** Exclusive upper bound; null for the topmost band. */
    @Column(name = "max_score", precision = 5, scale = 2)
    private BigDecimal maxScore;

    /** What the engine advises for an attempt landing in this band. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskRecommendation recommendation = RiskRecommendation.ALLOW;

    /** What the platform does on its own, with no human in the loop. */
    @Enumerated(EnumType.STRING)
    @Column(name = "auto_action", nullable = false, length = 32)
    private AutoAction autoAction = AutoAction.NONE;

    /** Open a review case for anything reaching this band. */
    @Column(name = "opens_review_case", nullable = false)
    private boolean opensReviewCase = false;

    /** Withhold the result until the review closes, rather than publishing provisionally. */
    @Column(name = "withholds_result", nullable = false)
    private boolean withholdsResult = false;

    /** Raise the band on the live monitoring wall and notify the assigned proctor. */
    @Column(name = "alerts_proctor", nullable = false)
    private boolean alertsProctor = false;

    /** Colour token for the dashboard, so the wall and the reports agree. */
    @Column(name = "display_colour", length = 16)
    private String displayColour;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();

    @Column(name = "effective_to")
    private Instant effectiveTo;
}
