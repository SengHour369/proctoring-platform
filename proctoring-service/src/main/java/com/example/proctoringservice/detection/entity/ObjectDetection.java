package com.example.proctoringservice.detection.entity;

import com.example.proctoringservice.detection.enums.DetectedObjectClass;
import com.example.proctoringservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Object-model findings for one frame: a prohibited item recognised in the candidate's
 * environment. One row per detected instance, so a frame containing a phone and a book produces
 * two rows and each can be weighted on its own.
 */
@Entity
@Table(
        name = "object_detections",
        uniqueConstraints = @UniqueConstraint(name = "uk_object_detections_detection", columnNames = "ai_detection_id"),
        indexes = @Index(name = "ix_object_detections_class", columnList = "object_class"))
@Getter
@Setter
public class ObjectDetection extends BaseEntity {

    /** The AI_DETECTIONS row this detail belongs to; exactly one detail row per detection. */
    @Column(name = "ai_detection_id", nullable = false)
    private Long aiDetectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_class", nullable = false, length = 32)
    private DetectedObjectClass objectClass;

    /** Raw model label, kept because the taxonomy above is coarser than the model's vocabulary. */
    @Column(name = "object_label", length = 100)
    private String objectLabel;

    @Column(name = "object_count", nullable = false)
    private int objectCount = 1;

    @Column(name = "is_prohibited", nullable = false)
    private boolean prohibited = true;

    /** How close to the candidate's hands or face the item was — a phone in hand outranks one on a shelf. */
    @Column(name = "proximity_score", precision = 5, scale = 4)
    private BigDecimal proximityScore;

    /** Consecutive frames the item stayed visible; separates a glimpse from sustained use. */
    @Column(name = "persisted_frames")
    private Integer persistedFrames;

    @Embedded
    private BoundingBox boundingBox;

}
