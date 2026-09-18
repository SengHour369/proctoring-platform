package com.example.proctoringservice.detection.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.detection.enums.DetectionType;
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
 * One inference produced by an AI model over one captured frame or time window. Holds everything
 * common to every model — which model ran, how confident it was, what it analysed — while the
 * findings themselves live in the subtype tables.
 *
 * <p>The disjoint specialisation of the sketch is kept as physical tables rather than as JPA
 * inheritance: this row plus exactly one FACE_/OBJECT_/BEHAVIOR_/AUDIO_DETECTION row that names it
 * by id. The alternative — one wide nullable table — would leave three quarters of the columns
 * null on every row and no constraint to say which three quarters.
 *
 * <p>Because the subtype is a stored value rather than a discriminator, loading the detail for a
 * row is an explicit second query against the table {@code detectionType} names. Nothing in the
 * ingest path needs it: writers know what they produced, and the risk engine reads the detail
 * tables directly.
 *
 * <p>{@code modelName} and {@code modelVersion} are stored per row, not looked up: a score is only
 * defensible in an appeal if you can say which model version produced it.
 */
@Entity
@Table(
        name = "ai_detections",
        indexes = {
                @Index(name = "ix_ai_detections_session_time", columnList = "proctoring_session_id, captured_at"),
                @Index(name = "ix_ai_detections_event", columnList = "proctoring_event_id"),
                @Index(name = "ix_ai_detections_anomaly", columnList = "is_anomaly")
        })
@Getter
@Setter
public class AiDetection extends BaseEntity {

    /**
     * Which detail table holds the findings for this row: FACE, OBJECT, BEHAVIOR or AUDIO. A
     * stored column, so a reader knows where to look without probing four tables.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "detection_type", nullable = false, length = 16)
    private DetectionType detectionType;

    @Column(name = "proctoring_session_id", nullable = false)
    private Long proctoringSessionId;

    /**
     * The event this inference raised, if it crossed the reporting threshold. Null for routine
     * frames that were analysed and found unremarkable — those are kept for the baseline.
     */
    @Column(name = "proctoring_event_id")
    private Long proctoringEventId;

    /** The frame or clip the inference ran over, when it was retained. */
    @Column(name = "evidence_file_id")
    private Long evidenceFileId;

    /**
     * Registry entry for the model version that produced this row. Optional because a
     * client-side model may report a version the registry has not seen; the name/version strings
     * below are the authoritative snapshot either way.
     */
    @Column(name = "ai_model_version_id")
    private Long aiModelVersionId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 32)
    private String modelVersion;

    /** 0.0000–1.0000. Below the model's threshold the row is advisory only. */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "offset_ms")
    private Long offsetMs;

    /** True when this row is what a human should look at, as judged by the model. */
    @Column(name = "is_anomaly", nullable = false)
    private boolean anomaly = false;

    /** Verbatim model output, kept for re-scoring and for defending a decision on appeal. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_output")
    private String rawOutput;
}
