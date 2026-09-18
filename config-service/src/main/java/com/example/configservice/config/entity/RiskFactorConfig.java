package com.example.configservice.config.entity;

import com.example.configservice.common.entity.BaseEntity;
import com.example.configservice.config.enums.RiskFactorTrigger;
import com.example.configservice.sharedenums.EventSeverity;
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
 * The risk engine's rule book: one row per scoring factor, giving the weight a signal carries, how
 * repeats accumulate, and how fast old contributions decay.
 *
 * <p>Its own table rather than settings rows because the fields are a fixed, meaningful shape and
 * because RISK_EVENTS copies the weight it used at scoring time — the pair is what lets a score be
 * both re-tunable and reproducible.
 *
 * <p>Versioned and effective-dated, never edited in place: an assessment records its
 * {@code thresholdVersion}, so a change today cannot silently re-explain a decision from March.
 */
@Entity
@Table(
        name = "risk_factor_configs",
        uniqueConstraints = @UniqueConstraint(name = "uk_risk_factor_configs",
                columnNames = {"factor_code", "config_version"}),
        indexes = {
                @Index(name = "ix_risk_factor_configs_active", columnList = "is_active"),
                @Index(name = "ix_risk_factor_configs_trigger", columnList = "trigger_kind, trigger_code")
        })
@Getter
@Setter
public class RiskFactorConfig extends BaseEntity {

    @Column(name = "factor_code", nullable = false, length = 64)
    private String factorCode;

    @Column(name = "factor_label", nullable = false, length = 150)
    private String factorLabel;

    @Column(name = "config_version", nullable = false, length = 32)
    private String configVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_kind", nullable = false, length = 32)
    private RiskFactorTrigger triggerKind;

    /** The enum constant this rule reacts to, e.g. {@code PROHIBITED_OBJECT_DETECTED}. */
    @Column(name = "trigger_code", nullable = false, length = 48)
    private String triggerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private EventSeverity severity = EventSeverity.MEDIUM;

    /** Base points a single occurrence contributes. */
    @Column(name = "base_weight", nullable = false, precision = 6, scale = 3)
    private BigDecimal baseWeight;

    /** Multiplier applied to the model's confidence before weighting. */
    @Column(name = "confidence_multiplier", nullable = false, precision = 6, scale = 3)
    private BigDecimal confidenceMultiplier = BigDecimal.ONE;

    /** Extra points per repeat, so sustained behavior outweighs a one-off. */
    @Column(name = "frequency_increment", precision = 6, scale = 3)
    private BigDecimal frequencyIncrement;

    /** Points per second of duration, for factors where dwell time is the signal. */
    @Column(name = "duration_weight_per_second", precision = 6, scale = 3)
    private BigDecimal durationWeightPerSecond;

    /** Ceiling on this factor's total, so one noisy signal cannot dominate the score. */
    @Column(name = "max_contribution", precision = 6, scale = 2)
    private BigDecimal maxContribution;

    /** Half-life in seconds for decay; a glance twenty minutes ago should not weigh as much as one now. */
    @Column(name = "decay_half_life_seconds")
    private Integer decayHalfLifeSeconds;

    /** Occurrences tolerated before the factor scores at all. */
    @Column(name = "grace_occurrences", nullable = false)
    private int graceOccurrences = 0;

    /** Minimum model confidence for the signal to be considered. */
    @Column(name = "min_confidence", precision = 5, scale = 4)
    private BigDecimal minConfidence;

    /** True when a single occurrence should escalate straight to CRITICAL. */
    @Column(name = "is_immediate_critical", nullable = false)
    private boolean immediateCritical = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();

    @Column(name = "effective_to")
    private Instant effectiveTo;
}
