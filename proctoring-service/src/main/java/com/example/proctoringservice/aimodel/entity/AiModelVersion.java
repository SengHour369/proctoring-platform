package com.example.proctoringservice.aimodel.entity;

import com.example.proctoringservice.aimodel.enums.ModelVersionStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One deployable version of a model, with the thresholds it runs at. Thresholds live on the
 * version, not on the model: raising the phone-detector's confidence floor from 0.60 to 0.80
 * changes what counts as evidence, and every detection scored under the old floor must remain
 * interpretable.
 *
 * <p>{@link ModelVersionStatus#SHADOW} allows a candidate version to score alongside the active
 * one without affecting outcomes — the only honest way to compare them on real traffic.
 */
@Entity
@Table(
        name = "ai_model_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_model_versions", columnNames = {"ai_model_id", "version"}),
        indexes = @Index(name = "ix_ai_model_versions_status", columnList = "status"))
@Getter
@Setter
public class AiModelVersion extends BaseEntity {

    @Column(name = "ai_model_id", nullable = false)
    private Long aiModelId;

    @Column(nullable = false, length = 32)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModelVersionStatus status = ModelVersionStatus.DRAFT;

    /** Artefact reference — registry URI, image digest or bundle path. */
    @Column(name = "artifact_ref", length = 512)
    private String artifactRef;

    @Column(name = "artifact_checksum", length = 64)
    private String artifactChecksum;

    /** Confidence below which output is ignored entirely. */
    @Column(name = "confidence_threshold", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceThreshold;

    /** Confidence at or above which a detection is raised as an event. */
    @Column(name = "detection_threshold", nullable = false, precision = 5, scale = 4)
    private BigDecimal detectionThreshold;

    /** Remaining knobs — frame rate, window length, NMS, per-class overrides. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration")
    private String configuration;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "deprecated_at")
    private Instant deprecatedAt;

    @Column(name = "activated_by_user_id")
    private Long activatedByUserId;

    @Column(name = "release_notes", length = 2000)
    private String releaseNotes;
}
